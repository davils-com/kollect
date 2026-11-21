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
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val SUBJECT: String = "ingest limits"

/**
 * The limits that bound an OTLP ingestion, whose every input may come from an attacker: the size and shape of one
 * request, the sources and series it holds, the rate of every source and the times it accepts.
 *
 * The ingestion checks a limit before it does the work. Every count is at least 1 and every duration is positive, and
 * none exceeds its hard ceiling.
 *
 * @since 1.0.0
 */
public class IngestLimits internal constructor(builder: IngestLimitsBuilder) : Validatable {
    /**
     * The most bytes of one request body, decompressed: 4 MiB by default, at most [HARD_PAYLOAD_BYTES]. A larger body
     * is refused with [com.davils.kollect.core.otlp.IngestVerdict.TooLarge] before it is read.
     *
     * @since 1.0.0
     */
    public val maximumPayloadBytes: Int = builder.maximumPayloadBytes

    /**
     * The most points of one request: 10,000 by default, at most [HARD_POINTS_PER_REQUEST].
     *
     * @since 1.0.0
     */
    public val maximumPointsPerRequest: Int = builder.maximumPointsPerRequest

    /**
     * The most attributes of one point: 32 by default, at most [HARD_ATTRIBUTES].
     *
     * @since 1.0.0
     */
    public val maximumAttributes: Int = builder.maximumAttributes

    /**
     * The most bytes of one string of a request, such as a name or an attribute value: 1,024 by default, at most
     * [HARD_STRING_BYTES].
     *
     * @since 1.0.0
     */
    public val maximumStringBytes: Int = builder.maximumStringBytes

    /**
     * The most buckets of one histogram point: 160 by default, at most [HARD_BUCKETS].
     *
     * @since 1.0.0
     */
    public val maximumBuckets: Int = builder.maximumBuckets

    /**
     * The most sources an ingestion holds series of: 10,000 by default, at most [HARD_SOURCES].
     *
     * @since 1.0.0
     */
    public val maximumSources: Int = builder.maximumSources

    /**
     * The most series one source may hold, every instance counted: 2,000 by default, at most [HARD_SERIES_PER_SOURCE].
     *
     * @since 1.0.0
     */
    public val maximumSeriesPerSource: Int = builder.maximumSeriesPerSource

    /**
     * The most series of every source together: 200,000 by default, at most [HARD_SERIES].
     *
     * @since 1.0.0
     */
    public val maximumSeries: Int = builder.maximumSeries

    /**
     * The most points one source may send per minute, measured as a token bucket on the clock of the ingestion: 60,000
     * by default, at most [HARD_POINTS_PER_MINUTE].
     *
     * @since 1.0.0
     */
    public val maximumPointsPerMinute: Int = builder.maximumPointsPerMinute

    /**
     * How long an instance of a source may stay silent before its series are retired: five minutes by default, at most
     * [HARD_INSTANCE_EXPIRY]. The sums of a retired instance stay in the totals of its series, so counters stay
     * monotonic.
     *
     * @since 1.0.0
     */
    public val instanceExpiry: Duration = builder.instanceExpiry

    /**
     * How long a series may stay without points before it is removed: one hour by default, at most
     * [HARD_SERIES_EXPIRY].
     *
     * @since 1.0.0
     */
    public val seriesExpiry: Duration = builder.seriesExpiry

    /**
     * How far the time of a point may lie ahead of the clock of the ingestion: five minutes by default, at most
     * [HARD_CLOCK_SKEW].
     *
     * @since 1.0.0
     */
    public val maximumClockSkew: Duration = builder.maximumClockSkew

    /**
     * How far the time of a point may lie behind the clock of the ingestion: one hour by default, at most
     * [HARD_POINT_AGE].
     *
     * @since 1.0.0
     */
    public val maximumPointAge: Duration = builder.maximumPointAge

    init {
        validateOrThrow(SUBJECT)
    }

    private val counts: List<Bound>
        get() = listOf(
            Bound(property = ::maximumPayloadBytes, value = maximumPayloadBytes, ceiling = HARD_PAYLOAD_BYTES),
            Bound(
                property = ::maximumPointsPerRequest,
                value = maximumPointsPerRequest,
                ceiling = HARD_POINTS_PER_REQUEST,
            ),
            Bound(property = ::maximumAttributes, value = maximumAttributes, ceiling = HARD_ATTRIBUTES),
            Bound(property = ::maximumStringBytes, value = maximumStringBytes, ceiling = HARD_STRING_BYTES),
            Bound(property = ::maximumBuckets, value = maximumBuckets, ceiling = HARD_BUCKETS),
            Bound(property = ::maximumSources, value = maximumSources, ceiling = HARD_SOURCES),
            Bound(
                property = ::maximumSeriesPerSource,
                value = maximumSeriesPerSource,
                ceiling = HARD_SERIES_PER_SOURCE,
            ),
            Bound(property = ::maximumSeries, value = maximumSeries, ceiling = HARD_SERIES),
            Bound(
                property = ::maximumPointsPerMinute,
                value = maximumPointsPerMinute,
                ceiling = HARD_POINTS_PER_MINUTE,
            ),
        )

    private val durations: List<DurationBound>
        get() = listOf(
            DurationBound(property = ::instanceExpiry, value = instanceExpiry, ceiling = HARD_INSTANCE_EXPIRY),
            DurationBound(property = ::seriesExpiry, value = seriesExpiry, ceiling = HARD_SERIES_EXPIRY),
            DurationBound(property = ::maximumClockSkew, value = maximumClockSkew, ceiling = HARD_CLOCK_SKEW),
            DurationBound(property = ::maximumPointAge, value = maximumPointAge, ceiling = HARD_POINT_AGE),
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
        if (other !is IngestLimits) return false
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
     * Shows every limit, such as `IngestLimits(payloadBytes=4194304, …)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = showBounds("IngestLimits", counts, durations)

    /**
     * The hard ceilings of the limits and the default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maximumPayloadBytes]: 67,108,864.
         *
         * @since 1.0.0
         */
        public const val HARD_PAYLOAD_BYTES: Int = 67_108_864

        /**
         * The hard ceiling of [maximumPointsPerRequest]: 1,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_POINTS_PER_REQUEST: Int = 1_000_000

        /**
         * The hard ceiling of [maximumAttributes]: 128.
         *
         * @since 1.0.0
         */
        public const val HARD_ATTRIBUTES: Int = 128

        /**
         * The hard ceiling of [maximumStringBytes]: 16,384.
         *
         * @since 1.0.0
         */
        public const val HARD_STRING_BYTES: Int = 16_384

        /**
         * The hard ceiling of [maximumBuckets]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_BUCKETS: Int = 1_024

        /**
         * The hard ceiling of [maximumSources]: 1,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SOURCES: Int = 1_000_000

        /**
         * The hard ceiling of [maximumSeriesPerSource]: 1,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SERIES_PER_SOURCE: Int = 1_000_000

        /**
         * The hard ceiling of [maximumSeries]: 10,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_SERIES: Int = 10_000_000

        /**
         * The hard ceiling of [maximumPointsPerMinute]: 10,000,000.
         *
         * @since 1.0.0
         */
        public const val HARD_POINTS_PER_MINUTE: Int = 10_000_000

        /**
         * The hard ceiling of [instanceExpiry]: one day.
         *
         * @since 1.0.0
         */
        public val HARD_INSTANCE_EXPIRY: Duration = 1.days

        /**
         * The hard ceiling of [seriesExpiry]: seven days.
         *
         * @since 1.0.0
         */
        public val HARD_SERIES_EXPIRY: Duration = 7.days

        /**
         * The hard ceiling of [maximumClockSkew]: one hour.
         *
         * @since 1.0.0
         */
        public val HARD_CLOCK_SKEW: Duration = 1.hours

        /**
         * The hard ceiling of [maximumPointAge]: one day.
         *
         * @since 1.0.0
         */
        public val HARD_POINT_AGE: Duration = 1.days

        internal const val DEFAULT_PAYLOAD_BYTES: Int = 4_194_304
        internal const val DEFAULT_POINTS_PER_REQUEST: Int = 10_000
        internal const val DEFAULT_ATTRIBUTES: Int = 32
        internal const val DEFAULT_STRING_BYTES: Int = 1_024
        internal const val DEFAULT_BUCKETS: Int = 160
        internal const val DEFAULT_SOURCES: Int = 10_000
        internal const val DEFAULT_SERIES_PER_SOURCE: Int = 2_000
        internal const val DEFAULT_SERIES: Int = 200_000
        internal const val DEFAULT_POINTS_PER_MINUTE: Int = 60_000
        internal val DEFAULT_INSTANCE_EXPIRY: Duration = 5.minutes
        internal val DEFAULT_SERIES_EXPIRY: Duration = 1.hours
        internal val DEFAULT_CLOCK_SKEW: Duration = 5.minutes
        internal val DEFAULT_POINT_AGE: Duration = 1.hours

        /**
         * The limits of an ingestion that sets none.
         *
         * @since 1.0.0
         */
        public val Default: IngestLimits = IngestLimits(IngestLimitsBuilder())
    }
}
