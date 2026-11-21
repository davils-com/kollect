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

package com.davils.kollect.core.binder

import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The families that report a konfetti event bus, from [bindBusMetrics]: the published, rejected, dispatched,
 * dropped and queued envelopes of every topic, the counts of every consumer and the failures of observers and
 * interceptors.
 *
 * The families read one snapshot of the bus per registry snapshot. Topics and consumers are bounded by the limits of
 * the bus and of the registry; topics and consumers that disappear leave the families with the next snapshot.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class BusMetrics internal constructor(registrations: List<Registration<ObservedFamily>>) : AutoCloseable {
    /**
     * The registration of every family; a family whose name another family took is refused.
     *
     * @since 1.0.0
     */
    public val registrations: List<Registration<ObservedFamily>> = registrations.immutableList()

    /**
     * Removes every family from the registry.
     *
     * @since 1.0.0
     */
    override fun close() {
        registrations.forEach { registration -> registration.family.close() }
    }

    /**
     * Shows the binding, such as `BusMetrics(16 families)`.
     *
     * @return the binding as text.
     * @since 1.0.0
     */
    override fun toString(): String = "BusMetrics(${registrations.size} families)"
}
