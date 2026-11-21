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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val SUBJECT: String = "registry limits"

/**
 * The limits that bound what one meter registry holds.
 *
 * Every structure of a registry that grows with its use has a limit here: the families, the series of one family and
 * of the whole registry, the labels of a series, the length of names, label values and descriptions, the buckets of a
 * histogram, the states of a state set, the running tasks of a long task timer and the filters. The registry checks a
 * limit before it does the work, so a declaration beyond a limit is refused, a series beyond a limit lands in the
 * overflow series of its family, and nothing grows memory first.
 *
 * Every count is at least 1 and every duration is positive, and none exceeds its hard ceiling, a `HARD_*` constant of
 * the companion that no configuration can raise. [Default] is the limits of a registry that sets none; other limits
 * are built with [registryLimits], which validates them.
 *
 * The limits are an immutable value and safe to share between threads.
 *
 * @since 1.0.0
 */
public class RegistryLimits internal constructor(builder: RegistryLimitsBuilder) : Validatable {
    /**
     * The most families a registry holds, its own families included: 1,024 by default, at most [HARD_FAMILIES].
     *
     * @since 1.0.0
     */
    public val maximumFamilies: Int = builder.maximumFamilies

    /**
     * The most series of one family, its overflow series included: 2,000 by default, at most
     * [HARD_SERIES_PER_FAMILY].
     *
     * A new series beyond the limit is recorded in the overflow series of its family, which carries the single label
     * `otel.metric.overflow="true"`, so totals stay exact.
     *
     * @since 1.0.0
     */
    public val maximumSeriesPerFamily: Int = builder.maximumSeriesPerFamily

    /**
     * The most series of the whole registry: 50,000 by default, at most [HARD_SERIES].
     *
     * Every family reserves one series for its overflow series when it is declared. Once the registry holds this many
     * series, new series of every family land in its overflow series and new families are refused.
     *
     * @since 1.0.0
     */
    public val maximumSeries: Int = builder.maximumSeries

    /**
     * The most label keys of one family: 16 by default, at most [HARD_LABELS].
     *
     * @since 1.0.0
     */
    public val maximumLabels: Int = builder.maximumLabels

    /**
     * The longest metric name and label key, in characters: 128 by default, at most [HARD_NAME_LENGTH].
     *
     * @since 1.0.0
     */
    public val maximumNameLength: Int = builder.maximumNameLength

    /**
     * The longest label value, in characters: 256 by default, at most [HARD_LABEL_VALUE_LENGTH].
     *
     * A longer value is refused, never truncated: the meter of such a series records nothing and the refusal is
     * counted.
     *
     * @since 1.0.0
     */
    public val maximumLabelValueLength: Int = builder.maximumLabelValueLength

    /**
     * The longest description of a family, in characters: 512 by default, at most [HARD_DESCRIPTION_LENGTH].
     *
     * @since 1.0.0
     */
    public val maximumDescriptionLength: Int = builder.maximumDescriptionLength

    /**
     * The most bucket bounds of a histogram: 64 by default, at most [HARD_BUCKETS].
     *
     * @since 1.0.0
     */
    public val maximumBuckets: Int = builder.maximumBuckets

    /**
     * The most states of a state set: 64 by default, at most [HARD_STATES].
     *
     * @since 1.0.0
     */
    public val maximumStates: Int = builder.maximumStates

    /**
     * The most tasks one long task timer series tracks at the same time: 10,000 by default, at most
     * [HARD_ACTIVE_TASKS].
     *
     * A task started beyond the limit is not tracked; its refusal is counted.
     *
     * @since 1.0.0
     */
    public val maximumActiveTasks: Int = builder.maximumActiveTasks

    /**
     * The most meter filters of a registry: 64 by default, at most [HARD_FILTERS].
     *
     * @since 1.0.0
     */
    public val maximumFilters: Int = builder.maximumFilters

    /**
     * The longest a snapshot waits for recordings that are in flight in a histogram: 100 milliseconds by default, at
     * most [HARD_SNAPSHOT_STALL].
     *
     * A histogram whose recordings do not finish in time is reported as unreadable by that snapshot and caught up by
     * the next one; a snapshot never waits without a bound.
     *
     * @since 1.0.0
     */
    public val maximumSnapshotStall: Duration = builder.maximumSnapshotStall

    init {
        validateOrThrow(SUBJECT)
    }

    private val counts: List<Bound>
        get() = listOf(
            Bound(property = ::maximumFamilies, value = maximumFamilies, ceiling = HARD_FAMILIES),
            Bound(
                property = ::maximumSeriesPerFamily,
                value = maximumSeriesPerFamily,
                ceiling = HARD_SERIES_PER_FAMILY,
            ),
            Bound(property = ::maximumSeries, value = maximumSeries, ceiling = HARD_SERIES),
            Bound(property = ::maximumLabels, value = maximumLabels, ceiling = HARD_LABELS),
            Bound(property = ::maximumNameLength, value = maximumNameLength, ceiling = HARD_NAME_LENGTH),
            Bound(
                property = ::maximumLabelValueLength,
                value = maximumLabelValueLength,
                ceiling = HARD_LABEL_VALUE_LENGTH,
            ),
            Bound(
                property = ::maximumDescriptionLength,
                value = maximumDescriptionLength,
                ceiling = HARD_DESCRIPTION_LENGTH,
            ),
            Bound(property = ::maximumBuckets, value = maximumBuckets, ceiling = HARD_BUCKETS),
            Bound(property = ::maximumStates, value = maximumStates, ceiling = HARD_STATES),
            Bound(property = ::maximumActiveTasks, value = maximumActiveTasks, ceiling = HARD_ACTIVE_TASKS),
            Bound(property = ::maximumFilters, value = maximumFilters, ceiling = HARD_FILTERS),
        )

    private val durations: List<DurationBound>
        get() = listOf(
            DurationBound(
                property = ::maximumSnapshotStall,
                value = maximumSnapshotStall,
                ceiling = HARD_SNAPSHOT_STALL,
            ),
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
        if (other !is RegistryLimits) return false
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
     * Shows every limit, such as `RegistryLimits(families=1024, seriesPerFamily=2000, …)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = showBounds("RegistryLimits", counts, durations)

    /**
     * The hard ceilings of the limits and the default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maximumFamilies]: 16,384.
         *
         * @since 1.0.0
         */
        public const val HARD_FAMILIES: Int = 16_384

        /**
         * The hard ceiling of [maximumSeriesPerFamily]: 100,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SERIES_PER_FAMILY: Int = 100_000

        /**
         * The hard ceiling of [maximumSeries]: 1,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SERIES: Int = 1_000_000

        /**
         * The hard ceiling of [maximumLabels]: 64.
         *
         * @since 1.0.0
         */
        public const val HARD_LABELS: Int = 64

        /**
         * The hard ceiling of [maximumNameLength]: 255 characters.
         *
         * @since 1.0.0
         */
        public const val HARD_NAME_LENGTH: Int = 255

        /**
         * The hard ceiling of [maximumLabelValueLength]: 4,096 characters.
         *
         * @since 1.0.0
         */
        public const val HARD_LABEL_VALUE_LENGTH: Int = 4_096

        /**
         * The hard ceiling of [maximumDescriptionLength]: 4,096 characters.
         *
         * @since 1.0.0
         */
        public const val HARD_DESCRIPTION_LENGTH: Int = 4_096

        /**
         * The hard ceiling of [maximumBuckets]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_BUCKETS: Int = 1_024

        /**
         * The hard ceiling of [maximumStates]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_STATES: Int = 1_024

        /**
         * The hard ceiling of [maximumActiveTasks]: 1,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_ACTIVE_TASKS: Int = 1_000_000

        /**
         * The hard ceiling of [maximumFilters]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_FILTERS: Int = 1_024

        /**
         * The hard ceiling of [maximumSnapshotStall]: ten seconds.
         *
         * @since 1.0.0
         */
        public val HARD_SNAPSHOT_STALL: Duration = 10.seconds

        internal const val DEFAULT_FAMILIES: Int = 1_024
        internal const val DEFAULT_SERIES_PER_FAMILY: Int = 2_000
        internal const val DEFAULT_SERIES: Int = 50_000
        internal const val DEFAULT_LABELS: Int = 16
        internal const val DEFAULT_NAME_LENGTH: Int = 128
        internal const val DEFAULT_LABEL_VALUE_LENGTH: Int = 256
        internal const val DEFAULT_DESCRIPTION_LENGTH: Int = 512
        internal const val DEFAULT_BUCKETS: Int = 64
        internal const val DEFAULT_STATES: Int = 64
        internal const val DEFAULT_ACTIVE_TASKS: Int = 10_000
        internal const val DEFAULT_FILTERS: Int = 64
        internal val DEFAULT_SNAPSHOT_STALL: Duration = 100.milliseconds

        /**
         * The limits of a registry that sets none, as each limit describes.
         *
         * @since 1.0.0
         */
        public val Default: RegistryLimits = RegistryLimits(RegistryLimitsBuilder())
    }
}
