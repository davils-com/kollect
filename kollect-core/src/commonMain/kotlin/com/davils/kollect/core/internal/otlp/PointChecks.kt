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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.otlp.IngestRejection
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

internal class PointChecks(private val limits: IngestLimits) {
    fun keysOf(point: DecodedPoint): List<LabelKey>? {
        val attributes = point.attributes ?: return null
        val keys = attributes.map { (key, _) -> LabelKey.ofOrNull(key) }
        val isValid = keys.all { key -> key != null && !key.isReserved && key != SOURCE_KEY }
        val sorted = keys.filterNotNull().sorted()
        return sorted.takeIf { isValid && sorted.distinct().size == sorted.size }
    }

    fun rejectionOf(family: IngestedFamily, point: DecodedPoint, now: Instant): IngestRejection? =
        labelRejectionOf(family, point) ?: timeRejectionOf(point, now) ?: valueRejectionOf(family, point)

    fun valuesOf(family: IngestedFamily, point: DecodedPoint): List<String> {
        val attributes = point.attributes.orEmpty()
        return family.keys.map { key -> attributes.first { (name, _) -> name == key.value }.second }
    }

    private fun labelRejectionOf(family: IngestedFamily, point: DecodedPoint): IngestRejection? {
        val keys = keysOf(point)
        val areValuesValid = point.attributes.orEmpty().all { (_, value) ->
            TextRules.isValidText(
                value,
                limits.maximumStringBytes,
            )
        }
        if (keys == null || !areValuesValid) return IngestRejection.InvalidLabels
        return IngestRejection.Mismatch.takeIf { keys != family.keys }
    }

    private fun timeRejectionOf(point: DecodedPoint, now: Instant): IngestRejection? {
        val time = Instant.fromEpochSeconds(0L) + point.time.nanoseconds
        val isAhead = time > now + limits.maximumClockSkew
        val isBehind = time < now - limits.maximumPointAge
        return IngestRejection.OutOfTime.takeIf { point.time <= 0L || isAhead || isBehind }
    }

    private fun valueRejectionOf(family: IngestedFamily, point: DecodedPoint): IngestRejection? {
        val isHistogram = family.kind == MeterKind.Histogram
        val isInRange = family.kind != MeterKind.Counter || point.value >= 0.0
        val isValid = isValidHistogram(point).takeIf { isHistogram } ?: (point.value.isFinite() && isInRange)
        if (!isValid) return IngestRejection.InvalidValue
        return IngestRejection.Mismatch.takeIf { isHistogram && point.bounds != family.bounds }
    }

    private fun isValidHistogram(point: DecodedPoint): Boolean {
        val bounds = point.bounds
        val isIncreasing = bounds.all(Double::isFinite) && bounds.zipWithNext().all { (lower, upper) -> lower < upper }
        val areCountsValid = point.buckets.size == bounds.size + 1 && point.buckets.all { count -> count >= 0L }
        return isIncreasing && areCountsValid && point.sum.isFinite() && isSummedTo(point.buckets, point.count)
    }

    private fun isSummedTo(buckets: List<Long>, count: Long): Boolean {
        var total = 0L
        var isOverflowing = false
        buckets.forEach { bucket ->
            isOverflowing = isOverflowing || bucket > Long.MAX_VALUE - total
            total += bucket
        }
        return !isOverflowing && total == count
    }

    companion object {
        val SOURCE_KEY: LabelKey = LabelKey.of("kollect.source")
    }
}
