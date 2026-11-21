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

package com.davils.kollect.core.filter

import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.name.LabelKey

/**
 * Decides once, when a family is declared, whether the registry keeps it and which labels it drops.
 *
 * Filters run in the order they were added: the first that accepts or denies decides, a filter that reshapes drops
 * labels and lets the next decide, and a neutral filter lets the next decide. A filter that throws refuses the family,
 * so a broken filter fails closed. Filters see descriptors only, never label values or measurements, and cannot rename
 * a family, so names never collide behind the back of the application.
 *
 * @since 1.0.0
 */
public fun interface MeterFilter {
    /**
     * Decides about the family [descriptor] describes.
     *
     * @param descriptor the family as declared.
     * @return the decision.
     * @since 1.0.0
     */
    public fun decide(descriptor: Descriptor): FilterDecision

    /**
     * Common filters.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Denies every family whose name starts with [prefix].
         *
         * @param prefix the start of the names, such as `jvm.`.
         * @return the filter.
         * @since 1.0.0
         */
        public fun denyPrefix(prefix: String): MeterFilter = MeterFilter { descriptor ->
            val isDenied = descriptor.name.value.startsWith(prefix)
            FilterDecision.Deny.takeIf { isDenied } ?: FilterDecision.Neutral
        }

        /**
         * Drops the labels [keys] from every family that has them, merging the series that differ only in them.
         *
         * @param keys the keys to drop, each following the grammar of a [LabelKey].
         * @return the filter.
         * @throws IllegalArgumentException if a key does not follow the grammar.
         * @since 1.0.0
         */
        public fun dropLabels(vararg keys: String): MeterFilter {
            val dropped = keys.map(LabelKey::of).toSet()
            return MeterFilter { descriptor ->
                val isAffected = descriptor.labelKeys.any(dropped::contains)
                FilterDecision.Reshape(dropped).takeIf { isAffected } ?: FilterDecision.Neutral
            }
        }
    }
}
