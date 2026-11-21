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

private const val HEX: String = "0123456789abcdef"
private const val LAST_CONTROL: Char = '\u001F'
private const val NIBBLE: Int = 4
private const val NIBBLE_MASK: Int = 0xF
private val ESCAPES: Map<Char, String> = mapOf(
    '"' to "\\\"",
    '\\' to "\\\\",
    '\n' to "\\n",
    '\r' to "\\r",
    '\t' to "\\t",
)

internal object Json {
    fun string(text: String): String = buildString(text.length + 2) {
        append('"')
        text.forEach { character -> append(escapeOf(character)) }
        append('"')
    }

    private fun escapeOf(character: Char): String {
        val known = ESCAPES[character]
        if (known != null) return known
        return unicodeOf(character).takeIf { character <= LAST_CONTROL } ?: character.toString()
    }

    private fun unicodeOf(character: Char): String {
        val code = character.code
        val digits = (3 downTo 0).map { shift -> HEX[code shr shift * NIBBLE and NIBBLE_MASK] }
        return "\\u" + digits.joinToString(separator = "")
    }
}
