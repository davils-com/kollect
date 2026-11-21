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

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.internal.event

import com.davils.kollect.core.event.HealthEvent
import com.davils.kollect.core.event.MetricTopics
import com.davils.kollect.core.health.HealthReport
import com.davils.kollect.core.health.HealthStatus
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

internal class HealthEvents(bus: EventBus) {
    private val outbox: Outbox<HealthEvent> = Outbox(bus, MetricTopics.HEALTH)
    private var previous: OrderedMap<String, HealthStatus> = OrderedMap()
    private var overall: HealthStatus? = null

    fun evaluated(report: HealthReport) {
        val next = OrderedMap<String, HealthStatus>()
        report.members.forEach { member ->
            next[member.path] = member.status
            previous[member.path]?.let { from -> changed(path = member.path, from = from, to = member.status) }
        }
        overall?.let { from -> changed(path = null, from = from, to = report.status) }
        previous = next
        overall = report.status
        outbox.flush()
    }

    private fun changed(path: String?, from: HealthStatus, to: HealthStatus) {
        if (from != to) outbox.add(HealthEvent.StatusChanged(path, from, to))
    }
}
