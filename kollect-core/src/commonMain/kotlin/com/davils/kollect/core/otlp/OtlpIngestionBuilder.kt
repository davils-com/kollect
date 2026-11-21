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

package com.davils.kollect.core.otlp

import com.davils.kollect.core.internal.otlp.FamilyRule
import com.davils.kollect.core.internal.otlp.PointChecks
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import com.davils.kore.core.annotation.DavilsDsl

private val INGESTED_KINDS: Set<MeterKind> = setOf(
    MeterKind.Counter,
    MeterKind.UpDownCounter,
    MeterKind.Gauge,
    MeterKind.Histogram,
)

/**
 * Configures an [OtlpIngestion] while [otlpIngestion] builds it: its limits and the families it accepts.
 *
 * An ingestion accepts no family unless one is declared with [acceptFamily] or opened with [acceptAnyFamily], so a
 * client cannot create families on a server it was not meant to.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OtlpIngestionBuilder internal constructor() {
    /**
     * The limits of the ingestion; [IngestLimits.Default] by default.
     *
     * @since 1.0.0
     */
    public var limits: IngestLimits = IngestLimits.Default

    internal val rules: MutableList<FamilyRule> = mutableListOf()

    internal var anyPrefix: String? = null
        private set

    /**
     * Accepts the family [name] with [kind], [unit] and exactly the attributes [labelKeys].
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param kind a counter, up-down counter, gauge or histogram.
     * @param unit the unit the points must carry.
     * @param labelKeys the attribute keys every point must carry, following the grammar of [LabelKey].
     * @throws IllegalArgumentException if the name, the kind or a key is invalid or reserved, or the family is
     *   declared twice.
     * @since 1.0.0
     */
    public fun acceptFamily(
        name: String,
        kind: MeterKind,
        unit: MeasureUnit = MeasureUnit.NONE,
        vararg labelKeys: String,
    ) {
        val metric = MetricName.of(name)
        require(kind in INGESTED_KINDS) { "An ingestion takes counters, up-down counters, gauges and histograms" }
        require(rules.none { rule -> rule.name == metric }) { "The family $metric is declared twice" }
        val keys = labelKeys.map(LabelKey::of)
        require(
            keys.none { key -> key.isReserved || key == PointChecks.SOURCE_KEY },
        ) { "A label key of $metric is reserved" }
        require(keys.distinct().size == keys.size) { "A label key of $metric repeats" }
        rules.add(FamilyRule(name = metric, kind = kind, unit = unit, keys = keys.sorted()))
    }

    /**
     * Accepts every family whose name starts with [prefix], with the attributes of its first point; for generic OTLP
     * clients. Every limit still applies.
     *
     * @param prefix the start of the names, at least one character, such as `client.`.
     * @throws IllegalArgumentException if [prefix] is empty.
     * @since 1.0.0
     */
    public fun acceptAnyFamily(prefix: String) {
        require(prefix.isNotEmpty()) { "The prefix of the families an ingestion accepts is not empty" }
        anyPrefix = prefix
    }
}
