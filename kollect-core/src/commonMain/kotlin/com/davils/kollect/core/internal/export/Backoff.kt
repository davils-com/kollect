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

package com.davils.kollect.core.internal.export

import com.davils.konfetti.core.bus.Entropy
import com.davils.kore.core.failure.contained
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val FRACTION_SHIFT: Int = 11
private const val FRACTION_SCALE: Double = 9_007_199_254_740_992.0
private const val MAXIMUM_DOUBLINGS: Int = 30

internal class Backoff(private val maximum: Duration, private val entropy: Entropy) {
    fun delayOf(attempt: Int): Duration {
        val doublings = (attempt - 1).coerceIn(minimumValue = 0, maximumValue = MAXIMUM_DOUBLINGS)
        val ceiling = minOf(a = 1.seconds * (1L shl doublings).toDouble(), b = maximum)
        return ceiling * fraction()
    }

    private fun fraction(): Double {
        val drawn = contained { entropy.nextLong() }.getOrNull() ?: 0L
        return (drawn ushr FRACTION_SHIFT) / FRACTION_SCALE
    }
}
