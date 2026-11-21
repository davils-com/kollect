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

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.limit.RegistryLimits
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.name.PrometheusNaming
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.SafeText

private const val ACTIVE_SUFFIX: String = ".active"

internal class Checker(private val limits: RegistryLimits) {
    fun check(request: Request): Outcome<Checked, Refusal> {
        val problem = textProblemOf(request) ?: shapeProblemOf(request, descriptorOf(request))
        if (problem != null) return Outcome.Failure(Refusal.Invalid(problem))
        val descriptor = descriptorOf(request)
        return Outcome.Success(
            Checked(
                descriptor = descriptor,
                arity = request.keys.size,
                kept = request.keys.indices.toList(),
                claims = claimsOf(request, descriptor),
            ),
        )
    }

    fun activeDescriptorOf(descriptor: Descriptor): Descriptor = Descriptor(
        name = MetricName.of(descriptor.name.value + ACTIVE_SUFFIX),
        kind = MeterKind.Gauge,
        unit = MeasureUnit.annotation("task"),
        description = "The tasks that run.",
        labelKeys = descriptor.labelKeys,
    )

    private fun descriptorOf(request: Request): Descriptor = Descriptor(
        name = MetricName.of(request.name),
        kind = request.kind,
        unit = request.unit,
        description = request.description,
        labelKeys = request.keys.map(LabelKey::of),
        bounds = request.bounds,
        states = request.states,
    )

    private fun claimsOf(request: Request, descriptor: Descriptor): List<String> {
        val own = listOf(PrometheusNaming.familyName(descriptor)) + PrometheusNaming.sampleNames(descriptor)
        if (request.variant != Variant.LongTask) return own.distinct()
        val active = activeDescriptorOf(descriptor)
        return (own + PrometheusNaming.familyName(active) + PrometheusNaming.sampleNames(active)).distinct()
    }

    private fun textProblemOf(request: Request): String? = listOfNotNull(
        nameProblemOf(request),
        request.keys.firstNotNullOfOrNull(::keyProblemOf),
        "it has more than ${limits.maximumLabels} label keys".takeIf { request.keys.size > limits.maximumLabels },
        descriptionProblemOf(request.description),
        statesProblemOf(request.states),
    ).firstOrNull()

    private fun nameProblemOf(request: Request): String? {
        val isValid = MetricName.ofOrNull(request.name) != null
        val suffix = ACTIVE_SUFFIX.takeIf { request.variant == Variant.LongTask }.orEmpty()
        val isShort = request.name.length + suffix.length <= limits.maximumNameLength
        val problem = "the name ${SafeText.quoted(request.name)} is not a metric name of at most " +
            "${limits.maximumNameLength - suffix.length} characters"
        return problem.takeUnless { isValid && isShort }
    }

    private fun keyProblemOf(text: String): String? {
        val key = LabelKey.ofOrNull(text)
        val isValid = key != null && !key.isReserved && text.length <= limits.maximumNameLength
        return "the label key ${SafeText.quoted(text)} is invalid or reserved".takeUnless { isValid }
    }

    private fun descriptionProblemOf(description: String): String? {
        val isValid = TextRules.isValidText(description, limits.maximumDescriptionLength)
        return "its description is too long or holds a control character".takeUnless { isValid }
    }

    private fun statesProblemOf(states: List<String>): String? =
        "it has more than ${limits.maximumStates} states".takeIf {
            states.size > limits.maximumStates
        }

    private fun shapeProblemOf(request: Request, descriptor: Descriptor): String? = listOfNotNull(
        "its label keys repeat after the mapping to Prometheus".takeIf { hasRepeatedKeys(descriptor.labelKeys) },
        "it has more than ${limits.maximumBuckets} bounds".takeIf { bucketCountOf(request) > limits.maximumBuckets },
        "a timer measures seconds".takeIf { isTimed(request) && request.unit != MeasureUnit.SECONDS },
        "its Prometheus name ends with a reserved suffix".takeIf { PrometheusNaming.isReserved(descriptor) },
        "a label key equals the state label of the state set".takeIf { hasStateLabelClash(descriptor) },
    ).firstOrNull()

    private fun hasRepeatedKeys(keys: List<LabelKey>): Boolean {
        val names = keys.map(PrometheusNaming::labelName)
        return names.distinct().size != names.size
    }

    private fun bucketCountOf(request: Request): Int = request.bounds?.run { values.size } ?: 0

    private fun isTimed(request: Request): Boolean =
        request.variant == Variant.Timer || request.variant == Variant.LongTask

    private fun hasStateLabelClash(descriptor: Descriptor): Boolean {
        if (descriptor.kind != MeterKind.StateSet) return false
        val stateLabel = PrometheusNaming.familyName(descriptor)
        return descriptor.labelKeys.any { key -> PrometheusNaming.labelName(key) == stateLabel }
    }
}
