/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.internal.platform.Guard
import com.davils.kollect.core.internal.platform.guard
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.otlp.IngestRejection
import com.davils.kollect.core.otlp.IngestVerdict
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

private val EXPIRY_INTERVAL = 1.seconds
private val TOKEN_PERIOD = 1.minutes

internal class IngestStore(
    private val limits: IngestLimits,
    private val rules: List<FamilyRule>,
    private val anyPrefix: String?,
) {
    private val guard: Guard = guard()
    private val checks: PointChecks = PointChecks(limits)
    private var families: OrderedMap<String, IngestedFamily> = OrderedMap()
    private var sources: OrderedMap<String, SourceState> = OrderedMap()
    private var seriesCount: Int = 0
    private var expiredAt: Instant = Instant.DISTANT_PAST

    val sourceCount: Int get() = guard.locked { sources.size }

    fun merge(source: String, resources: List<DecodedResource>, now: Instant, counts: IngestCounts): IngestVerdict? =
        guard.locked {
            expire(now)
            val points = resources.sumOf { resource -> resource.metrics.sumOf { metric -> metric.points.size } }
            val state = admitted(source, now, points)
            state?.let { admittedState ->
                mergeInto(
                    state = admittedState,
                    resources = resources,
                    now = now,
                    counts = counts,
                )
            }
            IngestVerdict.Throttled.takeIf { state == null }
        }

    fun forget(source: String): Boolean = guard.locked {
        val state = sources[source]
        if (state != null) rebuildSources(now = null, without = source)
        state != null
    }

    fun snapshot(now: Instant): MetricsSnapshot = guard.locked {
        expire(now)
        IngestSnapshots.of(now, families.entries.map { (_, family) -> family }, sources.entries)
    }

    private fun admitted(source: String, now: Instant, points: Int): SourceState? {
        val existing = sources[source]
        val isFull = existing == null && sources.size >= limits.maximumSources
        val state = existing ?: SourceState(
            tokens = limits.maximumPointsPerMinute.toDouble(),
            refilledAt = now,
        ).takeUnless { isFull }
        if (state == null || !isPaid(state, now, points)) return null
        if (existing == null) sources[source] = state
        return state
    }

    private fun isPaid(state: SourceState, now: Instant, points: Int): Boolean {
        val capacity = limits.maximumPointsPerMinute.toDouble()
        val elapsed = (now - state.refilledAt).toDouble(DurationUnit.SECONDS).coerceAtLeast(0.0)
        val refill = elapsed * capacity / TOKEN_PERIOD.toDouble(DurationUnit.SECONDS)
        state.tokens = minOf(a = capacity, b = state.tokens + refill)
        state.refilledAt = now
        val isPaid = points <= state.tokens
        if (isPaid) state.tokens -= points
        return isPaid
    }

    private fun mergeInto(state: SourceState, resources: List<DecodedResource>, now: Instant, counts: IngestCounts) {
        resources.forEach { resource ->
            resource.metrics.forEach { metric ->
                metric.points.forEach { point ->
                    counts.record(
                        applied(state = state, instance = resource.instance, metric = metric, point = point, now = now),
                    )
                }
            }
        }
    }

    private fun applied(
        state: SourceState,
        instance: String,
        metric: DecodedMetric,
        point: DecodedPoint,
        now: Instant,
    ): IngestRejection? {
        val family = familyOf(metric, point) ?: return rejectionOfFamily(metric, point)
        val arrival = Arrival(state = state, instance = instance, metric = metric, point = point, now = now)
        return checks.rejectionOf(family, point, now) ?: recordedIfValued(arrival, family)
    }

    private fun recordedIfValued(arrival: Arrival, family: IngestedFamily): IngestRejection? {
        if (isNoRecordedValue(arrival.point)) return null
        val values = checks.valuesOf(family, arrival.point)
        val id = SeriesId(family = family.name.value, instance = arrival.instance, values = values)
        return recorded(
            state = arrival.state,
            id = id,
            family = family,
            metric = arrival.metric,
            point = arrival.point,
            now = arrival.now,
        )
    }

    private fun recorded(
        state: SourceState,
        id: SeriesId,
        family: IngestedFamily,
        metric: DecodedMetric,
        point: DecodedPoint,
        now: Instant,
    ): IngestRejection? {
        val series = state.series[id] ?: newSeries(state, id, now) ?: return IngestRejection.SeriesLimit
        series.lastSeen = now
        return SeriesUpdate.apply(
            series = series,
            kind = family.kind,
            temporality = metric.temporality,
            point = point,
        ) { retired -> retire(state, id, retired) }
    }

    private fun newSeries(state: SourceState, id: SeriesId, now: Instant): SeriesState? {
        val isFull = state.size >= limits.maximumSeriesPerSource || seriesCount >= limits.maximumSeries
        if (isFull) return null
        val series = SeriesState(created = now, lastSeen = now)
        state.series[id] = series
        seriesCount += 1
        return series
    }

    private fun retire(state: SourceState, id: SeriesId, retired: SeriesState) {
        val base = state.retired[id.retiredId] ?: SeriesState(
            created = retired.created,
            lastSeen = retired.lastSeen,
        ).also { fresh ->
            state.retired[id.retiredId] = fresh
            seriesCount += 1
        }
        retired.addInto(base)
    }

    private fun familyOf(metric: DecodedMetric, point: DecodedPoint): IngestedFamily? {
        val kind = kindOf(metric) ?: return null
        val existing = families[metric.name]
        val isRoomy = existing == null && families.size < limits.maximumSeries
        val created = newFamily(metric, kind, point).takeIf { isRoomy }
        created?.let { family -> families[family.name.value] = family }
        return existing?.takeIf { found -> found.kind == kind && found.unit.ucum == metric.unit } ?: created
    }

    private fun newFamily(metric: DecodedMetric, kind: MeterKind, point: DecodedPoint): IngestedFamily? {
        val name = MetricName.ofOrNull(metric.name) ?: return null
        val unit = MeasureUnit.ofUcumOrNull(metric.unit)
        return unit?.let { found ->
            describedFamily(
                name = name,
                unit = found,
                kind = kind,
                metric = metric,
                point = point,
            )
        }
    }

    private fun describedFamily(
        name: MetricName,
        unit: MeasureUnit,
        kind: MeterKind,
        metric: DecodedMetric,
        point: DecodedPoint,
    ): IngestedFamily? {
        val rule = ruleOf(name)
        val isMatching = rule?.let { declared -> declared.kind == kind && declared.unit == unit } != false
        val keys = keysFor(name, metric, point)?.takeIf { isMatching } ?: return null
        val isDescribed = TextRules.isValidText(metric.description, limits.maximumStringBytes)
        return IngestedFamily(
            name = name,
            kind = kind,
            unit = unit,
            description = metric.description.takeIf { isDescribed }.orEmpty(),
            keys = keys,
            bounds = point.bounds.takeIf { kind == MeterKind.Histogram },
        )
    }

    private fun keysFor(name: MetricName, metric: DecodedMetric, point: DecodedPoint): List<LabelKey>? {
        val rule = ruleOf(name)
        if (rule != null) return rule.keys
        val isOpen = anyPrefix != null && metric.name.startsWith(anyPrefix)
        return checks.keysOf(point)?.takeIf { isOpen }
    }

    private fun rejectionOfFamily(metric: DecodedMetric, point: DecodedPoint): IngestRejection {
        if (metric.type == DecodedType.Unsupported) return IngestRejection.UnsupportedType
        val isKnown = metric.name in families || MetricName.ofOrNull(metric.name)?.let(::ruleOf) != null
        val isOpen = anyPrefix != null && metric.name.startsWith(anyPrefix) && checks.keysOf(point) == null
        return IngestRejection.Mismatch.takeIf { isKnown } ?: IngestRejection.InvalidLabels.takeIf { isOpen }
            ?: IngestRejection.UndeclaredFamily
    }

    private fun ruleOf(name: MetricName): FamilyRule? = rules.firstOrNull { rule -> rule.name == name }

    private fun kindOf(metric: DecodedMetric): MeterKind? = when (metric.type) {
        DecodedType.Gauge -> MeterKind.Gauge
        DecodedType.Sum -> MeterKind.Counter.takeIf { metric.isMonotonic } ?: MeterKind.UpDownCounter
        DecodedType.Histogram -> MeterKind.Histogram
        DecodedType.Unsupported -> null
    }

    private fun isNoRecordedValue(point: DecodedPoint): Boolean =
        point.flags and OtlpFields.FLAG_NO_RECORDED_VALUE != 0L

    private fun expire(now: Instant) {
        if (now - expiredAt < EXPIRY_INTERVAL) return
        expiredAt = now
        rebuildSources(now = now, without = null)
    }

    private fun rebuildSources(now: Instant?, without: String?) {
        val rebuilt = OrderedMap<String, SourceState>()
        var count = 0
        sources.entries.forEach { (name, state) ->
            if (name != without && now != null) Expiry.expire(state, now, limits) { family -> families[family]?.kind }
            val isKept = name != without && state.size > 0
            if (isKept) rebuilt[name] = state
            if (isKept) count += state.size
        }
        sources = rebuilt
        seriesCount = count
        rebuildFamilies()
    }

    private fun rebuildFamilies() {
        val used = OrderedSet<String>()
        sources.entries.forEach { (_, state) ->
            (state.series.keys + state.retired.keys).forEach { id ->
                used.add(
                    id.family,
                )
            }
        }
        val rebuilt = OrderedMap<String, IngestedFamily>()
        families.entries.forEach { (name, family) -> if (name in used) rebuilt[name] = family }
        families = rebuilt
    }

    private data class Arrival(
        val state: SourceState,
        val instance: String,
        val metric: DecodedMetric,
        val point: DecodedPoint,
        val now: Instant,
    )
}
