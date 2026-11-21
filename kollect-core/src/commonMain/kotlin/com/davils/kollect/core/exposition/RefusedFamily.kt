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

package com.davils.kollect.core.exposition

import com.davils.kollect.core.name.MetricName

private const val HASH_FACTOR: Int = 31

/**
 * A family an exposition left out, and why.
 *
 * @property name the name of the family.
 * @property problem why it was left out.
 * @since 1.0.0
 */
public class RefusedFamily internal constructor(public val name: MetricName, public val problem: FamilyProblem) {
    /**
     * Tells whether [other] names the same family and problem.
     *
     * @param other the object to compare with.
     * @return true if [other] is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is RefusedFamily) return false
        return name == other.name && problem == other.problem
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * name.hashCode() + problem.hashCode()

    /**
     * Shows the entry, such as `RefusedFamily(jobs.total, NameCollision)`.
     *
     * @return the entry as text.
     * @since 1.0.0
     */
    override fun toString(): String = "RefusedFamily($name, $problem)"
}
