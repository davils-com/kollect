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

package com.davils.kollect.core.internal.otlp

import kotlin.time.Instant

internal class SeriesState(val created: Instant, var lastSeen: Instant) {
    var start: Long = 0L
    var lastTime: Long = 0L
    var value: Double = 0.0
    var count: Long = 0L
    var sum: Double = 0.0
    var buckets: List<Long> = emptyList()

    fun addInto(target: SeriesState) {
        target.value += value
        target.count += count
        target.sum += sum
        target.buckets = added(first = target.buckets, second = buckets)
        target.lastSeen = maxOf(a = target.lastSeen, b = lastSeen)
    }

    private fun added(first: List<Long>, second: List<Long>): List<Long> {
        if (first.isEmpty()) return second
        return first.zip(second) { left, right -> left + right }
    }
}
