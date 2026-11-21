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

package com.davils.kollect.core.meter

/**
 * What the callback of an observed family reports its values to while a snapshot collects it.
 *
 * The callback reports at most one value for each set of label values, in the order of the label keys of its family.
 * A value the family cannot take, such as NaN, a negative value of a counter, label values that are invalid, a second
 * value for the same labels or a series beyond
 * [com.davils.kollect.core.limit.RegistryLimits.maximumSeriesPerFamily], is rejected and counted. A sink is valid
 * only during the callback it is passed to.
 *
 * @since 1.0.0
 */
public interface ObservationSink {
    /**
     * Reports [value] for the series with [labelValues].
     *
     * @param value the value; a sum for an observed counter or up-down counter, the current value for a gauge.
     * @param labelValues one value for every label key of the family, in their order.
     * @since 1.0.0
     */
    public fun observe(value: Double, vararg labelValues: String)
}
