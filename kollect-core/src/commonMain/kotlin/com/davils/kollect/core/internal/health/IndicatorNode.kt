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
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.Impact
import com.davils.kollect.core.health.ReasonCode
import com.davils.kore.core.failure.attempted
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class IndicatorNode(
    name: String,
    impact: Impact,
    private val timeout: Duration,
    private val indicator: HealthIndicator,
) : HealthNode(name, impact) {
    override suspend fun own(evaluation: Evaluation, path: String): NodeResult {
        val outcome = evaluation.checks.withPermit { withTimeoutOrNull(timeout) { attempted { indicator.check() } } }
        val health = outcome?.getOrElse { Health.down(ReasonCode.CheckFailed) } ?: Health.down(ReasonCode.TimedOut)
        return NodeResult(health, emptyList())
    }
}
