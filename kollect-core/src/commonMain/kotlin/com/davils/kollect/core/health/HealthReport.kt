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
import kotlin.time.Instant

/**
 * The result of evaluating a health registry: the overall status and the status of every member.
 *
 * @property status the overall status, the worst status of the top-level members after their impact; Down if the
 *   registry has no member.
 * @property evaluatedAt when the evaluation started.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class HealthReport internal constructor(
    public val status: HealthStatus,
    public val evaluatedAt: Instant,
    members: List<MemberReport>,
) {
    /**
     * Every member, groups before their members, in the order they were declared.
     *
     * @since 1.0.0
     */
    public val members: List<MemberReport> = members.immutableList()

    /**
     * Returns the report of the member at [path], or null if there is none.
     *
     * @param path the path, such as `database.pool`.
     * @return the report, or null.
     * @since 1.0.0
     */
    public fun member(path: String): MemberReport? = members.firstOrNull { member -> member.path == path }

    /**
     * Shows the report, such as `HealthReport(Degraded, 4 members)`.
     *
     * @return the report as text.
     * @since 1.0.0
     */
    override fun toString(): String = "HealthReport($status, ${members.size} members)"
}
