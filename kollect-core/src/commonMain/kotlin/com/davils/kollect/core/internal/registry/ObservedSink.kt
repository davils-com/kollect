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

import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.meter.Rejection
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

internal class ObservedSink(
    private val kind: MeterKind,
    private val arity: Int,
    private val context: RegistryContext,
) : ObservationSink {
    val observed: OrderedMap<SeriesKey, Double> = OrderedMap()
    var isOpen: Boolean = true

    override fun observe(value: Double, vararg labelValues: String) {
        val rejection = rejectionOf(value, labelValues)
        if (rejection != null) return context.rejections.reject(rejection)
        observed[SeriesKey(labelValues.asList())] = value
    }

    private fun rejectionOf(value: Double, labelValues: Array<out String>): Rejection? {
        if (!isOpen || !areValid(labelValues)) return Rejection.InvalidLabels
        val isTaken = SeriesKey(labelValues.asList()) in observed
        val isFull = observed.size >= context.limits.maximumSeriesPerFamily
        return valueRejectionOf(value) ?: Rejection.Duplicate.takeIf { isTaken || isFull }
    }

    private fun valueRejectionOf(value: Double): Rejection? {
        val isNegative = value < 0.0 && kind == MeterKind.Counter
        return Rejection.NotFinite.takeUnless { value.isFinite() } ?: Rejection.Negative.takeIf { isNegative }
    }

    private fun areValid(labelValues: Array<out String>): Boolean {
        val maximum = context.limits.maximumLabelValueLength
        return labelValues.size == arity && labelValues.all { value -> TextRules.isValidText(value, maximum) }
    }
}
