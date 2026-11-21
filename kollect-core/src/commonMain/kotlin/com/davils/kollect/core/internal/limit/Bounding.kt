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

package com.davils.kollect.core.internal.limit

import com.davils.kollect.core.MetricViolationCodes
import com.davils.kore.core.validation.ValidationScope

private const val HASH_FACTOR: Int = 31

internal fun ValidationScope.bounded(bound: Bound) {
    val value = bound.value
    if (value < 1) fail(bound.property, "must be at least 1, but was $value", MetricViolationCodes.NOT_POSITIVE)
    if (value <= bound.ceiling) return
    fail(bound.property, "must not exceed ${bound.ceiling}, but was $value", MetricViolationCodes.ABOVE_HARD_LIMIT)
}

internal fun ValidationScope.bounded(bound: DurationBound) {
    val value = bound.value
    if (!value.isPositive()) fail(bound.property, "must be positive, but was $value", MetricViolationCodes.NOT_POSITIVE)
    if (value <= bound.ceiling) return
    fail(bound.property, "must not exceed ${bound.ceiling}, but was $value", MetricViolationCodes.ABOVE_HARD_LIMIT)
}

internal fun ValidationScope.boundedAll(counts: List<Bound>, durations: List<DurationBound>) {
    counts.forEach { bound -> bounded(bound) }
    durations.forEach { bound -> bounded(bound) }
}

internal fun sameBounds(
    counts: List<Bound>,
    durations: List<DurationBound>,
    otherCounts: List<Bound>,
    otherDurations: List<DurationBound>,
): Boolean = counts.map(Bound::value) == otherCounts.map(Bound::value) &&
    durations.map(DurationBound::value) == otherDurations.map(DurationBound::value)

internal fun hashOfBounds(counts: List<Bound>, durations: List<DurationBound>): Int =
    HASH_FACTOR * counts.map(Bound::value).hashCode() + durations.map(DurationBound::value).hashCode()

internal fun showBounds(type: String, counts: List<Bound>, durations: List<DurationBound>): String {
    val shownCounts = counts.map { bound -> "${shortName(bound.property.name)}=${bound.value}" }
    val shownDurations = durations.map { bound -> "${shortName(bound.property.name)}=${bound.value}" }
    return (shownCounts + shownDurations).joinToString(prefix = "$type(", postfix = ")")
}

private fun shortName(property: String): String {
    val name = property.removePrefix("maximum")
    return name.take(1).lowercase() + name.drop(1)
}
