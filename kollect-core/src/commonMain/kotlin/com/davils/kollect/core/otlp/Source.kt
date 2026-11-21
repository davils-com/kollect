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

package com.davils.kollect.core.otlp

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val MAXIMUM_LENGTH: Int = 128

/**
 * Who sent an OTLP request, as the application decided after it authenticated the caller, such as a tenant or a
 * client id.
 *
 * kollect never takes a source from the request itself: the application maps its own authentication to a source, so a
 * caller cannot pose as another. Every series of a source carries it in the label `kollect.source`. A source has 1 to
 * 128 characters of ASCII letters, digits, `.`, `_` and `-`.
 *
 * @property id the source as text.
 * @since 1.0.0
 */
@JvmInline
public value class Source private constructor(public val id: String) : Comparable<Source> {
    /**
     * Orders sources by their text.
     *
     * @param other the source to compare with.
     * @return a negative number, zero or a positive number as this source is before, equal to or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: Source): Int = id.compareTo(other.id)

    /**
     * Returns the source as text.
     *
     * @return [id].
     * @since 1.0.0
     */
    override fun toString(): String = id

    /**
     * Creates sources.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns [id] as a source.
         *
         * @param id the source.
         * @return the source.
         * @throws IllegalArgumentException if [id] is empty, longer than 128 characters or holds another character.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        public fun of(id: String): Source {
            require(
                isValid(id),
            ) { "${SafeText.quoted(id)} is no source of 1 to $MAXIMUM_LENGTH letters, digits, '.', '_' or '-'" }
            return Source(id)
        }

        /**
         * Returns [id] as a source, or null if it is invalid.
         *
         * @param id the source.
         * @return the source, or null.
         * @since 1.0.0
         */
        public fun ofOrNull(id: String): Source? = id.takeIf(::isValid)?.let(::Source)

        private fun isValid(id: String): Boolean = id.length in 1..MAXIMUM_LENGTH && id.all(::isAllowed)

        private fun isAllowed(character: Char): Boolean =
            character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character in "._-"
    }
}
