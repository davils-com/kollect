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

package com.davils.kollect.core.internal.otlp

private const val HASH_FACTOR: Int = 31

internal class SeriesId(val family: String, val instance: String, val values: List<String>) : Comparable<SeriesId> {
    val retiredId: SeriesId get() = SeriesId(family = family, instance = "", values = values)

    override fun compareTo(other: SeriesId): Int {
        val byFamily = family.compareTo(other.family)
        if (byFamily != 0) return byFamily
        return compareTexts(first = listOf(instance) + values, second = listOf(other.instance) + other.values)
    }

    override fun equals(other: Any?): Boolean =
        other is SeriesId && family == other.family && instance == other.instance && values == other.values

    override fun hashCode(): Int =
        HASH_FACTOR * (HASH_FACTOR * family.hashCode() + instance.hashCode()) + values.hashCode()

    override fun toString(): String = "SeriesId($family, ${values.size} values)"
}

private fun compareTexts(first: List<String>, second: List<String>): Int {
    val common = minOf(a = first.size, b = second.size)
    val indices = (0 until common).asSequence()
    val comparisons = indices.map { index -> first[index].compareTo(second[index]) }
    val difference = comparisons.firstOrNull { comparison -> comparison != 0 }
    return difference ?: first.size.compareTo(second.size)
}
