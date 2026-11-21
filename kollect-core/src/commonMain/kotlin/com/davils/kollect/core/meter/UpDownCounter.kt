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
 * A meter of a sum that grows and shrinks, such as the number of requests in flight.
 *
 * @since 1.0.0
 */
public interface UpDownCounter : Meter {
    /**
     * Adds one.
     *
     * @since 1.0.0
     */
    public fun increment()

    /**
     * Subtracts one.
     *
     * @since 1.0.0
     */
    public fun decrement()

    /**
     * Adds [amount], which lies between `-`[Counter.MAXIMUM_INCREMENT] and [Counter.MAXIMUM_INCREMENT]; any other
     * amount is rejected and counted.
     *
     * @param amount what to add, negative to subtract.
     * @since 1.0.0
     */
    public fun add(amount: Long)
}
