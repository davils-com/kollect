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

package com.davils.kollect.core.snapshot

import com.davils.kollect.core.meter.Descriptor
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

private const val HASH_FACTOR: Int = 31

/**
 * The points of every series of one family at the time of a snapshot.
 *
 * @property descriptor what describes the family.
 * @property temporality whether its sums and histograms are cumulative or deltas.
 * @since 1.0.0
 */
public class FamilySnapshot internal constructor(
    public val descriptor: Descriptor,
    public val temporality: Temporality,
    points: List<Point>,
) {
    /**
     * The points, ordered by their labels.
     *
     * @since 1.0.0
     */
    public val points: List<Point> = points.immutableList()

    /**
     * Tells whether [other] is an equal family snapshot.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same descriptor, temporality and points.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is FamilySnapshot) return false
        return descriptor == other.descriptor && temporality == other.temporality && points == other.points
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int =
        HASH_FACTOR * (HASH_FACTOR * descriptor.hashCode() + temporality.hashCode()) + points.hashCode()

    /**
     * Shows the family, such as `FamilySnapshot(http.server.requests, Cumulative, 3 points)`.
     *
     * @return the family as text.
     * @since 1.0.0
     */
    override fun toString(): String = "FamilySnapshot(${descriptor.name}, $temporality, ${points.size} points)"
}
