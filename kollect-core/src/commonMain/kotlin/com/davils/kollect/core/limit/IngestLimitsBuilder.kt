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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration

/**
 * Builds [IngestLimits]; every property starts from the limits [ingestLimits] starts from.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class IngestLimitsBuilder internal constructor() : ValidatingBuilder<IngestLimits>() {
    override val subject: String get() = "ingest limits"

    /**
     * See [IngestLimits.maximumPayloadBytes].
     *
     * @since 1.0.0
     */
    public var maximumPayloadBytes: Int = IngestLimits.DEFAULT_PAYLOAD_BYTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumPointsPerRequest].
     *
     * @since 1.0.0
     */
    public var maximumPointsPerRequest: Int = IngestLimits.DEFAULT_POINTS_PER_REQUEST
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumAttributes].
     *
     * @since 1.0.0
     */
    public var maximumAttributes: Int = IngestLimits.DEFAULT_ATTRIBUTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumStringBytes].
     *
     * @since 1.0.0
     */
    public var maximumStringBytes: Int = IngestLimits.DEFAULT_STRING_BYTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumBuckets].
     *
     * @since 1.0.0
     */
    public var maximumBuckets: Int = IngestLimits.DEFAULT_BUCKETS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumSources].
     *
     * @since 1.0.0
     */
    public var maximumSources: Int = IngestLimits.DEFAULT_SOURCES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumSeriesPerSource].
     *
     * @since 1.0.0
     */
    public var maximumSeriesPerSource: Int = IngestLimits.DEFAULT_SERIES_PER_SOURCE
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumSeries].
     *
     * @since 1.0.0
     */
    public var maximumSeries: Int = IngestLimits.DEFAULT_SERIES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumPointsPerMinute].
     *
     * @since 1.0.0
     */
    public var maximumPointsPerMinute: Int = IngestLimits.DEFAULT_POINTS_PER_MINUTE
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.instanceExpiry].
     *
     * @since 1.0.0
     */
    public var instanceExpiry: Duration = IngestLimits.DEFAULT_INSTANCE_EXPIRY
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.seriesExpiry].
     *
     * @since 1.0.0
     */
    public var seriesExpiry: Duration = IngestLimits.DEFAULT_SERIES_EXPIRY
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumClockSkew].
     *
     * @since 1.0.0
     */
    public var maximumClockSkew: Duration = IngestLimits.DEFAULT_CLOCK_SKEW
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [IngestLimits.maximumPointAge].
     *
     * @since 1.0.0
     */
    public var maximumPointAge: Duration = IngestLimits.DEFAULT_POINT_AGE
        set(value) {
            checkOpen()
            field = value
        }

    internal fun from(base: IngestLimits) {
        maximumPayloadBytes = base.maximumPayloadBytes
        maximumPointsPerRequest = base.maximumPointsPerRequest
        maximumAttributes = base.maximumAttributes
        maximumStringBytes = base.maximumStringBytes
        maximumBuckets = base.maximumBuckets
        maximumSources = base.maximumSources
        maximumSeriesPerSource = base.maximumSeriesPerSource
        maximumSeries = base.maximumSeries
        maximumPointsPerMinute = base.maximumPointsPerMinute
        instanceExpiry = base.instanceExpiry
        seriesExpiry = base.seriesExpiry
        maximumClockSkew = base.maximumClockSkew
        maximumPointAge = base.maximumPointAge
    }

    override fun assemble(): IngestLimits = IngestLimits(this)
}
