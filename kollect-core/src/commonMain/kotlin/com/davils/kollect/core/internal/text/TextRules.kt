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

private const val LAST_C0: Char = '\u001F'
private const val DELETE: Char = '\u007F'
private const val LAST_C1: Char = '\u009F'

internal object TextRules {
    fun isValidText(text: CharSequence, maximumLength: Int): Boolean {
        if (text.length > maximumLength) return false
        return firstInvalidIndex(text) < 0
    }

    fun firstInvalidIndex(text: CharSequence): Int {
        var index = 0
        var invalid = -1
        while (invalid < 0 && index < text.length) {
            val step = validStep(text, index)
            if (step == 0) invalid = index
            index += step.coerceAtLeast(1)
        }
        return invalid
    }

    private fun validStep(text: CharSequence, index: Int): Int {
        val character = text[index]
        if (isControl(character) || character.isLowSurrogate()) return 0
        return stepAfter(text, index, character)
    }

    private fun stepAfter(text: CharSequence, index: Int, character: Char): Int {
        if (!character.isHighSurrogate()) return 1
        val isPaired = text.getOrNull(index + 1)?.isLowSurrogate() == true
        return 2.takeIf { isPaired } ?: 0
    }

    private fun isControl(character: Char): Boolean = character <= LAST_C0 || character in DELETE..LAST_C1
}
