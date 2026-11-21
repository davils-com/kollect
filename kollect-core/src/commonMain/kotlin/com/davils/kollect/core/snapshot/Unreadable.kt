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

import com.davils.kollect.core.name.MetricName

private const val HASH_FACTOR: Int = 31

/**
 * A family a snapshot could not read, and why. The snapshot leaves the family out instead of showing values it cannot
 * vouch for.
 *
 * @property name the name of the family.
 * @property reason why it could not be read.
 * @since 1.0.0
 */
public class Unreadable internal constructor(public val name: MetricName, public val reason: Unreadability) {
    /**
     * Tells whether [other] is an equal entry.
     *
     * @param other the object to compare with.
     * @return true if [other] names the same family and reason.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Unreadable) return false
        return name == other.name && reason == other.reason
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * name.hashCode() + reason.hashCode()

    /**
     * Shows the entry, such as `Unreadable(jobs.duration, Stalled)`.
     *
     * @return the entry as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Unreadable($name, $reason)"
}
