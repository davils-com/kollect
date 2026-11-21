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

import com.davils.kollect.core.Payload
import com.davils.kollect.core.exposition.ExpositionFormat
import com.davils.kollect.core.exposition.ExpositionReport
import com.davils.kollect.core.exposition.ExpositionResult
import com.davils.kollect.core.exposition.FamilyProblem
import com.davils.kollect.core.exposition.LabelRedaction
import com.davils.kollect.core.exposition.RefusedFamily
import com.davils.kollect.core.limit.ExpositionLimits
import com.davils.kollect.core.name.PrometheusNaming
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal class ExpositionWriter(
    private val format: ExpositionFormat,
    private val limits: ExpositionLimits,
    redaction: LabelRedaction,
) {
    private val renderer: FamilyRenderer = FamilyRenderer(format, redaction)
    private val bytes: BoundedBytes = BoundedBytes(limits.maximumBytes)
    private val claims: OrderedSet<String> = OrderedSet()
    private val refused: MutableList<RefusedFamily> = mutableListOf()
    private var families: Int = 0
    private var series: Int = 0
    private var exceeded: String? = null

    fun write(snapshots: List<MetricsSnapshot>): ExpositionResult {
        val all = snapshots.flatMap(MetricsSnapshot::families)
        val pending = all.asSequence().takeWhile { exceeded == null }
        pending.forEach(::writeFamily)
        if (exceeded == null && format == ExpositionFormat.OpenMetrics) ended()
        val limit = exceeded
        if (limit != null) return ExpositionResult.Refused(limit)
        val report = ExpositionReport(families = families, series = series, refused = refused)
        return ExpositionResult.Written(Payload(bytes.toByteArray(), format.contentType), report)
    }

    private fun writeFamily(family: FamilySnapshot) {
        val descriptor = family.descriptor
        val names = listOf(PrometheusNaming.familyName(descriptor)) + PrometheusNaming.sampleNames(descriptor)
        if (names.any(claims::contains)) return refuse(family, FamilyProblem.NameCollision)
        val rendered = renderer.render(family)
        val problem = rendered.problem
        if (problem != null) return refuse(family, problem)
        accept(rendered, names)
    }

    private fun accept(rendered: Rendered, names: List<String>) {
        series += rendered.series
        if (series > limits.maximumSeries) return exceed("maximumSeries")
        if (!bytes.append(rendered.text)) return exceed("maximumBytes")
        names.forEach(claims::add)
        families += 1
    }

    private fun ended() {
        if (!bytes.append("# EOF\n")) exceed("maximumBytes")
    }

    private fun refuse(family: FamilySnapshot, problem: FamilyProblem) {
        refused.add(RefusedFamily(family.descriptor.name, problem))
    }

    private fun exceed(limit: String) {
        exceeded = limit
    }
}
