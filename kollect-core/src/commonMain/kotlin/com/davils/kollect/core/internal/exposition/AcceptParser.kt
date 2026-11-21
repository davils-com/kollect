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

package com.davils.kollect.core.internal.exposition

import com.davils.kollect.core.exposition.ExpositionFormat

private const val MAXIMUM_LENGTH: Int = 4_096
private const val MAXIMUM_RANGES: Int = 32
private const val OPEN_METRICS: String = "application/openmetrics-text"
private const val TEXT_VERSION: String = "0.0.4"
private val OPEN_METRICS_VERSIONS: Set<String?> = setOf(null, "1.0.0", "0.0.1")
private val TEXT_TYPES: Set<String> = setOf("text/plain", "text/*", "*/*")

internal object AcceptParser {
    fun formatOf(accept: String?): ExpositionFormat {
        val header = accept.orEmpty().take(MAXIMUM_LENGTH)
        val texts = header.split(',').take(MAXIMUM_RANGES)
        val ranges = texts.mapNotNull(::rangeOf)
        val openMetrics = qualityAmong(ranges.filter(::isOpenMetrics))
        val text = qualityAmong(ranges.filter(::isText))
        val isOpenMetricsPreferred = openMetrics > 0.0 && openMetrics >= text
        return ExpositionFormat.OpenMetrics.takeIf { isOpenMetricsPreferred } ?: ExpositionFormat.PrometheusText
    }

    private fun qualityAmong(ranges: List<MediaRange>): Double = ranges.maxOfOrNull(MediaRange::quality) ?: 0.0

    private fun isOpenMetrics(range: MediaRange): Boolean =
        range.type == OPEN_METRICS && range.parameter("version") in OPEN_METRICS_VERSIONS

    private fun isText(range: MediaRange): Boolean {
        val version = range.parameter("version")
        return range.type in TEXT_TYPES && (version == null || version == TEXT_VERSION)
    }

    private fun rangeOf(text: String): MediaRange? {
        val parts = text.split(';').map(String::trim)
        val parameters = parts.drop(1).mapNotNull(::parameterOf)
        val range = MediaRange(parts.first().lowercase(), parameters, quality = 1.0)
        val written = range.parameter("q") ?: return range
        return qualityOf(written)?.let { quality -> range.copy(quality = quality) }
    }

    private fun parameterOf(text: String): Pair<String, String>? {
        val separator = text.indexOf('=')
        if (separator <= 0) return null
        val name = text.take(separator).trim()
        val value = text.substring(separator + 1).trim()
        return name.lowercase() to value.removeSurrounding("\"")
    }

    private fun qualityOf(text: String): Double? {
        val quality = text.toDoubleOrNull()
        return quality?.takeIf { written -> written in 0.0..1.0 }
    }

    private fun MediaRange.parameter(name: String): String? = parameters.firstOrNull { (key, _) -> key == name }?.second

    private data class MediaRange(val type: String, val parameters: List<Pair<String, String>>, val quality: Double)
}
