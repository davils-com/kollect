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

package com.davils.kollect.core.name

import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeterKind

private const val SEPARATOR: Char = '_'
private const val TOTAL: String = "_total"
private const val CREATED: String = "_created"
private const val BUCKET: String = "_bucket"
private const val COUNT: String = "_count"
private const val SUM: String = "_sum"
private const val INFO: String = "_info"

/**
 * How kollect maps names to the names Prometheus and OpenMetrics see, following the compatibility rules of
 * OpenTelemetry.
 *
 * A metric name and a label key become a Prometheus name by replacing every `.` with `_`, so `http.server.duration`
 * becomes `http_server_duration`. The family name gets the suffix of its unit unless it already ends with it, so a
 * family in seconds becomes `http_server_duration_seconds`. Counters write their samples with `_total`, histograms
 * with `_bucket`, `_count` and `_sum`, and info families with `_info`. The mapping is deterministic, and the registry
 * refuses a family whose names collide with those of another family after the mapping.
 *
 * @since 1.0.0
 */
public object PrometheusNaming {
    /**
     * The suffixes of sample names that a family name may not end with, because they would make it read as a sample
     * of a counter, a histogram or an info family. `_count` and `_sum` stay allowed, as OpenTelemetry uses them for
     * gauges such as `jvm.thread.count`; a family whose names collide with the samples of a histogram is refused by
     * the registry.
     *
     * @since 1.0.0
     */
    public val RESERVED_SUFFIXES: List<String> = listOf(TOTAL, CREATED, BUCKET, INFO)

    /**
     * Returns the Prometheus name of the family [descriptor] describes.
     *
     * @param descriptor the family.
     * @return the name, such as `http_server_duration_seconds`.
     * @since 1.0.0
     */
    public fun familyName(descriptor: Descriptor): String {
        val base = descriptor.name.value.replace(oldChar = '.', newChar = SEPARATOR)
        val suffix = descriptor.unit.prometheusSuffix?.let { unit -> "$SEPARATOR$unit" }
        if (suffix == null || base.endsWith(suffix)) return base
        return base + suffix
    }

    /**
     * Returns the Prometheus name of the label key [key].
     *
     * @param key the key.
     * @return the name, such as `http_request_method`.
     * @since 1.0.0
     */
    public fun labelName(key: LabelKey): String = key.value.replace(oldChar = '.', newChar = SEPARATOR)

    /**
     * Returns every sample name the family [descriptor] describes writes, in Prometheus text and in OpenMetrics.
     *
     * @param descriptor the family.
     * @return the sample names.
     * @since 1.0.0
     */
    public fun sampleNames(descriptor: Descriptor): List<String> {
        val family = familyName(descriptor)
        return when (descriptor.kind) {
            MeterKind.Counter -> listOf(family + TOTAL, family + CREATED)
            MeterKind.UpDownCounter, MeterKind.Gauge, MeterKind.StateSet -> listOf(family)
            MeterKind.Histogram -> listOf(family + BUCKET, family + COUNT, family + SUM, family + CREATED)
            MeterKind.Info -> listOf(family + INFO)
        }
    }

    /**
     * Tells whether the family name of [descriptor] ends with one of [RESERVED_SUFFIXES].
     *
     * @param descriptor the family.
     * @return true if the family name is reserved.
     * @since 1.0.0
     */
    public fun isReserved(descriptor: Descriptor): Boolean {
        val family = familyName(descriptor)
        return RESERVED_SUFFIXES.any(family::endsWith)
    }
}
