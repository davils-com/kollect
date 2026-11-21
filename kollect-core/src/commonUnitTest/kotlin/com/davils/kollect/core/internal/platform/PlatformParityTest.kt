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

package com.davils.kollect.core.internal.platform

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val AMOUNT: Long = 5L

class PlatformParityTest : FunSpec({
    test("a striped counter sums what it is given") {
        val counter = stripedCounter()
        counter.add(AMOUNT)
        counter.add(-1L)
        counter.sum shouldBe AMOUNT - 1L
    }

    test("a guard runs its block and returns its result") {
        guard().locked { AMOUNT } shouldBe AMOUNT
        spinPause()
    }
})
