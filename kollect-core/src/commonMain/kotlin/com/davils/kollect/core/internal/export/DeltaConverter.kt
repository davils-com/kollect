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

package com.davils.kollect.core.internal.export

import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlin.time.Instant

internal class DeltaConverter {
    private var previous: OrderedMap<DeltaKey, Point> = OrderedMap()
    private var previousAt: Instant? = null

    fun convert(snapshot: MetricsSnapshot): MetricsSnapshot {
        val next = OrderedMap<DeltaKey, Point>()
        val families = snapshot.families.map { family -> converted(family, next) }
        previous = next
        previousAt = snapshot.timestamp
        return MetricsSnapshot(snapshot.timestamp, families, snapshot.unreadable)
    }

    private fun converted(family: FamilySnapshot, next: OrderedMap<DeltaKey, Point>): FamilySnapshot {
        val kind = family.descriptor.kind
        val isAccumulating = kind == MeterKind.Counter || kind == MeterKind.Histogram
        if (!isAccumulating) return family
        val points = family.points.map { point ->
            val key = DeltaKey(family.descriptor.name, point.labels)
            next[key] = point
            deltaOf(point, previous[key])
        }
        return FamilySnapshot(family.descriptor, Temporality.Delta, points)
    }

    private fun deltaOf(point: Point, before: Point?): Point {
        val since = previousAt?.takeIf { before != null && before.start == point.start } ?: return point
        return when (point) {
            is SumPoint -> sumDelta(point, before as? SumPoint, since)
            is HistogramPoint -> histogramDelta(point, before as? HistogramPoint, since)
            is GaugePoint, is StateSetPoint, is InfoPoint -> point
        }
    }

    private fun sumDelta(point: SumPoint, before: SumPoint?, since: Instant): Point {
        val earlier = before ?: return point
        return SumPoint(labels = point.labels, start = since, value = point.value - earlier.value)
    }

    private fun histogramDelta(point: HistogramPoint, before: HistogramPoint?, since: Instant): Point {
        val earlier = before ?: return point
        return HistogramPoint(
            labels = point.labels,
            start = since,
            count = point.count - earlier.count,
            sum = point.sum - earlier.sum,
            min = null,
            max = null,
            bucketCounts = point.bucketCounts.zip(earlier.bucketCounts, Long::minus),
        )
    }
}
