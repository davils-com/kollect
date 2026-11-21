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

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.internal.platform.Guard
import com.davils.kollect.core.internal.platform.guard
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kollect.core.snapshot.Unreadability
import com.davils.kollect.core.snapshot.Unreadable
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.failure.contained
import kotlin.time.Instant

internal class ObservedCore(
    val descriptor: Descriptor,
    private val context: RegistryContext,
    private val callback: ObservationSink.() -> Unit,
) {
    private val guard: Guard = guard()
    private var states: OrderedMap<SeriesKey, ObservedState> = OrderedMap()

    fun collect(now: Instant): Collected = guard.locked { collectedAt(now) }

    private fun collectedAt(now: Instant): Collected {
        val sink = ObservedSink(descriptor.kind, descriptor.labelKeys.size, context)
        val outcome = contained { callback(sink) }
        sink.isOpen = false
        if (outcome.isFailure) return failed()
        return Collected(listOf(snapshotOf(sink, now)), unreadable = null)
    }

    private fun failed(): Collected {
        val entry = Unreadable(descriptor.name, Unreadability.CallbackFailed)
        context.listener.unreadable(entry)
        return Collected(emptyList(), entry)
    }

    private fun snapshotOf(sink: ObservedSink, now: Instant): FamilySnapshot {
        val next = OrderedMap<SeriesKey, ObservedState>()
        val points = sink.observed.entries.map { (key, value) ->
            val state = stateOf(states[key], value, now)
            next[key] = state
            pointOf(labels = Labels(descriptor.labelKeys, key.values), state = state, value = value, now = now)
        }
        states = next
        return FamilySnapshot(descriptor, Temporality.Cumulative, points.sortedBy(Point::labels))
    }

    private fun stateOf(previous: ObservedState?, value: Double, now: Instant): ObservedState {
        val isReset = previous != null && descriptor.kind == MeterKind.Counter && value < previous.last
        val start = previous?.takeUnless { isReset }?.start ?: now
        return ObservedState(start, value)
    }

    private fun pointOf(labels: Labels, state: ObservedState, value: Double, now: Instant): Point {
        val isGauge = descriptor.kind == MeterKind.Gauge
        if (isGauge) return GaugePoint(labels = labels, start = state.start, value = value, observedAt = now)
        return SumPoint(labels = labels, start = state.start, value = value)
    }
}
