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
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.Point
import kotlin.time.Instant

internal abstract class RuleNode(
    name: String,
    impact: Impact,
    private val meter: MetricName,
    private val undetermined: Undetermined,
) : HealthNode(name, impact) {
    override suspend fun own(evaluation: Evaluation, path: String): NodeResult {
        val snapshot = evaluation.snapshot
        val isUnreadable = snapshot.unreadable.any { entry -> entry.name == meter }
        val family = snapshot.family(meter)?.takeIf { found -> found.points.isNotEmpty() && !isUnreadable }
        val missing = ReasonCode.Unreadable.takeIf { isUnreadable } ?: ReasonCode.NoData
        if (family == null) return NodeResult(unknown(missing), emptyList())
        val health = worst(family, evaluation.now)
        observed(family)
        return NodeResult(health, emptyList())
    }

    protected abstract fun judge(family: FamilySnapshot, point: Point, now: Instant): Health

    protected open fun observed(family: FamilySnapshot) = Unit

    protected fun unknown(reason: ReasonCode): Health {
        val status = HealthStatus.Degraded.takeIf { undetermined == Undetermined.Degraded } ?: HealthStatus.Down
        return Health.of(status, listOf(reason))
    }

    private fun worst(family: FamilySnapshot, now: Instant): Health =
        GroupNode.combined(family.points.map { point -> judge(family, point, now) })
}
