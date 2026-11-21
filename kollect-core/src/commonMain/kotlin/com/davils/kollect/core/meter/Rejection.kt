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
 * Why a registry rejected a measurement or a series instead of recording it.
 *
 * Every rejection is counted in the family `kollect.measurements.rejected` of the registry, labelled with [label].
 *
 * @property label the value of the `reason` label, such as `negative`.
 * @since 1.0.0
 */
public enum class Rejection(public val label: String) {
    /**
     * A counter was asked to add a negative amount, or a histogram or timer to record a negative value it does not
     * allow.
     *
     * @since 1.0.0
     */
    Negative("negative"),

    /**
     * A value was NaN or infinite.
     *
     * @since 1.0.0
     */
    NotFinite("not_finite"),

    /**
     * An amount exceeded [Counter.MAXIMUM_INCREMENT].
     *
     * @since 1.0.0
     */
    TooLarge("too_large"),

    /**
     * Label values had the wrong number, were too long or held a control character or a lone surrogate.
     *
     * @since 1.0.0
     */
    InvalidLabels("invalid_labels"),

    /**
     * A long task started beyond [com.davils.kollect.core.limit.RegistryLimits.maximumActiveTasks].
     *
     * @since 1.0.0
     */
    Untracked("untracked"),

    /**
     * An observation callback reported a series twice or more series than its family may hold.
     *
     * @since 1.0.0
     */
    Duplicate("duplicate"),
}
