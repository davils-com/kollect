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

package com.davils.kollect.core.internal.registry

internal class SeriesKey(val values: List<String>) : Comparable<SeriesKey> {
    override fun compareTo(other: SeriesKey): Int {
        val common = minOf(a = values.size, b = other.values.size)
        val indices = (0 until common).asSequence()
        val comparisons = indices.map { index -> values[index].compareTo(other.values[index]) }
        val difference = comparisons.firstOrNull { comparison -> comparison != 0 }
        return difference ?: values.size.compareTo(other.values.size)
    }

    override fun equals(other: Any?): Boolean = other is SeriesKey && values == other.values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "SeriesKey(${values.size})"
}
