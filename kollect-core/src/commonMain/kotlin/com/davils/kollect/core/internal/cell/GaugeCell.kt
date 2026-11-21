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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kollect.core.internal.cell

import com.davils.kollect.core.meter.Gauge
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.Point
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.Instant

private const val UNSET: Long = Long.MIN_VALUE

internal class GaugeCell(private val clock: Clock, private val rejections: Rejections) : Gauge, Cell {
    private val bits: AtomicLong = AtomicLong(0L)
    private val observedAt: AtomicLong = AtomicLong(UNSET)

    override fun set(value: Double) {
        if (!value.isFinite()) return rejections.reject(Rejection.NotFinite)
        bits.storeDouble(value)
        observedAt.store(clock.now().toEpochMilliseconds())
    }

    override fun read(labels: Labels, start: Instant, context: ReadContext): Point? {
        val observed = observedAt.load()
        if (observed == UNSET) return null
        val value = bits.loadDouble()
        return GaugePoint(
            labels = labels,
            start = start,
            value = value,
            observedAt = Instant.fromEpochMilliseconds(observed),
        )
    }
}
