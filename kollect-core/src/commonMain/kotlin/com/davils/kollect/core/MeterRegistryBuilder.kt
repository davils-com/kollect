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

import com.davils.kollect.core.filter.MeterFilter
import com.davils.kollect.core.internal.event.RegistryEvents
import com.davils.kollect.core.internal.registry.RegistryListener
import com.davils.kollect.core.limit.RegistryLimits
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Configures a [MeterRegistry] while [meterRegistry] builds it: its limits and its filters.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MeterRegistryBuilder internal constructor() {
    /**
     * The limits of the registry; [RegistryLimits.Default] by default.
     *
     * @since 1.0.0
     */
    public var limits: RegistryLimits = RegistryLimits.Default

    internal val filters: MutableList<MeterFilter> = mutableListOf()

    internal var listener: RegistryListener = RegistryListener.None

    /**
     * Publishes the events of the registry on the topic [com.davils.kollect.core.event.MetricTopics.REGISTRY] of
     * [bus], declaring the topic if the bus has none.
     *
     * The registry collects events while it works and publishes them after each declaration and snapshot with
     * `offer`, so it never waits for the bus; an event the bus rejects, or one beyond 1,024 waiting events, is dropped
     * and counted in the family `kollect.events.rejected`.
     *
     * @param bus the bus.
     * @throws com.davils.konfetti.core.error.TopicTypeMismatchException if the bus has the topic for another event
     *   type.
     * @since 1.0.0
     */
    public fun events(bus: EventBus) {
        listener = RegistryEvents(bus)
    }

    /**
     * Adds [filter] after the filters added before.
     *
     * @param filter the filter.
     * @throws IllegalArgumentException if the registry would have more than [RegistryLimits.maximumFilters] filters.
     * @since 1.0.0
     */
    public fun filter(filter: MeterFilter) {
        require(filters.size < limits.maximumFilters) { "A registry has at most ${limits.maximumFilters} filters" }
        filters.add(filter)
    }
}
