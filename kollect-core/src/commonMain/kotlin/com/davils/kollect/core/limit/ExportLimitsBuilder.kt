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
 * Builds [ExportLimits]; every property starts from the limits [exportLimits] starts from.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ExportLimitsBuilder internal constructor() : ValidatingBuilder<ExportLimits>() {
    override val subject: String get() = "export limits"

    /**
     * See [ExportLimits.maximumQueuedBatches].
     *
     * @since 1.0.0
     */
    public var maximumQueuedBatches: Int = ExportLimits.DEFAULT_QUEUED_BATCHES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExportLimits.maximumPayloadBytes].
     *
     * @since 1.0.0
     */
    public var maximumPayloadBytes: Int = ExportLimits.DEFAULT_PAYLOAD_BYTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExportLimits.maximumPointsPerPayload].
     *
     * @since 1.0.0
     */
    public var maximumPointsPerPayload: Int = ExportLimits.DEFAULT_POINTS_PER_PAYLOAD
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExportLimits.maximumAttempts].
     *
     * @since 1.0.0
     */
    public var maximumAttempts: Int = ExportLimits.DEFAULT_ATTEMPTS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExportLimits.maximumBackoff].
     *
     * @since 1.0.0
     */
    public var maximumBackoff: Duration = ExportLimits.DEFAULT_BACKOFF
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExportLimits.maximumRetryAfter].
     *
     * @since 1.0.0
     */
    public var maximumRetryAfter: Duration = ExportLimits.DEFAULT_RETRY_AFTER
        set(value) {
            checkOpen()
            field = value
        }

    internal fun from(base: ExportLimits) {
        maximumQueuedBatches = base.maximumQueuedBatches
        maximumPayloadBytes = base.maximumPayloadBytes
        maximumPointsPerPayload = base.maximumPointsPerPayload
        maximumAttempts = base.maximumAttempts
        maximumBackoff = base.maximumBackoff
        maximumRetryAfter = base.maximumRetryAfter
    }

    override fun assemble(): ExportLimits = ExportLimits(this)
}
