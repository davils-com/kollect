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

package com.davils.kollect.core.internal.health

import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.Impact
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.health.rule.Direction
import com.davils.kollect.core.health.rule.Statistic
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlin.time.Duration
import kotlin.time.Instant

internal class ThresholdNode(
    name: String,
    impact: Impact,
    meter: MetricName,
    undetermined: Undetermined,
    private val rule: ThresholdSpec,
) : RuleNode(name, impact, meter, undetermined) {
    private var checkpoints: OrderedMap<Labels, Checkpoints> = OrderedMap()

    override fun judge(family: FamilySnapshot, point: Point, now: Instant): Health {
        val reading = readingOf(point, now)
        val value = reading.value
        val reason = ReasonCode.NotANumber.takeIf { value != null } ?: reading.reason ?: ReasonCode.NoData
        if (value == null || value.isNaN()) return unknown(reason)
        return thresholdOf(value)
    }

    override fun observed(family: FamilySnapshot) {
        val kept = OrderedMap<Labels, Checkpoints>()
        family.points.forEach { point -> checkpoints[point.labels]?.let { kept[point.labels] = it } }
        checkpoints = kept
    }

    private fun thresholdOf(value: Double): Health {
        val isAbove = rule.direction == Direction.Above
        val isDown = rule.down?.let { down -> crosses(value, down, isAbove) } == true
        val isDegraded = rule.degraded?.let { degraded -> crosses(value, degraded, isAbove) } == true
        if (isDown) return Health.down(ReasonCode.ThresholdCrossed)
        return Health.degraded(ReasonCode.ThresholdCrossed).takeIf { isDegraded } ?: Health.Up
    }

    private fun crosses(value: Double, threshold: Double, isAbove: Boolean): Boolean = value.let { reading ->
        val isAboveCrossed = isAbove && reading >= threshold
        val isBelowCrossed = !isAbove && reading <= threshold
        isAboveCrossed || isBelowCrossed
    }

    private fun readingOf(point: Point, now: Instant): Reading = when (val statistic = rule.statistic) {
        Statistic.Value -> valueOf(point, now)
        Statistic.Count -> Reading(countOf(point))
        Statistic.Mean -> meanOf(point)
        is Statistic.Rate -> rateOf(point, now, statistic.window)
    }

    private fun valueOf(point: Point, now: Instant): Reading {
        val maximumAge = rule.maximumAge
        val isStale = point is GaugePoint && maximumAge != null && now - point.observedAt > maximumAge
        if (isStale) return Reading(null, ReasonCode.Stale)
        return Reading((point as? SumPoint)?.value ?: (point as? GaugePoint)?.value)
    }

    private fun countOf(point: Point): Double? = when (point) {
        is SumPoint -> point.value
        is HistogramPoint -> point.count.toDouble()
        is GaugePoint, is StateSetPoint, is InfoPoint -> null
    }

    private fun meanOf(point: Point): Reading {
        val histogram = point as? HistogramPoint ?: return Reading(null)
        val mean = (histogram.sum / histogram.count).takeIf { histogram.count > 0L }
        return Reading(mean, ReasonCode.InsufficientSamples)
    }

    private fun rateOf(point: Point, now: Instant, window: Duration): Reading {
        val value = countOf(point) ?: return Reading(null)
        val existing = checkpoints[point.labels]?.takeIf { found -> found.isFor(point.start) }
        val series = existing ?: Checkpoints(
            point.start,
            rule.maximumCheckpoints,
        ).also { checkpoints[point.labels] = it }
        return Reading(series.rate(now, value, window), ReasonCode.InsufficientSamples)
    }

    private data class Reading(val value: Double?, val reason: ReasonCode? = null)
}
