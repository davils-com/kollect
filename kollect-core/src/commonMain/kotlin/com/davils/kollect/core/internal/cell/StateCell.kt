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

import com.davils.kollect.core.meter.StateSet
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Instant

internal class StateCell<E : Enum<E>>(initial: Int) : StateSet<E>, Cell {
    private val active: AtomicInt = AtomicInt(initial)

    override fun set(state: E) {
        active.store(state.ordinal)
    }

    override fun read(labels: Labels, start: Instant, context: ReadContext): Point =
        StateSetPoint(labels = labels, start = start, active = active.load())
}
