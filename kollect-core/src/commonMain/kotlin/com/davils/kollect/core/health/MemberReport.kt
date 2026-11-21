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

package com.davils.kollect.core.health

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

private const val HASH_FACTOR: Int = 31

/**
 * The health of one member of a health registry: an indicator, a rule or a group.
 *
 * @property path the names from the root to the member, joined with `.`, such as `database.pool`.
 * @property status the status of the member, before its impact on its group.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class MemberReport internal constructor(
    public val path: String,
    public val status: HealthStatus,
    reasons: List<ReasonCode>,
) {
    /**
     * Why the status is not Up.
     *
     * @since 1.0.0
     */
    public val reasons: List<ReasonCode> = reasons.immutableList()

    /**
     * Tells whether [other] is an equal report.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same path, status and reasons.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is MemberReport && path == other.path && status == other.status && reasons == other.reasons

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int =
        HASH_FACTOR * (HASH_FACTOR * path.hashCode() + status.hashCode()) + reasons.hashCode()

    /**
     * Shows the report, such as `database.pool=Down([TimedOut])`.
     *
     * @return the report as text.
     * @since 1.0.0
     */
    override fun toString(): String = "$path=$status($reasons)"
}
