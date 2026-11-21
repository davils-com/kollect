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
import com.davils.kollect.core.health.Impact
import com.davils.kollect.core.health.ReasonCode
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import kotlin.time.Instant

internal class StateNode(
    name: String,
    impact: Impact,
    meter: MetricName,
    undetermined: Undetermined,
    private val downStates: Set<String>,
    private val degradedStates: Set<String>,
) : RuleNode(name, impact, meter, undetermined) {
    override fun judge(family: FamilySnapshot, point: Point, now: Instant): Health {
        val active = (point as? StateSetPoint)?.active
        val state = active?.let(family.descriptor.states::getOrNull) ?: return unknown(ReasonCode.NoData)
        val isDown = state in downStates
        val degraded = Health.degraded(ReasonCode.StateMatched).takeIf { state in degradedStates } ?: Health.Up
        return Health.down(ReasonCode.StateMatched).takeIf { isDown } ?: degraded
    }
}
