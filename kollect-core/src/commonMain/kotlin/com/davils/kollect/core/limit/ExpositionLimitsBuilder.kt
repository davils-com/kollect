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

/**
 * Builds [ExpositionLimits]; every property starts from the limits [expositionLimits] starts from.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ExpositionLimitsBuilder internal constructor() : ValidatingBuilder<ExpositionLimits>() {
    override val subject: String get() = "exposition limits"

    /**
     * See [ExpositionLimits.maximumBytes].
     *
     * @since 1.0.0
     */
    public var maximumBytes: Int = ExpositionLimits.DEFAULT_BYTES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [ExpositionLimits.maximumSeries].
     *
     * @since 1.0.0
     */
    public var maximumSeries: Int = ExpositionLimits.DEFAULT_SERIES
        set(value) {
            checkOpen()
            field = value
        }

    internal fun from(base: ExpositionLimits) {
        maximumBytes = base.maximumBytes
        maximumSeries = base.maximumSeries
    }

    override fun assemble(): ExpositionLimits = ExpositionLimits(this)
}
