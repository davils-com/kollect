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
 * The health something reports: a status and the reasons for it.
 *
 * @property status the status.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class Health private constructor(public val status: HealthStatus, reasons: List<ReasonCode>) {
    /**
     * Why the status is not Up; empty for Up.
     *
     * @since 1.0.0
     */
    public val reasons: List<ReasonCode> = reasons.immutableList()

    /**
     * Tells whether [other] is an equal health.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same status and reasons.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is Health && status == other.status && reasons == other.reasons

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * status.hashCode() + reasons.hashCode()

    /**
     * Shows the health, such as `Down([TimedOut])`.
     *
     * @return the health as text.
     * @since 1.0.0
     */
    override fun toString(): String = "$status($reasons)"

    /**
     * Creates healths.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Healthy.
         *
         * @since 1.0.0
         */
        public val Up: Health = Health(HealthStatus.Up, emptyList())

        /**
         * Impaired for [reasons]; [ReasonCode.Reported] if none are given.
         *
         * @param reasons why.
         * @return the health.
         * @since 1.0.0
         */
        public fun degraded(vararg reasons: ReasonCode): Health = of(HealthStatus.Degraded, reasons.asList())

        /**
         * Not working for [reasons]; [ReasonCode.Reported] if none are given.
         *
         * @param reasons why.
         * @return the health.
         * @since 1.0.0
         */
        public fun down(vararg reasons: ReasonCode): Health = of(HealthStatus.Down, reasons.asList())

        internal fun of(status: HealthStatus, reasons: List<ReasonCode>): Health {
            if (status == HealthStatus.Up) return Up
            return Health(status, reasons.ifEmpty { listOf(ReasonCode.Reported) }.distinct())
        }
    }
}
