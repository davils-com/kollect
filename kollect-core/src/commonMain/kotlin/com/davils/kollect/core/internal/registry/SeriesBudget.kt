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

package com.davils.kollect.core.internal.registry

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class SeriesBudget(private val limit: Int) {
    private val used: AtomicInt = AtomicInt(0)

    val count: Int get() = used.load()

    fun tryAcquire(): Boolean {
        var current = used.load()
        while (current < limit && !taken(current)) current = used.load()
        return current < limit
    }

    private fun taken(current: Int): Boolean = used.compareAndSet(expectedValue = current, newValue = current + 1)

    fun release(count: Int) {
        used.addAndFetch(-count)
    }
}
