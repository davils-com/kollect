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

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal fun AtomicLong.addDouble(amount: Double) {
    var current = load()
    while (!swapped(current, Double.fromBits(current) + amount)) current = load()
}

internal fun AtomicLong.lowerTo(value: Double) {
    var current = load()
    while (value < Double.fromBits(current) && !swapped(current, value)) current = load()
}

internal fun AtomicLong.raiseTo(value: Double) {
    var current = load()
    while (value > Double.fromBits(current) && !swapped(current, value)) current = load()
}

internal fun AtomicLong.loadDouble(): Double = Double.fromBits(load())

internal fun AtomicLong.storeDouble(value: Double) {
    store(value.toRawBits())
}

private fun AtomicLong.swapped(expected: Long, value: Double): Boolean =
    compareAndSet(expectedValue = expected, newValue = value.toRawBits())
