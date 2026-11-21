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

package com.davils.kollect.core.internal.exposition

private val LABEL_ESCAPES: Map<Char, String> = mapOf('\\' to "\\\\", '"' to "\\\"", '\n' to "\\n")
private val HELP_ESCAPES: Map<Char, String> = mapOf('\\' to "\\\\", '\n' to "\\n")

internal object Escaping {
    fun labelValue(text: String): String = escaped(text, LABEL_ESCAPES)

    fun help(text: String, escapesQuotes: Boolean): String {
        val escapes = LABEL_ESCAPES.takeIf { escapesQuotes } ?: HELP_ESCAPES
        return escaped(text, escapes)
    }

    private fun escaped(text: String, escapes: Map<Char, String>): String = buildString(text.length) {
        text.forEach { character ->
            val escape = escapes[character]
            if (escape != null) append(escape)
            if (escape == null) append(character)
        }
    }
}
