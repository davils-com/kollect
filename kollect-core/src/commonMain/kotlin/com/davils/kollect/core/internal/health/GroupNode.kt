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

package com.davils.kollect.core.internal.health

import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.health.Impact
import com.davils.kollect.core.health.ReasonCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class GroupNode(name: String, impact: Impact, private val members: List<HealthNode>) : HealthNode(
    name,
    impact,
) {
    override suspend fun own(evaluation: Evaluation, path: String): NodeResult {
        val results = coroutineScope { started(evaluation, path).awaitAll() }
        return NodeResult(combined(results.map(NodeResult::health)), results.flatMap(NodeResult::reports))
    }

    private fun CoroutineScope.started(evaluation: Evaluation, path: String): List<Deferred<NodeResult>> =
        members.map { member -> async { member.evaluate(evaluation, path) } }

    companion object {
        fun combined(healths: List<Health>): Health {
            if (healths.isEmpty()) return Health.down(ReasonCode.EmptyGroup)
            val status = healths.maxOf(Health::status)
            val reasons = healths.filter { health -> health.status == status }.flatMap(Health::reasons)
            return Health.of(status, reasons.takeIf { status != HealthStatus.Up }.orEmpty())
        }
    }
}
