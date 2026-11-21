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

package com.davils.kollect.core

import com.davils.kollect.core.export.DropReason
import com.davils.kollect.core.export.ExportAccounting
import com.davils.kollect.core.exposition.FamilyProblem
import com.davils.kollect.core.exposition.RefusedFamily
import com.davils.kollect.core.fixture.checkValue
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.health.MemberReport
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.internal.otlp.SeriesId
import com.davils.kollect.core.internal.registry.SeriesKey
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kollect.core.snapshot.Unreadability
import com.davils.kollect.core.snapshot.Unreadable
import io.kotest.core.spec.style.FunSpec
import kotlin.time.Instant

private val start: Instant = Instant.fromEpochSeconds(1L)
private val later: Instant = Instant.fromEpochSeconds(2L)
private val labels: Labels = Labels(listOf(LabelKey.of("a")), listOf("x"))
private val otherLabels: Labels = Labels(listOf(LabelKey.of("a")), listOf("y"))
private val name: MetricName = MetricName.of("queue.size")
private val otherName: MetricName = MetricName.of("queue.length")

private fun descriptor(
    kind: MeterKind = MeterKind.Gauge,
    unit: MeasureUnit = MeasureUnit.NONE,
    description: String = "",
    keys: List<LabelKey> = emptyList(),
    bounds: ExplicitBounds? = null,
    states: List<String> = emptyList(),
    metric: MetricName = name,
): Descriptor = Descriptor(
    name = metric,
    kind = kind,
    unit = unit,
    description = description,
    labelKeys = keys,
    bounds = bounds,
    states = states,
)

private fun histogram(
    point: Labels = labels,
    at: Instant = start,
    count: Long = 1L,
    sum: Double = 1.0,
    min: Double? = 1.0,
    max: Double? = 1.0,
    buckets: List<Long> = listOf(1L),
): HistogramPoint = HistogramPoint(
    labels = point,
    start = at,
    count = count,
    sum = sum,
    min = min,
    max = max,
    bucketCounts = buckets,
)

class ValueSemanticsTest : FunSpec({
    test("points compare by every part") {
        checkValue(
            SumPoint(labels, start, 1.0) to
                SumPoint(labels, start, 1.0),
            SumPoint(otherLabels, start, 1.0),
            SumPoint(labels, later, 1.0),
            SumPoint(labels, start, 2.0),
        )
        checkValue(
            GaugePoint(labels = labels, start = start, value = 1.0, observedAt = start) to
                GaugePoint(labels = labels, start = start, value = 1.0, observedAt = start),
            GaugePoint(labels = otherLabels, start = start, value = 1.0, observedAt = start),
            GaugePoint(labels = labels, start = later, value = 1.0, observedAt = start),
            GaugePoint(labels = labels, start = start, value = 2.0, observedAt = start),
            GaugePoint(labels = labels, start = start, value = 1.0, observedAt = later),
        )
        checkValue(
            histogram() to
                histogram(),
            histogram(point = otherLabels),
            histogram(at = later),
            histogram(count = 2L),
            histogram(sum = 2.0),
            histogram(min = null),
            histogram(max = null),
            histogram(buckets = listOf(2L)),
        )
        checkValue(
            StateSetPoint(labels, start, 0) to
                StateSetPoint(labels, start, 0),
            StateSetPoint(otherLabels, start, 0),
            StateSetPoint(labels, later, 0),
            StateSetPoint(labels, start, 1),
        )
        checkValue(
            InfoPoint(labels, start) to
                InfoPoint(labels, start),
            InfoPoint(otherLabels, start),
            InfoPoint(labels, later),
        )
    }

    test("snapshots compare by every part") {
        val points = listOf(SumPoint(labels, start, 1.0))
        val family = FamilySnapshot(descriptor(), Temporality.Cumulative, points)
        checkValue(
            family to
                FamilySnapshot(descriptor(), Temporality.Cumulative, points),
            FamilySnapshot(descriptor(metric = otherName), Temporality.Cumulative, points),
            FamilySnapshot(descriptor(), Temporality.Delta, points),
            FamilySnapshot(descriptor(), Temporality.Cumulative, emptyList()),
        )
        val unreadable = Unreadable(name, Unreadability.Stalled)
        checkValue(
            unreadable to
                Unreadable(name, Unreadability.Stalled),
            Unreadable(otherName, Unreadability.Stalled),
            Unreadable(name, Unreadability.CallbackFailed),
        )
        checkValue(
            MetricsSnapshot(start, listOf(family), listOf(unreadable)) to
                MetricsSnapshot(start, listOf(family), listOf(unreadable)),
            MetricsSnapshot(later, listOf(family), listOf(unreadable)),
            MetricsSnapshot(start, emptyList(), listOf(unreadable)),
            MetricsSnapshot(start, listOf(family), emptyList()),
        )
    }

    test("descriptors compare by every part") {
        checkValue(
            descriptor() to
                descriptor(),
            descriptor(metric = otherName),
            descriptor(kind = MeterKind.Counter),
            descriptor(unit = MeasureUnit.BYTES),
            descriptor(description = "x"),
            descriptor(keys = listOf(LabelKey.of("a"))),
            descriptor(bounds = ExplicitBounds.DEFAULT),
            descriptor(states = listOf("on")),
        )
        checkValue(
            MeasureUnit.annotation("item") to
                MeasureUnit.annotation("item"),
            MeasureUnit.annotation("thing"),
            MeasureUnit.RATIO,
        )
    }

    test("results and reports compare by every part") {
        checkValue(
            RefusedFamily(name, FamilyProblem.InvalidText) to
                RefusedFamily(name, FamilyProblem.InvalidText),
            RefusedFamily(otherName, FamilyProblem.InvalidText),
            RefusedFamily(name, FamilyProblem.NameCollision),
        )
        val dropped = mapOf(DropReason.Rejected to 1L)
        checkValue(
            ExportAccounting(produced = 1L, delivered = 1L, rejected = 1L, dropped = dropped, pending = 1L) to
                ExportAccounting(produced = 1L, delivered = 1L, rejected = 1L, dropped = dropped, pending = 1L),
            ExportAccounting(produced = 2L, delivered = 1L, rejected = 1L, dropped = dropped, pending = 1L),
            ExportAccounting(produced = 1L, delivered = 2L, rejected = 1L, dropped = dropped, pending = 1L),
            ExportAccounting(produced = 1L, delivered = 1L, rejected = 2L, dropped = dropped, pending = 1L),
            ExportAccounting(produced = 1L, delivered = 1L, rejected = 1L, dropped = emptyMap(), pending = 1L),
            ExportAccounting(produced = 1L, delivered = 1L, rejected = 1L, dropped = dropped, pending = 2L),
        )
        checkValue(
            Health.down(ReasonCode.TimedOut) to
                Health.down(ReasonCode.TimedOut),
            Health.degraded(ReasonCode.TimedOut),
            Health.down(ReasonCode.Stale),
        )
        checkValue(
            MemberReport("a", HealthStatus.Up, emptyList()) to
                MemberReport("a", HealthStatus.Up, emptyList()),
            MemberReport("b", HealthStatus.Up, emptyList()),
            MemberReport("a", HealthStatus.Down, emptyList()),
            MemberReport("a", HealthStatus.Up, listOf(ReasonCode.Stale)),
        )
    }

    test("keys compare by every part") {
        checkValue(SeriesKey(listOf("a")) to SeriesKey(listOf("a")), SeriesKey(listOf("b")))
        checkValue(
            SeriesId(family = "f", instance = "i", values = listOf("a")) to
                SeriesId(family = "f", instance = "i", values = listOf("a")),
            SeriesId(family = "g", instance = "i", values = listOf("a")),
            SeriesId(family = "f", instance = "j", values = listOf("a")),
            SeriesId(family = "f", instance = "i", values = listOf("b")),
        )
    }
})
