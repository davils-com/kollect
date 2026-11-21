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

package com.davils.kollect.core

import com.davils.kollect.core.internal.registry.FamilyCore
import com.davils.kollect.core.meter.Descriptor

/**
 * A declared family: every series of one metric, told apart by their label values.
 *
 * [with] returns the meter of a series and creates the series on first use. A new series beyond
 * [com.davils.kollect.core.limit.RegistryLimits.maximumSeriesPerFamily] or
 * [com.davils.kollect.core.limit.RegistryLimits.maximumSeries] records into the overflow series of the family, so
 * totals stay exact while the number of series stays bounded. Label values that are invalid, too long or of the wrong
 * number are rejected: the returned meter records nothing and the rejection is counted.
 *
 * A family is safe to use from many threads.
 *
 * @since 1.0.0
 */
public class Family<out M> internal constructor(
    descriptor: Descriptor?,
    internal val core: FamilyCore<M>?,
    internal val noop: M,
) {
    /**
     * What describes the family, or null if its declaration was refused.
     *
     * @since 1.0.0
     */
    public val descriptor: Descriptor? = descriptor

    /**
     * The number of series the family holds, its overflow series included.
     *
     * @since 1.0.0
     */
    public val size: Int get() = core?.size ?: 0

    /**
     * The meter of the series without label values, for a family without label keys.
     *
     * @since 1.0.0
     */
    public val meter: M get() = with()

    /**
     * Returns the meter of the series with [labelValues], creating the series on first use.
     *
     * @param labelValues one value for every label key of the family, in their order.
     * @return the meter; it records nothing if the values are invalid or the declaration was refused.
     * @since 1.0.0
     */
    public fun with(vararg labelValues: String): M = core?.with(labelValues) ?: noop

    /**
     * Removes the series with [labelValues], which frees its place for another series.
     *
     * @param labelValues the values of the series.
     * @return true if the family held the series.
     * @since 1.0.0
     */
    public fun remove(vararg labelValues: String): Boolean = core?.remove(labelValues) == true

    /**
     * Shows the family, such as `Family(http.server.requests, 3 series)`.
     *
     * @return the family as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Family(${descriptor?.name ?: "refused"}, $size series)"
}
