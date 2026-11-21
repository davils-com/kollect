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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.internal.event.HealthEvents
import com.davils.kollect.core.internal.health.Evaluation
import com.davils.kollect.core.internal.health.GroupNode
import com.davils.kollect.core.limit.HealthLimits
import com.davils.kollect.core.snapshot.MetricsSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The health of an application: indicators and rules over its meters, grouped, evaluated together.
 *
 * Health fails closed: an indicator that throws or does not answer in time is Down, a rule that finds no data, a
 * stale gauge, NaN, too few readings or an unreadable family is Down unless it was told to be Degraded, and a group
 * without members is Down. Nothing that cannot be decided is ever Up.
 *
 * An evaluation takes one snapshot of the meter registry for every rule and runs the indicators in parallel on the
 * calling coroutine, at most [HealthLimits.maximumConcurrentChecks] at a time. Evaluations that overlap run one after
 * the other, so rate rules see their readings in order.
 * A health registry is safe to use from many coroutines; it starts no thread.
 *
 * @property limits the limits of the registry.
 * @since 1.0.0
 */
public class HealthRegistry internal constructor(
    private val clock: Clock,
    private val meters: MeterRegistry?,
    public val limits: HealthLimits,
    private val root: GroupNode,
    private val events: HealthEvents?,
) {
    private val mutex: Mutex = Mutex()

    /**
     * Evaluates every member.
     *
     * @return the report.
     * @since 1.0.0
     */
    public suspend fun evaluate(): HealthReport = mutex.withLock { evaluated() }

    /**
     * Shows the registry, such as `HealthRegistry(HealthLimits(…))`.
     *
     * @return the registry as text.
     * @since 1.0.0
     */
    override fun toString(): String = "HealthRegistry($limits)"

    private fun snapshotAt(now: Instant): MetricsSnapshot {
        val registry = meters ?: return MetricsSnapshot(now, emptyList())
        return registry.snapshot()
    }

    private suspend fun evaluated(): HealthReport {
        val now = clock.now()
        val evaluation = Evaluation(now, snapshotAt(now), Semaphore(limits.maximumConcurrentChecks))
        val result = root.evaluate(evaluation, prefix = "")
        val report = HealthReport(result.health.status, now, result.reports.drop(1))
        events?.evaluated(report)
        return report
    }
}
