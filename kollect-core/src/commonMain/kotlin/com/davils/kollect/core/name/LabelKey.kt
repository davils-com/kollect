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

package com.davils.kollect.core.name

import com.davils.kollect.core.internal.text.NameRules
import kotlin.jvm.JvmInline

/**
 * The key of a label, the name of one dimension of a family, such as `http.request.method`.
 *
 * A key follows the grammar of a [MetricName]: parts separated by `.`, each a lower-case ASCII letter followed by
 * lower-case ASCII letters, digits and single `_`. A family may not declare a key that is reserved by an exposition
 * format: `le`, `quantile` and every key starting with `otel.`, which kollect uses for the overflow series.
 *
 * @property value the key as text.
 * @since 1.0.0
 */
@JvmInline
public value class LabelKey private constructor(public val value: String) : Comparable<LabelKey> {
    /**
     * Tells whether the key is reserved by an exposition format or by kollect itself.
     *
     * @since 1.0.0
     */
    public val isReserved: Boolean
        get() = value in RESERVED || value.startsWith(RESERVED_PREFIX)

    /**
     * Orders keys by their text, character by character.
     *
     * @param other the key to compare with.
     * @return a negative number, zero or a positive number as this key is before, equal to or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: LabelKey): Int = value.compareTo(other.value)

    /**
     * Returns the key as text.
     *
     * @return [value].
     * @since 1.0.0
     */
    override fun toString(): String = value

    /**
     * Creates label keys.
     *
     * @since 1.0.0
     */
    public companion object {
        private val RESERVED: Set<String> = setOf("le", "quantile")
        private const val RESERVED_PREFIX: String = "otel."

        /**
         * The key of the overflow series of a family: `otel.metric.overflow`.
         *
         * @since 1.0.0
         */
        public val OVERFLOW: LabelKey = LabelKey("otel.metric.overflow")

        /**
         * Returns [text] as a label key.
         *
         * @param text the key.
         * @return the key.
         * @throws IllegalArgumentException if [text] does not follow the grammar of a label key; the message shows
         *   [text] escaped.
         * @since 1.0.0
         */
        public fun of(text: String): LabelKey = LabelKey(NameRules.METRIC.requireValid(text = text, kind = "label key"))

        /**
         * Returns [text] as a label key, or null if it does not follow the grammar of a label key.
         *
         * @param text the key.
         * @return the key, or null.
         * @since 1.0.0
         */
        public fun ofOrNull(text: String): LabelKey? = NameRules.METRIC.validOrNull(text)?.let(::LabelKey)
    }
}
