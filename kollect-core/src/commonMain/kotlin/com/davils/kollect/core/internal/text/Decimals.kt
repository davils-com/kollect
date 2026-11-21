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

import com.davils.kore.core.ShortestDecimal
import com.davils.kore.core.annotation.DavilsInternal

private const val PLAIN_FROM: Int = -6
private const val PLAIN_UNTIL: Int = 20

@OptIn(DavilsInternal::class)
internal object Decimals {
    fun text(value: Double): String {
        if (value.isFinite()) return finiteText(value)
        val infinite = "+Inf".takeIf { value > 0.0 } ?: "-Inf"
        return "NaN".takeIf { value.isNaN() } ?: infinite
    }

    fun boundText(value: Double): String {
        val shown = text(value)
        if (!value.isFinite() || hasPointOrExponent(shown)) return shown
        return "$shown.0"
    }

    private fun finiteText(value: Double): String {
        val decimal = ShortestDecimal.of(value)
        if (decimal.exponent in PLAIN_FROM..PLAIN_UNTIL) return decimal.toPlainString()
        return scientificOf(decimal)
    }

    private fun scientificOf(decimal: ShortestDecimal): String {
        val sign = "-".takeIf { decimal.isNegative }.orEmpty()
        val digits = decimal.digits
        val fraction = digits.drop(1)
        val point = ".".takeIf { fraction.isNotEmpty() }.orEmpty()
        val exponentSign = "+".takeIf { decimal.exponent >= 0 }.orEmpty()
        return "$sign${digits.first()}$point${fraction}e$exponentSign${decimal.exponent}"
    }

    private fun hasPointOrExponent(text: String): Boolean = text.any { character ->
        character == '.' || character == 'e'
    }
}
