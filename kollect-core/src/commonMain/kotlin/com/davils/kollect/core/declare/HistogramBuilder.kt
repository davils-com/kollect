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

package com.davils.kollect.core.declare

import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Describes a histogram or a timer while it is declared: what a family has, its bucket bounds and whether it takes
 * negative values.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HistogramBuilder internal constructor(unit: MeasureUnit, bounds: ExplicitBounds) : FamilyBuilder(unit) {
    /**
     * The bucket bounds; at most [com.davils.kollect.core.limit.RegistryLimits.maximumBuckets] of them.
     *
     * @since 1.0.0
     */
    public var bounds: ExplicitBounds = bounds

    /**
     * Whether the histogram takes negative values; false by default, so a negative value is rejected and counted.
     * A timer never takes negative durations.
     *
     * @since 1.0.0
     */
    public var isNegativeAllowed: Boolean = false
}
