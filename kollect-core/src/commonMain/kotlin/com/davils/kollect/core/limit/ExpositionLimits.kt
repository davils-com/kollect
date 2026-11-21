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

package com.davils.kollect.core.limit

import com.davils.kollect.core.internal.limit.Bound
import com.davils.kollect.core.internal.limit.boundedAll
import com.davils.kollect.core.internal.limit.hashOfBounds
import com.davils.kollect.core.internal.limit.sameBounds
import com.davils.kollect.core.internal.limit.showBounds
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow

private const val SUBJECT: String = "exposition limits"

/**
 * The limits that bound one exposition: its size in bytes and the series it writes.
 *
 * An exposition beyond a limit is refused as a whole, never cut off, because a scraper would take a cut body for a
 * complete one. Every limit is at least 1 and at most its hard ceiling.
 *
 * @since 1.0.0
 */
public class ExpositionLimits internal constructor(builder: ExpositionLimitsBuilder) : Validatable {
    /**
     * The most bytes of one exposition: 16 MiB by default, at most [HARD_BYTES].
     *
     * @since 1.0.0
     */
    public val maximumBytes: Int = builder.maximumBytes

    /**
     * The most series of one exposition: 1,000,000 by default, at most [HARD_SERIES].
     *
     * @since 1.0.0
     */
    public val maximumSeries: Int = builder.maximumSeries

    init {
        validateOrThrow(SUBJECT)
    }

    private val counts: List<Bound>
        get() = listOf(
            Bound(property = ::maximumBytes, value = maximumBytes, ceiling = HARD_BYTES),
            Bound(property = ::maximumSeries, value = maximumSeries, ceiling = HARD_SERIES),
        )

    /**
     * Checks that every limit is at least 1 and none exceeds its hard ceiling.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        boundedAll(counts, emptyList())
    }

    /**
     * Tells whether [other] is limits with the same value for every limit.
     *
     * @param other the object to compare with.
     * @return true if [other] is equal limits.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ExpositionLimits) return false
        return sameBounds(
            counts = counts,
            durations = emptyList(),
            otherCounts = other.counts,
            otherDurations = emptyList(),
        )
    }

    /**
     * Returns a hash of every limit, consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = hashOfBounds(counts, emptyList())

    /**
     * Shows every limit, such as `ExpositionLimits(bytes=16777216, series=1000000)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = showBounds("ExpositionLimits", counts, emptyList())

    /**
     * The hard ceilings of the limits and the default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maximumBytes]: 512 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_BYTES: Int = 536_870_912

        /**
         * The hard ceiling of [maximumSeries]: 10,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SERIES: Int = 10_000_000

        internal const val DEFAULT_BYTES: Int = 16_777_216
        internal const val DEFAULT_SERIES: Int = 1_000_000

        /**
         * The limits of an exposition that sets none.
         *
         * @since 1.0.0
         */
        public val Default: ExpositionLimits = ExpositionLimits(ExpositionLimitsBuilder())
    }
}
