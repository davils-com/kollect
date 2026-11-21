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

import com.davils.kollect.core.name.LabelKey

/**
 * What a [MeterFilter] decided about a family.
 *
 * @since 1.0.0
 */
public sealed interface FilterDecision {
    /**
     * Keep the family; later filters are not asked.
     *
     * @since 1.0.0
     */
    public object Accept : FilterDecision {
        /**
         * Returns `Accept`.
         *
         * @return the decision as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Accept"
    }

    /**
     * Refuse the family; later filters are not asked.
     *
     * @since 1.0.0
     */
    public object Deny : FilterDecision {
        /**
         * Returns `Deny`.
         *
         * @return the decision as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Deny"
    }

    /**
     * No decision; the next filter decides, and the family is kept if none does.
     *
     * @since 1.0.0
     */
    public object Neutral : FilterDecision {
        /**
         * Returns `Neutral`.
         *
         * @return the decision as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Neutral"
    }

    /**
     * Drop the labels [dropLabels] from the family and let the next filter decide.
     *
     * @property dropLabels the keys to drop; keys the family does not have are ignored.
     * @since 1.0.0
     */
    public class Reshape(public val dropLabels: Set<LabelKey>) : FilterDecision {
        /**
         * Shows the decision, such as `Reshape([path])`.
         *
         * @return the decision as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Reshape($dropLabels)"
    }
}
