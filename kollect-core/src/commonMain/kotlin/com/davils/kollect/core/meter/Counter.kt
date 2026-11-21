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

package com.davils.kollect.core.meter

/**
 * A meter of a sum that only grows, such as the number of requests served.
 *
 * @since 1.0.0
 */
public interface Counter : Meter {
    /**
     * Adds one.
     *
     * @since 1.0.0
     */
    public fun increment()

    /**
     * Adds [amount], which lies between 0 and [MAXIMUM_INCREMENT]; any other amount is rejected and counted.
     *
     * @param amount what to add.
     * @since 1.0.0
     */
    public fun add(amount: Long)

    /**
     * The largest amount one call of [add] takes.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The largest amount one call of [add] takes: 2^32. With it a counter takes at least 2^31 calls before its
         * sum overflows.
         *
         * @since 1.0.0
         */
        public const val MAXIMUM_INCREMENT: Long = 4_294_967_296L
    }
}
