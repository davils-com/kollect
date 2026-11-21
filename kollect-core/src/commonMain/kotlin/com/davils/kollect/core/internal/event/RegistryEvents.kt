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

package com.davils.kollect.core.internal.event

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.event.MetricTopics
import com.davils.kollect.core.event.RegistryEvent
import com.davils.kollect.core.internal.registry.RegistryListener
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.Unreadable
import com.davils.konfetti.core.EventBus

internal class RegistryEvents(bus: EventBus) : RegistryListener {
    private val outbox: Outbox<RegistryEvent> = Outbox(bus, MetricTopics.REGISTRY)

    val rejected: Long get() = outbox.rejected

    override fun declared(descriptor: Descriptor) = outbox.add(RegistryEvent.FamilyDeclared(descriptor))

    override fun removed(name: MetricName) = outbox.add(RegistryEvent.FamilyRemoved(name))

    override fun refused(name: String, refusal: Refusal) = outbox.add(RegistryEvent.DeclarationRefused(name, refusal))

    override fun overflowed(name: MetricName) = outbox.add(RegistryEvent.FamilyOverflowed(name))

    override fun unreadable(entry: Unreadable) = outbox.add(RegistryEvent.FamilyUnreadable(entry.name, entry.reason))

    override fun rejected(rejection: Rejection, count: Long) =
        outbox.add(RegistryEvent.MeasurementsRejected(rejection, count))

    override fun flush() = outbox.flush()
}
