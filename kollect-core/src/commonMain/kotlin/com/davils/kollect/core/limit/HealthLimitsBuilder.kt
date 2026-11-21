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
 * Builds [HealthLimits]; every property starts from the limits [healthLimits] starts from.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HealthLimitsBuilder internal constructor() : ValidatingBuilder<HealthLimits>() {
    override val subject: String get() = "health limits"

    /**
     * See [HealthLimits.maximumMembers].
     *
     * @since 1.0.0
     */
    public var maximumMembers: Int = HealthLimits.DEFAULT_MEMBERS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [HealthLimits.maximumDepth].
     *
     * @since 1.0.0
     */
    public var maximumDepth: Int = HealthLimits.DEFAULT_DEPTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [HealthLimits.maximumConcurrentChecks].
     *
     * @since 1.0.0
     */
    public var maximumConcurrentChecks: Int = HealthLimits.DEFAULT_CONCURRENT_CHECKS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [HealthLimits.maximumCheckpoints].
     *
     * @since 1.0.0
     */
    public var maximumCheckpoints: Int = HealthLimits.DEFAULT_CHECKPOINTS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [HealthLimits.maximumCheckTimeout].
     *
     * @since 1.0.0
     */
    public var maximumCheckTimeout: Duration = HealthLimits.DEFAULT_CHECK_TIMEOUT
        set(value) {
            checkOpen()
            field = value
        }

    internal fun from(base: HealthLimits) {
        maximumMembers = base.maximumMembers
        maximumDepth = base.maximumDepth
        maximumConcurrentChecks = base.maximumConcurrentChecks
        maximumCheckpoints = base.maximumCheckpoints
        maximumCheckTimeout = base.maximumCheckTimeout
    }

    override fun assemble(): HealthLimits = HealthLimits(this)
}
