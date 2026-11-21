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

import com.davils.kollect.core.name.MetricName
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import kotlin.time.Instant

private const val HASH_FACTOR: Int = 31

/**
 * What a registry or an ingestion holds at one instant: every family it could read, ordered by name, and the families
 * it could not read.
 *
 * A snapshot is immutable and safe to share; exposition, OTLP export and health rules all read the same model.
 *
 * @property timestamp when the snapshot was taken.
 * @since 1.0.0
 */
public class MetricsSnapshot internal constructor(
    public val timestamp: Instant,
    families: List<FamilySnapshot>,
    unreadable: List<Unreadable> = emptyList(),
) {
    /**
     * The families, ordered by name.
     *
     * @since 1.0.0
     */
    public val families: List<FamilySnapshot> = families.immutableList()

    /**
     * The families the snapshot could not read, ordered by name.
     *
     * @since 1.0.0
     */
    public val unreadable: List<Unreadable> = unreadable.immutableList()

    /**
     * Returns the family named [name], or null if the snapshot holds none.
     *
     * @param name the name.
     * @return the family, or null.
     * @since 1.0.0
     */
    public fun family(name: MetricName): FamilySnapshot? = families.firstOrNull { family ->
        family.descriptor.name ==
            name
    }

    /**
     * Tells whether [other] is an equal snapshot.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same timestamp, families and unreadable families.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is MetricsSnapshot) return false
        return timestamp == other.timestamp && families == other.families && unreadable == other.unreadable
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int =
        HASH_FACTOR * (HASH_FACTOR * timestamp.hashCode() + families.hashCode()) + unreadable.hashCode()

    /**
     * Shows the snapshot, such as `MetricsSnapshot(…, 12 families, 0 unreadable)`.
     *
     * @return the snapshot as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "MetricsSnapshot($timestamp, ${families.size} families, ${unreadable.size} unreadable)"
}
