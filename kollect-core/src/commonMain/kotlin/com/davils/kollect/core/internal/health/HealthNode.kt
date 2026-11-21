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
import com.davils.kollect.core.health.MemberReport

internal abstract class HealthNode(val name: String, val impact: Impact) {
    suspend fun evaluate(evaluation: Evaluation, prefix: String): NodeResult {
        val parts = listOf(prefix, name).filter(String::isNotEmpty)
        val path = parts.joinToString(separator = ".")
        val result = own(evaluation, path)
        val report = MemberReport(path, result.health.status, result.health.reasons)
        return NodeResult(capped(result.health), listOf(report) + result.reports)
    }

    protected abstract suspend fun own(evaluation: Evaluation, path: String): NodeResult

    private fun capped(health: Health): Health {
        val isCapped = impact == Impact.Degrading && health.status == HealthStatus.Down
        if (!isCapped) return health
        return Health.of(HealthStatus.Degraded, health.reasons)
    }
}
