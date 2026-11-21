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

package com.davils.kollect.core.internal.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val LARGE: Double = 1.5e21
private const val SMALL: Double = 2.5e-7
private const val PLAIN: Double = 123_456.25
private const val WHOLE_LARGE: Double = 1e21
private const val ZERO: Double = 0.0
private const val ONE_AND_HALF: Double = 1.5
private const val NEGATIVE_LARGE: Double = -2e30

class DecimalsTest : FunSpec({
    test("writes the shortest decimal on every platform") {
        Decimals.text(Double.NaN) shouldBe "NaN"
        Decimals.text(Double.POSITIVE_INFINITY) shouldBe "+Inf"
        Decimals.text(Double.NEGATIVE_INFINITY) shouldBe "-Inf"
        Decimals.text(0.0) shouldBe "0"
        Decimals.text(-ZERO) shouldBe "-0"
        Decimals.text(PLAIN) shouldBe "123456.25"
        Decimals.text(LARGE) shouldBe "1.5e+21"
        Decimals.text(SMALL) shouldBe "2.5e-7"
        Decimals.text(NEGATIVE_LARGE) shouldBe "-2e+30"
    }

    test("writes bounds as floating point numbers") {
        Decimals.boundText(1.0) shouldBe "1.0"
        Decimals.boundText(ONE_AND_HALF) shouldBe "1.5"
        Decimals.boundText(WHOLE_LARGE) shouldBe "1e+21"
        Decimals.boundText(Double.POSITIVE_INFINITY) shouldBe "+Inf"
    }

    test("finds the first invalid character") {
        TextRules.firstInvalidIndex("ab\uD800") shouldBe 2
        TextRules.firstInvalidIndex("a😀b") shouldBe -1
        TextRules.firstInvalidIndex("\uDE00") shouldBe 0
        TextRules.firstInvalidIndex("a\u0085") shouldBe 1
    }
})
