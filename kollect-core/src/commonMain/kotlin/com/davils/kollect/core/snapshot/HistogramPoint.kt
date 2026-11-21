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

package com.davils.kollect.core.snapshot

import com.davils.kollect.core.internal.text.Decimals
import com.davils.kollect.core.name.Labels
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import kotlin.time.Instant

private const val HASH_FACTOR: Int = 31

/**
 * The distribution of the values a histogram series recorded since [start].
 *
 * The point is consistent: [count] equals the sum of [bucketCounts], and [sum], [min] and [max] cover exactly the
 * values counted. The bounds of the buckets are those of the family.
 *
 * @property labels the labels of the series.
 * @property start when the distribution started.
 * @property count how many values were recorded.
 * @property sum the sum of the values.
 * @property min the smallest value, or null if no value was recorded.
 * @property max the largest value, or null if no value was recorded.
 * @since 1.0.0
 */
public class HistogramPoint internal constructor(
    override val labels: Labels,
    override val start: Instant,
    public val count: Long,
    public val sum: Double,
    public val min: Double?,
    public val max: Double?,
    bucketCounts: List<Long>,
) : Point {
    /**
     * How many values each bucket holds, not cumulative: one count per bound of the family and a last one for the
     * values above every bound.
     *
     * @since 1.0.0
     */
    public val bucketCounts: List<Long> = bucketCounts.immutableList()

    /**
     * Tells whether [other] is an equal point.
     *
     * @param other the object to compare with.
     * @return true if every part of [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is HistogramPoint) return false
        return labels == other.labels &&
            start == other.start &&
            count == other.count &&
            sum.equals(other.sum) &&
            min == other.min &&
            max == other.max &&
            bucketCounts == other.bucketCounts
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(labels, start, count, sum, min, max, bucketCounts)
        .fold(0) { hash, part -> HASH_FACTOR * hash + part.hashCode() }

    /**
     * Shows the point, such as `HistogramPoint({}, count=3, sum=1.5, buckets=[1, 2, 0])`.
     *
     * @return the point as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "HistogramPoint($labels, count=$count, sum=${Decimals.text(sum)}, buckets=$bucketCounts, start=$start)"
}
