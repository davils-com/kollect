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

package com.davils.kollect.core.internal.health

import com.davils.kollect.core.health.Impact
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.health.rule.Direction
import com.davils.kollect.core.health.rule.Statistic
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.sync.Semaphore
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val RULE: String = "rule"
private const val MISSING_STATE: Int = 5

private val name: MetricName = MetricName.of("app.value")
private val now: Instant = Instant.fromEpochSeconds(1L)

private fun gauge(value: Double): Point = GaugePoint(
    labels = Labels.EMPTY,
    start = now,
    value = value,
    observedAt = now,
)

private fun evaluationOf(point: Point): Evaluation {
    val descriptor = Descriptor(
        name = name,
        kind = MeterKind.Gauge,
        unit = MeasureUnit.NONE,
        description = "",
        labelKeys = emptyList(),
        states = listOf("on"),
    )
    val family = FamilySnapshot(descriptor, Temporality.Cumulative, listOf(point))
    return Evaluation(now, MetricsSnapshot(now, listOf(family)), Semaphore(1))
}

private suspend fun HealthNode.reasonsFor(point: Point): List<ReasonCode> {
    val result = evaluate(evaluationOf(point), prefix = "")
    return result.health.reasons
}

private suspend fun reasonsOf(statistic: Statistic, point: Point): List<ReasonCode> {
    val spec = ThresholdSpec(
        statistic = statistic,
        direction = Direction.Above,
        degraded = null,
        down = 1.0,
        maximumAge = null,
        maximumCheckpoints = 1,
    )
    val node = ThresholdNode(
        name = RULE,
        impact = Impact.Critical,
        meter = name,
        undetermined = Undetermined.Down,
        rule = spec,
    )
    return node.reasonsFor(point)
}

@OptIn(ExperimentalKotest::class)
class ThresholdNodeTest : FunSpec({
    coroutineTestScope = true

    test("a rule decides only what its statistic can read") {
        reasonsOf(Statistic.Value, gauge(Double.NaN)) shouldBe listOf(ReasonCode.NotANumber)
        reasonsOf(Statistic.Value, StateSetPoint(Labels.EMPTY, now, 0)) shouldBe listOf(ReasonCode.NoData)
        reasonsOf(Statistic.Count, InfoPoint(Labels.EMPTY, now)) shouldBe listOf(ReasonCode.NoData)
        reasonsOf(Statistic.Mean, SumPoint(Labels.EMPTY, now, 1.0)) shouldBe listOf(ReasonCode.NoData)
        reasonsOf(Statistic.Rate(1.minutes), gauge(1.0)) shouldBe listOf(ReasonCode.NoData)
        reasonsOf(Statistic.Count, SumPoint(Labels.EMPTY, now, 2.0)) shouldBe listOf(ReasonCode.ThresholdCrossed)
        reasonsOf(Statistic.Count, gauge(1.0)) shouldBe listOf(ReasonCode.NoData)
        reasonsOf(Statistic.Count, StateSetPoint(Labels.EMPTY, now, 0)) shouldBe listOf(ReasonCode.NoData)
    }

    test("a state rule decides nothing for a state the family does not have") {
        val node = StateNode(
            name = RULE,
            impact = Impact.Critical,
            meter = name,
            undetermined = Undetermined.Down,
            downStates = setOf("on"),
            degradedStates = emptySet(),
        )
        node.reasonsFor(StateSetPoint(Labels.EMPTY, now, MISSING_STATE)) shouldBe listOf(ReasonCode.NoData)
        val families = evaluationOf(gauge(1.0)).snapshot.families
        val descriptor = families.single().descriptor
        descriptor.boundValues.shouldBeEmpty()
    }
})
