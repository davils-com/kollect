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

package com.davils.kollect.core.health.rule

import kotlin.time.Duration

/**
 * What a threshold rule reads from each series of its family.
 *
 * @since 1.0.0
 */
public sealed interface Statistic {
    /**
     * The value of a counter, up-down counter or gauge.
     *
     * @since 1.0.0
     */
    public object Value : Statistic {
        /**
         * Returns `Value`.
         *
         * @return the statistic as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Value"
    }

    /**
     * The count of a histogram.
     *
     * @since 1.0.0
     */
    public object Count : Statistic {
        /**
         * Returns `Count`.
         *
         * @return the statistic as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Count"
    }

    /**
     * The mean of a histogram, its sum divided by its count; a histogram without values gives no mean.
     *
     * @since 1.0.0
     */
    public object Mean : Statistic {
        /**
         * Returns `Mean`.
         *
         * @return the statistic as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Mean"
    }

    /**
     * The growth per second of a counter or of the count of a histogram over at least [window], from readings the
     * rule keeps between evaluations; until a reading is at least [window] old the rule cannot decide.
     *
     * @property window the shortest span the rate is measured over.
     * @since 1.0.0
     */
    public class Rate(public val window: Duration) : Statistic {
        init {
            require(window.isPositive() && window.isFinite()) { "The window of a rate is positive and finite" }
        }

        /**
         * Shows the statistic, such as `Rate(1m)`.
         *
         * @return the statistic as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Rate($window)"
    }
}
