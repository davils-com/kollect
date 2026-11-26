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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val FIRST: Int = 1
private const val FOURTH: Int = 4
private const val FAR: Int = 1_000

class BackoffTest : FunSpec({
    context("the backoff of an export") {
        test("doubles its ceiling from one second up to the maximum") {
            val backoff = Backoff(maximum = 30.seconds, entropy = { -1L })

            (backoff.delayOf(FIRST) <= 1.seconds) shouldBe true
            (backoff.delayOf(FOURTH) <= 8.seconds) shouldBe true
            (backoff.delayOf(FAR) <= 30.seconds) shouldBe true
        }

        test("spreads the delay by full jitter") {
            Backoff(maximum = 30.seconds, entropy = { 0L }).delayOf(FOURTH) shouldBe Duration.ZERO
            Backoff(maximum = 30.seconds, entropy = { Long.MIN_VALUE }).delayOf(FOURTH) shouldBe 4.seconds
        }

        test("waits the whole ceiling when the entropy fails, instead of retrying at once") {
            val failing = Entropy { error("The entropy is drained") }

            Backoff(maximum = 30.seconds, entropy = failing).delayOf(FOURTH) shouldBe 8.seconds
        }
    }
})
