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

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.Registration
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.declare.HistogramBuilder
import com.davils.kollect.core.internal.binder.DispatcherMeters
import com.davils.kollect.core.orThrow
import kotlinx.coroutines.CoroutineDispatcher

private const val PREFIX: String = "coroutine.dispatcher"
private val KEYED: FamilyBuilder.() -> Unit = { labelKeys("dispatcher") }
private val KEYED_TIMER: HistogramBuilder.() -> Unit = { labelKeys("dispatcher") }

/**
 * Wraps this dispatcher in a [MeteredDispatcher] that reports into [registry], labelled `dispatcher=`[name].
 *
 * The families are `coroutine.dispatcher.dispatched`, `.refused` and `.completed` (counters), `.queued` and
 * `.running` (up-down counters), and `.wait` and `.duration` (timers). Wrapping several dispatchers shares them.
 *
 * @receiver the dispatcher to wrap.
 * @param registry the registry.
 * @param name the name of the dispatcher, a valid label value.
 * @return the metered dispatcher.
 * @throws IllegalStateException if the registry refuses one of the families.
 * @since 1.0.0
 */
public fun CoroutineDispatcher.metered(registry: MeterRegistry, name: String): MeteredDispatcher {
    val meters = DispatcherMeters(
        dispatched = registry.counter(name = "$PREFIX.dispatched", configure = KEYED).seriesOf(name),
        refused = registry.counter(name = "$PREFIX.refused", configure = KEYED).seriesOf(name),
        completed = registry.counter(name = "$PREFIX.completed", configure = KEYED).seriesOf(name),
        queued = registry.upDownCounter(name = "$PREFIX.queued", configure = KEYED).seriesOf(name),
        running = registry.upDownCounter(name = "$PREFIX.running", configure = KEYED).seriesOf(name),
        wait = registry.timer(name = "$PREFIX.wait", configure = KEYED_TIMER).seriesOf(name),
        duration = registry.timer(name = "$PREFIX.duration", configure = KEYED_TIMER).seriesOf(name),
    )
    return MeteredDispatcher(this, meters, registry.timeSource)
}

/**
 * Creates a [JobMonitor] that reports into [registry], labelled `monitor=`[name]: the up-down counter
 * `coroutine.jobs.active` and the counter `coroutine.jobs.ended` with the label `outcome`.
 *
 * @receiver the registry.
 * @param name the name of the monitor, a valid label value.
 * @return the monitor.
 * @throws IllegalStateException if the registry refuses one of the families.
 * @since 1.0.0
 */
public fun MeterRegistry.jobMonitor(name: String): JobMonitor {
    val activeFamily = upDownCounter("coroutine.jobs.active") { labelKeys("monitor") }.orThrow()
    val ended = counter("coroutine.jobs.ended") { labelKeys("monitor", "outcome") }.orThrow()
    val active = activeFamily.with(name)
    return JobMonitor(name, active, ended)
}

private fun <M> Registration<Family<M>>.seriesOf(name: String): M = orThrow().with(name)
