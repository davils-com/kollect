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

package com.davils.kollect.core.meter

import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * What describes a family: its name, kind, unit, description, label keys and, by kind, its bucket bounds or states.
 *
 * Two families with equal descriptors are the same family; a declaration of an existing name with a different
 * descriptor conflicts with it. A descriptor is immutable and safe to share.
 *
 * @property name the name of the family.
 * @property kind what the family measures.
 * @property unit the unit of its values.
 * @property description what the family measures, in words; may be empty.
 * @param labelKeys the keys of the labels of its series, in the order the values are given.
 * @property bounds the bucket bounds of a [MeterKind.Histogram], null for every other kind.
 * @param states the states of a [MeterKind.StateSet] in their order, empty for every other kind.
 * @since 1.0.0
 */
public class Descriptor internal constructor(
    public val name: MetricName,
    public val kind: MeterKind,
    public val unit: MeasureUnit,
    public val description: String,
    labelKeys: List<LabelKey>,
    public val bounds: ExplicitBounds? = null,
    states: List<String> = emptyList(),
) {
    /**
     * The keys of the labels of the series, in the order the values are given.
     *
     * @since 1.0.0
     */
    public val labelKeys: List<LabelKey> = labelKeys.immutableList()

    /**
     * The states of a [MeterKind.StateSet] in their order, empty for every other kind.
     *
     * @since 1.0.0
     */
    public val states: List<String> = states.immutableList()

    internal val boundValues: List<Double> get() = bounds?.values.orEmpty()

    /**
     * Tells whether [other] describes the same family.
     *
     * @param other the object to compare with.
     * @return true if every part of [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Descriptor) return false
        return name == other.name &&
            kind == other.kind &&
            unit == other.unit &&
            description == other.description &&
            labelKeys == other.labelKeys &&
            bounds == other.bounds &&
            states == other.states
    }

    /**
     * Returns a hash of every part, consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int {
        val parts = listOf(name, kind, unit, description, labelKeys, bounds, states)
        return parts.fold(0) { hash, part -> HASH_FACTOR * hash + part.hashCode() }
    }

    /**
     * Shows the descriptor, such as `Descriptor(http.server.requests, Counter, {request}, [method])`; the description
     * is shown escaped.
     *
     * @return the descriptor as text.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String =
        "Descriptor($name, $kind, $unit, $labelKeys, description=${SafeText.quoted(description)})"
}
