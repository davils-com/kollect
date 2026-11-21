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

package com.davils.kollect.core.name

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * The labels of one series: a value for every label key of its family, in the order of the keys.
 *
 * Labels order first by their values and then by their keys, character by character, so the series of a snapshot
 * come in the same order on every platform. Labels are immutable and safe to share.
 *
 * @since 1.0.0
 */
public class Labels internal constructor(keys: List<LabelKey>, values: List<String>) : Comparable<Labels> {
    /**
     * The keys, in their order.
     *
     * @since 1.0.0
     */
    public val keys: List<LabelKey> = keys.immutableList()

    /**
     * The values, one for each key in the order of [keys].
     *
     * @since 1.0.0
     */
    public val values: List<String> = values.immutableList()

    init {
        require(keys.size == values.size) { "Labels have one value for every key" }
    }

    /**
     * The number of labels.
     *
     * @since 1.0.0
     */
    public val size: Int get() = keys.size

    /**
     * Returns the value of [key], or null if there is no label with that key.
     *
     * @param key the key.
     * @return the value, or null.
     * @since 1.0.0
     */
    public operator fun get(key: LabelKey): String? {
        val index = keys.indexOf(key)
        return values.getOrNull(index)
    }

    /**
     * Orders labels by their values and then by their keys.
     *
     * @param other the labels to compare with.
     * @return a negative number, zero or a positive number as these labels are before, equal to or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: Labels): Int {
        val byValues = compareLists(first = values, second = other.values)
        if (byValues != 0) return byValues
        return compareLists(first = keys.map(LabelKey::value), second = other.keys.map(LabelKey::value))
    }

    /**
     * Tells whether [other] are labels with the same keys and values.
     *
     * @param other the object to compare with.
     * @return true if [other] are equal labels.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Labels) return false
        return keys == other.keys && values == other.values
    }

    /**
     * Returns a hash of the keys and values.
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * keys.hashCode() + values.hashCode()

    /**
     * Shows the labels, such as `{method="GET"}`, with every value escaped.
     *
     * @return the labels as text.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = keys.indices.joinToString(prefix = "{", postfix = "}") { index ->
        "${keys[index]}=${SafeText.quoted(values[index])}"
    }

    /**
     * The labels of a series without labels.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * No labels.
         *
         * @since 1.0.0
         */
        public val EMPTY: Labels = Labels(emptyList(), emptyList())

        internal val OVERFLOW: Labels = Labels(listOf(LabelKey.OVERFLOW), listOf("true"))

        private fun compareLists(first: List<String>, second: List<String>): Int {
            val common = minOf(a = first.size, b = second.size)
            val indices = (0 until common).asSequence()
            val comparisons = indices.map { index -> first[index].compareTo(second[index]) }
            val difference = comparisons.firstOrNull { comparison -> comparison != 0 }
            return difference ?: first.size.compareTo(second.size)
        }
    }
}
