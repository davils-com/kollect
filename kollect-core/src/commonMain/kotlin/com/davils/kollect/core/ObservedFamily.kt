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

import com.davils.kollect.core.meter.Descriptor

/**
 * A declared family whose values a callback reports while a snapshot collects it, such as the size of a pool.
 *
 * The registry holds the callback until [close] is called or the registry is closed; it never relies on garbage
 * collection to forget it. A callback that throws makes the snapshot report the family as unreadable instead of
 * showing values it cannot vouch for. Two snapshots taken at the same time may run the callback at the same time.
 *
 * @since 1.0.0
 */
public class ObservedFamily internal constructor(
    descriptor: Descriptor?,
    internal val onClose: () -> Unit,
) : AutoCloseable {
    /**
     * What describes the family, or null if its declaration was refused.
     *
     * @since 1.0.0
     */
    public val descriptor: Descriptor? = descriptor

    /**
     * Removes the family from its registry and stops calling its callback; closing it again does nothing.
     *
     * @since 1.0.0
     */
    override fun close() {
        onClose()
    }

    /**
     * Shows the family, such as `ObservedFamily(pool.size)`.
     *
     * @return the family as text.
     * @since 1.0.0
     */
    override fun toString(): String = "ObservedFamily(${descriptor?.name ?: "refused"})"
}
