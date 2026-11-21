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

package com.davils.kollect.core.health

import com.davils.kollect.core.internal.event.HealthEvents
import com.davils.kollect.core.internal.health.BuildState
import com.davils.kollect.core.limit.HealthLimits
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Declares the limits and the top-level members of a health registry.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class HealthRegistryBuilder internal constructor(state: BuildState) : HealthGroupBuilder(state, depth = 0) {
    /**
     * The limits of the registry; set them before the first member.
     *
     * @since 1.0.0
     */
    public var limits: HealthLimits
        get() = state.limits
        set(value) {
            require(state.members == 0) { "The limits of a health registry are set before its members" }
            state.limits = value
        }

    /**
     * Publishes every change of a status on the topic [com.davils.kollect.core.event.MetricTopics.HEALTH] of [bus],
     * declaring the topic if the bus has none. Events are published with `offer` after each evaluation.
     *
     * @param bus the bus.
     * @throws com.davils.konfetti.core.error.TopicTypeMismatchException if the bus has the topic for another event
     *   type.
     * @since 1.0.0
     */
    public fun events(bus: EventBus) {
        state.events = HealthEvents(bus)
    }
}
