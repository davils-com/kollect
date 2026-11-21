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

package com.davils.kollect.core.snapshot

import com.davils.kollect.core.internal.text.Decimals
import com.davils.kollect.core.name.Labels
import kotlin.time.Instant

private const val HASH_FACTOR: Int = 31

/**
 * The last value of a gauge series and when it was observed.
 *
 * @property labels the labels of the series.
 * @property start when the series was created.
 * @property value the value.
 * @property observedAt when the value was set or observed; health rules use it to find stale values.
 * @since 1.0.0
 */
public class GaugePoint internal constructor(
    override val labels: Labels,
    override val start: Instant,
    public val value: Double,
    public val observedAt: Instant,
) : Point {
    /**
     * Tells whether [other] is an equal point.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same labels, start, value and observation time.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is GaugePoint) return false
        return labels == other.labels &&
            start == other.start &&
            value.equals(other.value) &&
            observedAt == other.observedAt
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int =
        listOf(labels, start, value, observedAt).fold(0) { hash, part -> HASH_FACTOR * hash + part.hashCode() }

    /**
     * Shows the point, such as `GaugePoint({}, 21.5, observedAt=…)`.
     *
     * @return the point as text.
     * @since 1.0.0
     */
    override fun toString(): String = "GaugePoint($labels, ${Decimals.text(value)}, observedAt=$observedAt)"
}
