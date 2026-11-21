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

package com.davils.kollect.core

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations kollect reports when it validates names, labels, units, limits and declarations.
 *
 * The codes are a contract: a code keeps its meaning across versions, so tools may match on it.
 *
 * @since 1.0.0
 */
public object MetricViolationCodes {
    /**
     * A count is below 1 or a duration is not positive.
     *
     * @since 1.0.0
     */
    public val NOT_POSITIVE: ViolationCode = ViolationCode("kollect.metric.not-positive")

    /**
     * A limit exceeds its hard ceiling.
     *
     * @since 1.0.0
     */
    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("kollect.metric.above-hard-limit")
}
