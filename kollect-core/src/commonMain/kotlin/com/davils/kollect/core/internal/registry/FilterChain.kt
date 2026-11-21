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

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.filter.FilterDecision
import com.davils.kollect.core.filter.MeterFilter
import com.davils.kollect.core.meter.Descriptor
import com.davils.kore.core.failure.contained
import com.davils.kore.core.outcome.Outcome

internal class FilterChain(private val filters: List<MeterFilter>) {
    fun apply(checked: Checked): Outcome<Checked, Refusal> {
        var current = checked
        var decision: FilterDecision? = FilterDecision.Neutral
        val pending = filters.iterator()
        while (isUndecided(decision) && pending.hasNext()) {
            val filter = pending.next()
            decision = contained { filter.decide(checked.descriptor) }.getOrNull()
            if (decision is FilterDecision.Reshape) current = reshaped(current, decision)
        }
        return outcomeOf(decision, current)
    }

    private fun isUndecided(decision: FilterDecision?): Boolean =
        decision is FilterDecision.Neutral || decision is FilterDecision.Reshape

    private fun outcomeOf(decision: FilterDecision?, current: Checked): Outcome<Checked, Refusal> = when (decision) {
        null -> Outcome.Failure(Refusal.FilterFailed)
        is FilterDecision.Deny -> Outcome.Failure(Refusal.Denied)
        is FilterDecision.Accept, is FilterDecision.Neutral, is FilterDecision.Reshape -> Outcome.Success(current)
    }

    private fun reshaped(checked: Checked, decision: FilterDecision.Reshape): Checked {
        val descriptor = checked.descriptor
        val kept = checked.kept.filter { index -> checked.declaredKeys[index] !in decision.dropLabels }
        val reshaped = Descriptor(
            name = descriptor.name,
            kind = descriptor.kind,
            unit = descriptor.unit,
            description = descriptor.description,
            labelKeys = kept.map(checked.declaredKeys::get),
            bounds = descriptor.bounds,
            states = descriptor.states,
        )
        return checked.copy(descriptor = reshaped, kept = kept)
    }
}
