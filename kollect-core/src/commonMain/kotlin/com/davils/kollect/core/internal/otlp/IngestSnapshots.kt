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

import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlin.time.Instant

internal object IngestSnapshots {
    fun of(now: Instant, families: List<IngestedFamily>, sources: List<Pair<String, SourceState>>): MetricsSnapshot {
        val groups = OrderedMap<SeriesId, MutableList<SeriesState>>()
        sources.forEach { (source, state) ->
            val all = state.series.entries + state.retired.entries
            all.forEach { (id, series) ->
                val group = SeriesId(family = id.family, instance = source, values = id.values)
                groups.getOrPut(group) { mutableListOf() }.add(series)
            }
        }
        val byFamily = groups.entries.groupBy { (id, _) -> id.family }
        val ordered = families.sortedWith { first, second -> first.name.compareTo(second.name) }
        val snapshots = ordered.map { family ->
            familyOf(family, byFamily[family.name.value].orEmpty())
        }
        return MetricsSnapshot(now, snapshots)
    }

    private fun familyOf(family: IngestedFamily, groups: List<Pair<SeriesId, List<SeriesState>>>): FamilySnapshot {
        val descriptor = Descriptor(
            name = family.name,
            kind = family.kind,
            unit = family.unit,
            description = family.description,
            labelKeys = listOf(PointChecks.SOURCE_KEY) + family.keys,
            bounds = family.bounds?.let(ExplicitBounds::of),
        )
        val points = groups.map { (id, series) ->
            pointOf(
                family.kind,
                Labels(descriptor.labelKeys, listOf(id.instance) + id.values),
                series,
            )
        }
        return FamilySnapshot(descriptor, Temporality.Cumulative, points.sortedBy(Point::labels))
    }

    private fun pointOf(kind: MeterKind, labels: Labels, series: List<SeriesState>): Point {
        val start = series.minOf(SeriesState::created)
        val total = SeriesState(created = start, lastSeen = series.maxOf(SeriesState::lastSeen))
        series.forEach { state -> state.addInto(total) }
        return when (kind) {
            MeterKind.Gauge -> {
                latestGauge(labels, start, series)
            }

            MeterKind.Histogram -> {
                histogramOf(labels, start, total)
            }

            MeterKind.Counter, MeterKind.UpDownCounter, MeterKind.StateSet, MeterKind.Info -> {
                SumPoint(labels = labels, start = start, value = total.value)
            }
        }
    }

    private fun histogramOf(labels: Labels, start: Instant, total: SeriesState): Point = HistogramPoint(
        labels = labels,
        start = start,
        count = total.count,
        sum = total.sum,
        min = null,
        max = null,
        bucketCounts = total.buckets,
    )

    private fun latestGauge(labels: Labels, start: Instant, series: List<SeriesState>): Point {
        val latest = series.maxBy(SeriesState::lastTime)
        return GaugePoint(labels = labels, start = start, value = latest.value, observedAt = latest.lastSeen)
    }
}
