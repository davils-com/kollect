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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.internal.binder.BusFamilies
import com.davils.kollect.core.internal.binder.BusFamily
import com.davils.kollect.core.internal.binder.BusReader
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.meter.ObservationSink
import com.davils.konfetti.core.EventBus

/**
 * Reports the konfetti event bus [bus] in this registry; see [BusMetrics].
 *
 * @receiver the registry.
 * @param bus the bus.
 * @return the binding, which removes the families when it is closed.
 * @since 1.0.0
 */
public fun MeterRegistry.bindBusMetrics(bus: EventBus): BusMetrics {
    val reader = BusReader(bus, timeSource)
    val registrations = BusFamilies.ALL.map { family -> observedBus(family, reader) }
    return BusMetrics(registrations)
}

private fun MeterRegistry.observedBus(family: BusFamily, reader: BusReader): Registration<ObservedFamily> {
    val keys = keysOf(family)
    val observe: ObservationSink.() -> Unit = { report(family, reader) }
    val describe: FamilyBuilder.() -> Unit = {
        description = family.description
        labelKeyList(keys)
    }
    if (family.kind == MeterKind.Gauge) return observedGauge(family.name, describe, observe)
    return observedCounter(family.name, describe, observe)
}

private fun keysOf(family: BusFamily): List<String> = when (family) {
    is BusFamily.Topic -> listOf("topic")
    is BusFamily.Consumer -> listOf("topic", "consumer")
    is BusFamily.Bus -> emptyList()
}

private fun ObservationSink.report(family: BusFamily, reader: BusReader) {
    val snapshot = reader.snapshot()
    when (family) {
        is BusFamily.Topic -> snapshot.topics.forEach { topic ->
            observe(family.read(topic.metrics).toDouble(), topic.name.value)
        }

        is BusFamily.Consumer -> snapshot.topics.forEach { topic ->
            topic.consumers.forEach { (consumer, metrics) ->
                observe(family.read(metrics).toDouble(), topic.name.value, consumer.value)
            }
        }

        is BusFamily.Bus -> observe(family.read(snapshot).toDouble())
    }
}
