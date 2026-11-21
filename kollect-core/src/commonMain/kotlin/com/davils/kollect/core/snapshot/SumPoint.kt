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
 * The sum of a counter or an up-down counter series since [start].
 *
 * @property labels the labels of the series.
 * @property start when the sum started.
 * @property value the sum.
 * @since 1.0.0
 */
public class SumPoint internal constructor(
    override val labels: Labels,
    override val start: Instant,
    public val value: Double,
) : Point {
    /**
     * Tells whether [other] is an equal point.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same labels, start and value.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is SumPoint) return false
        return labels == other.labels && start == other.start && value.equals(other.value)
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * (HASH_FACTOR * labels.hashCode() + start.hashCode()) + value.hashCode()

    /**
     * Shows the point, such as `SumPoint({method="GET"}, 42, start=…)`.
     *
     * @return the point as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SumPoint($labels, ${Decimals.text(value)}, start=$start)"
}
