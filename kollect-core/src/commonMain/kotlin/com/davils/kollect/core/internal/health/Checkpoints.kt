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

package com.davils.kollect.core.internal.health

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

internal class Checkpoints(private val start: Instant, private val maximum: Int) {
    private val readings: ArrayDeque<Checkpoint> = ArrayDeque()

    fun isFor(start: Instant): Boolean = this.start == start

    fun rate(now: Instant, value: Double, window: Duration): Double? {
        val oldest = readings.lastOrNull { reading -> now - reading.at >= window }
        readings.addLast(Checkpoint(now, value))
        while (readings.size > maximum) readings.removeFirst()
        val earlier = oldest ?: return null
        val seconds = (now - earlier.at).toDouble(DurationUnit.SECONDS)
        return (value - earlier.value) / seconds
    }
}
