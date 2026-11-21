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

package com.davils.kollect.core.fixture

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.exposition.ExpositionFormat
import com.davils.kollect.core.exposition.ExpositionResult
import com.davils.kollect.core.exposition.exposition
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.SumPoint

internal fun MetricsSnapshot.familyNamed(name: String): FamilySnapshot? = family(MetricName.of(name))

internal fun MetricsSnapshot.pointsOf(name: String): List<Point> = familyNamed(name)?.points.orEmpty()

internal fun MetricsSnapshot.sumsOf(name: String): List<Double> = pointsOf(
    name,
).map { point -> (point as SumPoint).value }

internal fun MetricsSnapshot.gaugesOf(name: String): List<Double> =
    pointsOf(name).map { point -> (point as GaugePoint).value }

internal fun MetricsSnapshot.histogramOf(name: String): HistogramPoint = pointsOf(name).single() as HistogramPoint

internal val MetricsSnapshot.userFamilies: List<FamilySnapshot>
    get() = families.filterNot { family ->
        family.descriptor.name.value
            .startsWith("kollect.")
    }

internal fun MetricsSnapshot.text(format: ExpositionFormat = ExpositionFormat.PrometheusText): String {
    val result = exposition().write(format, this) as ExpositionResult.Written
    return result.payload.copyBytes().decodeToString()
}

internal fun MetricsSnapshot.withoutOwnFamilies(): MetricsSnapshot = MetricsSnapshot(
    timestamp,
    userFamilies,
    unreadable,
)

internal fun MeterRegistry.points(name: String): List<Point> = snapshot().pointsOf(name)

internal fun MeterRegistry.sums(name: String): List<Double> = snapshot().sumsOf(name)

internal fun MeterRegistry.gauges(name: String): List<Double> = snapshot().gaugesOf(name)

internal fun MeterRegistry.histogramPoint(name: String): HistogramPoint = snapshot().histogramOf(name)

internal fun MetricsSnapshot.histogramNamed(name: String): HistogramPoint {
    val points = familyNamed(name)?.points.orEmpty()
    return points.single() as HistogramPoint
}

internal fun List<Point>.labelValues(): List<List<String>> = map { point -> point.labels.values }

internal fun List<Point>.firstLabels(): List<String> = map { point -> point.labels.values.first() }
