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

package com.davils.kollect.core.internal.exposition

import com.davils.kollect.core.exposition.ExpositionFormat
import com.davils.kollect.core.exposition.FamilyProblem
import com.davils.kollect.core.exposition.LabelRedaction
import com.davils.kollect.core.internal.text.Decimals
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.PrometheusNaming
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.text.SafeText
import kotlin.time.Instant

private const val MILLISECONDS_PER_SECOND: Double = 1_000.0

internal class FamilyRenderer(format: ExpositionFormat, private val redaction: LabelRedaction) {
    private val isOpenMetrics: Boolean = format == ExpositionFormat.OpenMetrics

    fun render(family: FamilySnapshot): Rendered {
        val descriptor = family.descriptor
        val labelTexts = family.points.map { point -> labelTextOf(point.labels) }
        val problem = problemOf(descriptor, labelTexts)
        if (problem != null) return Rendered(text = "", problem = problem, series = 0)
        val text = buildString {
            header(descriptor)
            family.points.zip(labelTexts.filterNotNull()) { point, labels -> samples(descriptor, point, labels) }
        }
        return Rendered(text = text, problem = null, series = family.points.size)
    }

    private fun problemOf(descriptor: Descriptor, labelTexts: List<String?>): FamilyProblem? {
        val isTextValid = TextRules.firstInvalidIndex(descriptor.description) < 0 && labelTexts.none { it == null }
        if (!isTextValid) return FamilyProblem.InvalidText
        val distinct = OrderedSet(labelTexts.filterNotNull())
        return FamilyProblem.DuplicateSeries.takeIf { distinct.size != labelTexts.size }
    }

    private fun labelTextOf(labels: Labels): String? {
        val pairs = labels.keys.indices.map { index ->
            val key = labels.keys[index]
            val value = SafeText.REDACTED.takeIf { redaction.redacts(key) } ?: labels.values[index]
            if (TextRules.firstInvalidIndex(value) >= 0) return null
            "${PrometheusNaming.labelName(key)}=\"${Escaping.labelValue(value)}\""
        }
        return pairs.joinToString(separator = ",")
    }

    private fun StringBuilder.header(descriptor: Descriptor) {
        val name = headerName(descriptor)
        val help = Escaping.help(descriptor.description, escapesQuotes = isOpenMetrics)
        if (help.isNotEmpty()) append("# HELP $name $help\n")
        append("# TYPE $name ${typeOf(descriptor.kind)}\n")
        val suffix = descriptor.unit.prometheusSuffix
        if (isOpenMetrics && suffix != null) append("# UNIT $name $suffix\n")
    }

    private fun headerName(descriptor: Descriptor): String {
        val family = PrometheusNaming.familyName(descriptor)
        if (isOpenMetrics) return family
        return family + suffixInText(descriptor.kind)
    }

    private fun suffixInText(kind: MeterKind): String = when (kind) {
        MeterKind.Counter -> "_total"
        MeterKind.Info -> "_info"
        MeterKind.UpDownCounter, MeterKind.Gauge, MeterKind.Histogram, MeterKind.StateSet -> ""
    }

    private fun typeOf(kind: MeterKind): String {
        if (!isOpenMetrics) return textTypeOf(kind)
        return when (kind) {
            MeterKind.Counter -> "counter"
            MeterKind.UpDownCounter, MeterKind.Gauge -> "gauge"
            MeterKind.Histogram -> "histogram"
            MeterKind.StateSet -> "stateset"
            MeterKind.Info -> "info"
        }
    }

    private fun textTypeOf(kind: MeterKind): String = when (kind) {
        MeterKind.Counter -> "counter"
        MeterKind.Histogram -> "histogram"
        MeterKind.UpDownCounter, MeterKind.Gauge, MeterKind.StateSet, MeterKind.Info -> "gauge"
    }

    private fun StringBuilder.samples(descriptor: Descriptor, point: Point, labels: String) {
        val family = PrometheusNaming.familyName(descriptor)
        when (point) {
            is SumPoint -> sum(descriptor = descriptor, family = family, point = point, labels = labels)
            is GaugePoint -> sample(name = family, labels = labels, extra = null, value = Decimals.text(point.value))
            is HistogramPoint -> histogram(descriptor = descriptor, family = family, point = point, labels = labels)
            is StateSetPoint -> states(descriptor = descriptor, family = family, point = point, labels = labels)
            is InfoPoint -> sample(name = family + "_info", labels = labels, extra = null, value = "1")
        }
    }

    private fun StringBuilder.sum(descriptor: Descriptor, family: String, point: SumPoint, labels: String) {
        if (descriptor.kind != MeterKind.Counter) {
            return sample(
                name = family,
                labels = labels,
                extra = null,
                value = Decimals.text(point.value),
            )
        }
        sample(name = family + "_total", labels = labels, extra = null, value = Decimals.text(point.value))
        created(family, labels, point.start)
    }

    private fun StringBuilder.histogram(descriptor: Descriptor, family: String, point: HistogramPoint, labels: String) {
        val bounds = descriptor.boundValues
        var cumulative = 0L
        bounds.forEachIndexed { index, bound ->
            cumulative += point.bucketCounts[index]
            sample(
                name = family + "_bucket",
                labels = labels,
                extra = "le=\"${Decimals.boundText(bound)}\"",
                value = cumulative.toString(),
            )
        }
        sample(name = family + "_bucket", labels = labels, extra = "le=\"+Inf\"", value = point.count.toString())
        sample(name = family + "_count", labels = labels, extra = null, value = point.count.toString())
        sample(name = family + "_sum", labels = labels, extra = null, value = Decimals.text(point.sum))
        created(family, labels, point.start)
    }

    private fun StringBuilder.states(descriptor: Descriptor, family: String, point: StateSetPoint, labels: String) {
        descriptor.states.forEachIndexed { index, state ->
            val value = "1".takeIf { index == point.active } ?: "0"
            sample(name = family, labels = labels, extra = "$family=\"${Escaping.labelValue(state)}\"", value = value)
        }
    }

    private fun StringBuilder.created(family: String, labels: String, start: Instant) {
        if (!isOpenMetrics) return
        val seconds = start.toEpochMilliseconds() / MILLISECONDS_PER_SECOND
        sample(name = family + "_created", labels = labels, extra = null, value = Decimals.text(seconds))
    }

    private fun StringBuilder.sample(name: String, labels: String, extra: String?, value: String) {
        val parts = listOfNotNull(labels.takeIf(String::isNotEmpty), extra)
        val all = parts.joinToString(separator = ",")
        val braces = "{$all}".takeIf { all.isNotEmpty() }.orEmpty()
        append("$name$braces $value\n")
    }
}
