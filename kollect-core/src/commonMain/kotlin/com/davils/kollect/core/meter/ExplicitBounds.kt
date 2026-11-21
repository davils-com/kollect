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

package com.davils.kollect.core.meter

import com.davils.kollect.core.internal.text.Decimals
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The upper bounds of the buckets of a histogram, finite and strictly increasing.
 *
 * A histogram with the bounds `b0 < b1 < … < bn` counts a value `v` in the first bucket whose bound is at least `v`,
 * so bucket `i` holds the values in `(b(i-1), bi]`, and a last bucket holds every value above `bn`, the `+Inf` bucket
 * of Prometheus. The bounds are immutable and safe to share.
 *
 * @property values the bounds in increasing order.
 * @since 1.0.0
 */
public class ExplicitBounds private constructor(public val values: List<Double>) {
    /**
     * The number of buckets, one more than the number of bounds.
     *
     * @since 1.0.0
     */
    public val bucketCount: Int get() = values.size + 1

    /**
     * Tells whether [other] are the same bounds.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same bounds in the same order.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ExplicitBounds) return false
        return values == other.values
    }

    /**
     * Returns a hash of the bounds.
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the bounds, such as `ExplicitBounds[0.005, 0.01, 1]`.
     *
     * @return the bounds as text.
     * @since 1.0.0
     */
    override fun toString(): String = values.joinToString(
        prefix = "ExplicitBounds[",
        postfix = "]",
        transform = Decimals::text,
    )

    /**
     * Creates bounds and holds the defaults.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default bounds of OpenTelemetry for values without a unit of time: 0, 5, 10, 25, 50, 75, 100, 250,
         * 500, 750, 1,000, 2,500, 5,000, 7,500 and 10,000.
         *
         * @since 1.0.0
         */
        public val DEFAULT: ExplicitBounds = of(
            0.0, 5.0, 10.0, 25.0, 50.0, 75.0, 100.0, 250.0, 500.0, 750.0, 1_000.0, 2_500.0, 5_000.0, 7_500.0, 10_000.0,
        )

        /**
         * The default bounds of OpenTelemetry for durations in seconds: 5 ms to 10 s.
         *
         * @since 1.0.0
         */
        public val SECONDS: ExplicitBounds = of(
            0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0,
        )

        /**
         * Returns the bounds [values].
         *
         * @param values the bounds, finite and strictly increasing.
         * @return the bounds.
         * @throws IllegalArgumentException if a bound is not finite or not greater than the one before.
         * @since 1.0.0
         */
        public fun of(vararg values: Double): ExplicitBounds = of(values.asList())

        /**
         * Returns the bounds [values].
         *
         * @param values the bounds, finite and strictly increasing.
         * @return the bounds.
         * @throws IllegalArgumentException if a bound is not finite or not greater than the one before.
         * @since 1.0.0
         */
        public fun of(values: List<Double>): ExplicitBounds {
            val problem = problemOf(values)
            require(problem == null) { "The bounds of a histogram must be ${problem.orEmpty()}" }
            return ExplicitBounds(values.immutableList())
        }

        /**
         * Returns the bounds [values], or null if they are not finite and strictly increasing.
         *
         * @param values the bounds.
         * @return the bounds, or null.
         * @since 1.0.0
         */
        public fun ofOrNull(values: List<Double>): ExplicitBounds? {
            if (problemOf(values) != null) return null
            return ExplicitBounds(values.immutableList())
        }

        private fun problemOf(values: List<Double>): String? {
            if (!values.all(Double::isFinite)) return "finite"
            val isIncreasing = values.zipWithNext().all { (lower, upper) -> lower < upper }
            return "strictly increasing".takeUnless { isIncreasing }
        }
    }
}
