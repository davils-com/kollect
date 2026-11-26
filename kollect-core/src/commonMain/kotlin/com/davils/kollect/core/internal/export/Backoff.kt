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

package com.davils.kollect.core.internal.export

import com.davils.konfetti.core.bus.Entropy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.backoff.fullJitter
import com.davils.kore.core.backoff.grownDelay
import com.davils.kore.core.failure.contained
import com.davils.kore.core.random.nextFraction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MAXIMUM_DOUBLINGS: Int = 30
private const val DOUBLING: Double = 2.0

internal class Backoff(private val maximum: Duration, private val entropy: Entropy) {
    fun delayOf(attempt: Int): Duration {
        val doublings = (attempt - 1).coerceIn(minimumValue = 0, maximumValue = MAXIMUM_DOUBLINGS)
        val ceiling = grownDelay(initial = 1.seconds, multiplier = DOUBLING, step = doublings, maximum = maximum)
        val fraction = contained { entropy.nextFraction() }.getOrDefault(1.0)
        return fullJitter(delay = ceiling, fraction = fraction)
    }
}
