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

package com.davils.kollect.core.event

import com.davils.kollect.core.health.HealthStatus
import com.davils.konfetti.core.Event

/**
 * What a health registry publishes on [MetricTopics.HEALTH].
 *
 * @since 1.0.0
 */
public sealed interface HealthEvent : Event {
    /**
     * The status of a member or of the whole registry changed between two evaluations.
     *
     * @property path the path of the member, or null for the whole registry.
     * @property from the status before.
     * @property to the status now.
     * @since 1.0.0
     */
    public class StatusChanged internal constructor(
        public val path: String?,
        public val from: HealthStatus,
        public val to: HealthStatus,
    ) : HealthEvent {
        /**
         * Shows the event, such as `StatusChanged(database.pool, Up -> Down)`.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "StatusChanged(${path ?: "registry"}, $from -> $to)"
    }
}
