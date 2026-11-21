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
import com.davils.kollect.core.internal.limit.DurationBound
import com.davils.kollect.core.internal.limit.boundedAll
import com.davils.kollect.core.internal.limit.hashOfBounds
import com.davils.kollect.core.internal.limit.sameBounds
import com.davils.kollect.core.internal.limit.showBounds
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val SUBJECT: String = "health limits"

/**
 * The limits that bound a health registry: its members, how deep its groups nest, how many checks run at the same
 * time, how long a check may run and how much history a rule keeps.
 *
 * Every count is at least 1 and every duration is positive, and none exceeds its hard ceiling.
 *
 * @since 1.0.0
 */
public class HealthLimits internal constructor(builder: HealthLimitsBuilder) : Validatable {
    /**
     * The most indicators, rules and groups of a health registry: 256 by default, at most [HARD_MEMBERS].
     *
     * @since 1.0.0
     */
    public val maximumMembers: Int = builder.maximumMembers

    /**
     * How deep groups may nest: 8 by default, at most [HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public val maximumDepth: Int = builder.maximumDepth

    /**
     * The most indicators that run at the same time during one evaluation: 16 by default, at most
     * [HARD_CONCURRENT_CHECKS].
     *
     * @since 1.0.0
     */
    public val maximumConcurrentChecks: Int = builder.maximumConcurrentChecks

    /**
     * The most readings a rate rule keeps of each series: 64 by default, at most [HARD_CHECKPOINTS].
     *
     * @since 1.0.0
     */
    public val maximumCheckpoints: Int = builder.maximumCheckpoints

    /**
     * The longest timeout an indicator may have: 30 seconds by default, at most [HARD_CHECK_TIMEOUT]. An indicator
     * that does not answer in time is Down.
     *
     * @since 1.0.0
     */
    public val maximumCheckTimeout: Duration = builder.maximumCheckTimeout

    init {
        validateOrThrow(SUBJECT)
    }

    private val counts: List<Bound>
        get() = listOf(
            Bound(property = ::maximumMembers, value = maximumMembers, ceiling = HARD_MEMBERS),
            Bound(property = ::maximumDepth, value = maximumDepth, ceiling = HARD_DEPTH),
            Bound(
                property = ::maximumConcurrentChecks,
                value = maximumConcurrentChecks,
                ceiling = HARD_CONCURRENT_CHECKS,
            ),
            Bound(property = ::maximumCheckpoints, value = maximumCheckpoints, ceiling = HARD_CHECKPOINTS),
        )

    private val durations: List<DurationBound>
        get() = listOf(
            DurationBound(property = ::maximumCheckTimeout, value = maximumCheckTimeout, ceiling = HARD_CHECK_TIMEOUT),
        )

    /**
     * Checks that every count is at least 1, every duration is positive and nothing exceeds its hard ceiling.
     *
     * @receiver the validation that collects the violations.
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        boundedAll(counts, durations)
    }

    /**
     * Tells whether [other] is limits with the same value for every limit.
     *
     * @param other the object to compare with.
     * @return true if [other] is equal limits.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is HealthLimits) return false
        return sameBounds(
            counts = counts,
            durations = durations,
            otherCounts = other.counts,
            otherDurations = other.durations,
        )
    }

    /**
     * Returns a hash of every limit, consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = hashOfBounds(counts, durations)

    /**
     * Shows every limit.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = showBounds("HealthLimits", counts, durations)

    /**
     * The hard ceilings of the limits and the default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maximumMembers]: 4,096.
         *
         * @since 1.0.0
         */
        public const val HARD_MEMBERS: Int = 4_096

        /**
         * The hard ceiling of [maximumDepth]: 32.
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = 32

        /**
         * The hard ceiling of [maximumConcurrentChecks]: 256.
         *
         * @since 1.0.0
         */
        public const val HARD_CONCURRENT_CHECKS: Int = 256

        /**
         * The hard ceiling of [maximumCheckpoints]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_CHECKPOINTS: Int = 1_024

        /**
         * The hard ceiling of [maximumCheckTimeout]: five minutes.
         *
         * @since 1.0.0
         */
        public val HARD_CHECK_TIMEOUT: Duration = 5.minutes

        internal const val DEFAULT_MEMBERS: Int = 256
        internal const val DEFAULT_DEPTH: Int = 8
        internal const val DEFAULT_CONCURRENT_CHECKS: Int = 16
        internal const val DEFAULT_CHECKPOINTS: Int = 64
        internal val DEFAULT_CHECK_TIMEOUT: Duration = 30.seconds

        /**
         * The limits that apply when none are set.
         *
         * @since 1.0.0
         */
        public val Default: HealthLimits = HealthLimits(HealthLimitsBuilder())
    }
}
