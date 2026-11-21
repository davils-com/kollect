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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val SUBJECT: String = "export limits"

/**
 * The limits that bound the OTLP export of a process: what it holds while a receiver is unreachable, how large one
 * request grows and how often and how long it retries.
 *
 * Every count is at least 1 and every duration is positive, and none exceeds its hard ceiling.
 *
 * @since 1.0.0
 */
public class ExportLimits internal constructor(builder: ExportLimitsBuilder) : Validatable {
    /**
     * The most batches an exporter holds while they wait to be sent: 8 by default, at most [HARD_QUEUED_BATCHES].
     *
     * @since 1.0.0
     */
    public val maximumQueuedBatches: Int = builder.maximumQueuedBatches

    /**
     * The most bytes of one OTLP request: 1 MiB by default, at most [HARD_PAYLOAD_BYTES].
     *
     * @since 1.0.0
     */
    public val maximumPayloadBytes: Int = builder.maximumPayloadBytes

    /**
     * The most points of one OTLP request: 5,000 by default, at most [HARD_POINTS_PER_PAYLOAD].
     *
     * @since 1.0.0
     */
    public val maximumPointsPerPayload: Int = builder.maximumPointsPerPayload

    /**
     * The most attempts to send one request: 8 by default, at most [HARD_ATTEMPTS].
     *
     * @since 1.0.0
     */
    public val maximumAttempts: Int = builder.maximumAttempts

    /**
     * The longest wait between two attempts: one minute by default, at most [HARD_BACKOFF].
     *
     * @since 1.0.0
     */
    public val maximumBackoff: Duration = builder.maximumBackoff

    /**
     * The longest wait a receiver may ask for when it throttles: five minutes by default, at most [HARD_BACKOFF].
     *
     * @since 1.0.0
     */
    public val maximumRetryAfter: Duration = builder.maximumRetryAfter

    init {
        validateOrThrow(SUBJECT)
    }

    private val counts: List<Bound>
        get() = listOf(
            Bound(property = ::maximumQueuedBatches, value = maximumQueuedBatches, ceiling = HARD_QUEUED_BATCHES),
            Bound(property = ::maximumPayloadBytes, value = maximumPayloadBytes, ceiling = HARD_PAYLOAD_BYTES),
            Bound(
                property = ::maximumPointsPerPayload,
                value = maximumPointsPerPayload,
                ceiling = HARD_POINTS_PER_PAYLOAD,
            ),
            Bound(property = ::maximumAttempts, value = maximumAttempts, ceiling = HARD_ATTEMPTS),
        )

    private val durations: List<DurationBound>
        get() = listOf(
            DurationBound(property = ::maximumBackoff, value = maximumBackoff, ceiling = HARD_BACKOFF),
            DurationBound(property = ::maximumRetryAfter, value = maximumRetryAfter, ceiling = HARD_BACKOFF),
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
        if (other !is ExportLimits) return false
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
     * Shows every limit, such as `ExportLimits(queuedBatches=8, …)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String = showBounds("ExportLimits", counts, durations)

    /**
     * The hard ceilings of the limits and the default limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maximumQueuedBatches]: 1,024.
         *
         * @since 1.0.0
         */
        public const val HARD_QUEUED_BATCHES: Int = 1_024

        /**
         * The hard ceiling of [maximumPayloadBytes]: 16 MiB.
         *
         * @since 1.0.0
         */
        public const val HARD_PAYLOAD_BYTES: Int = 16_777_216

        /**
         * The hard ceiling of [maximumPointsPerPayload]: 100,000.
         *
         * @since 1.0.0
         */
        public const val HARD_POINTS_PER_PAYLOAD: Int = 100_000

        /**
         * The hard ceiling of [maximumAttempts]: 100.
         *
         * @since 1.0.0
         */
        public const val HARD_ATTEMPTS: Int = 100

        /**
         * The hard ceiling of [maximumBackoff] and [maximumRetryAfter]: one hour.
         *
         * @since 1.0.0
         */
        public val HARD_BACKOFF: Duration = 1.hours

        internal const val DEFAULT_QUEUED_BATCHES: Int = 8
        internal const val DEFAULT_PAYLOAD_BYTES: Int = 1_048_576
        internal const val DEFAULT_POINTS_PER_PAYLOAD: Int = 5_000
        internal const val DEFAULT_ATTEMPTS: Int = 8
        internal val DEFAULT_BACKOFF: Duration = 60.seconds
        internal val DEFAULT_RETRY_AFTER: Duration = 5.minutes

        /**
         * The limits of an exporter that sets none.
         *
         * @since 1.0.0
         */
        public val Default: ExportLimits = ExportLimits(ExportLimitsBuilder())
    }
}
