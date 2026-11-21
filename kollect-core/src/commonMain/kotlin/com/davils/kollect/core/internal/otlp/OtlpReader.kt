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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.internal.text.Decimals
import com.davils.kollect.core.internal.wire.Malformed
import com.davils.kollect.core.internal.wire.ProtoReader
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget

private const val WORK_PER_BYTE: Long = 8L
private const val INSTANCE_KEY: String = "service.instance.id"
private const val EXPONENTIAL_HISTOGRAM: Int = 10
private const val SUMMARY: Int = 11

internal class OtlpReader(private val limits: IngestLimits) {
    private var points: Int = 0

    fun read(bytes: ByteArray): List<DecodedResource> {
        val budget = WorkBudget(maximum = WORK_PER_BYTE * bytes.size + 1L, limit = "otlp decoding")
        val reader = ProtoReader.of(bytes, budget)
        val resources = mutableListOf<DecodedResource>()
        reader.fields(
            mapOf(OtlpFields.REQUEST_RESOURCE_METRICS to { field -> resources.add(resourceOf(field.message())) }),
        )
        return resources
    }

    private fun resourceOf(reader: ProtoReader): DecodedResource {
        var instance = ""
        val metrics = mutableListOf<DecodedMetric>()
        reader.fields(
            mapOf(
                OtlpFields.RESOURCE_METRICS_RESOURCE to { field -> instance = instanceOf(field.message()) },
                OtlpFields.RESOURCE_METRICS_SCOPE_METRICS to { field -> metrics.addAll(scopeOf(field.message())) },
            ),
        )
        return DecodedResource(instance, metrics)
    }

    private fun instanceOf(reader: ProtoReader): String {
        var instance = ""
        reader.fields(
            mapOf(
                OtlpFields.RESOURCE_ATTRIBUTES to { field ->
                    val attribute = attributeOf(field.message())
                    if (attribute?.first == INSTANCE_KEY) instance = attribute.second
                },
            ),
        )
        return instance
    }

    private fun scopeOf(reader: ProtoReader): List<DecodedMetric> {
        val metrics = mutableListOf<DecodedMetric>()
        reader.fields(mapOf(OtlpFields.SCOPE_METRICS_METRICS to { field -> metrics.add(metricOf(field.message())) }))
        return metrics
    }

    private fun metricOf(reader: ProtoReader): DecodedMetric {
        var name = ""
        var description = ""
        var unit = ""
        var data: DecodedData? = null
        val setData: (DecodedData) -> Unit = { decoded ->
            if (data != null) throw Malformed("a metric has more than one type")
            data = decoded
        }
        reader.fields(
            mapOf(
                OtlpFields.METRIC_NAME to { field -> name = field.string(limits.maximumStringBytes) },
                OtlpFields.METRIC_DESCRIPTION to { field -> description = field.string(limits.maximumStringBytes) },
                OtlpFields.METRIC_UNIT to { field -> unit = field.string(limits.maximumStringBytes) },
                OtlpFields.METRIC_GAUGE to { field -> setData(dataOf(field.message(), DecodedType.Gauge)) },
                OtlpFields.METRIC_SUM to { field -> setData(dataOf(field.message(), DecodedType.Sum)) },
                OtlpFields.METRIC_HISTOGRAM to { field -> setData(dataOf(field.message(), DecodedType.Histogram)) },
                EXPONENTIAL_HISTOGRAM to { field -> setData(unsupportedOf(field)) },
                SUMMARY to { field -> setData(unsupportedOf(field)) },
            ),
        )
        val decoded = data ?: throw Malformed("a metric has no type")
        return DecodedMetric(
            name = name,
            description = description,
            unit = unit,
            type = decoded.type,
            temporality = decoded.temporality,
            isMonotonic = decoded.isMonotonic,
            points = decoded.points,
        )
    }

    private fun unsupportedOf(reader: ProtoReader): DecodedData {
        reader.skip()
        return DecodedData(
            type = DecodedType.Unsupported,
            points = listOf(DecodedPoint(attributes = null)),
            temporality = 0L,
            isMonotonic = false,
        )
    }

    private fun dataOf(reader: ProtoReader, type: DecodedType): DecodedData {
        val decoded = mutableListOf<DecodedPoint>()
        var temporality = 0L
        var isMonotonic = false
        reader.fields(
            mapOf(
                OtlpFields.DATA_POINTS to { field -> decoded.add(pointOf(field.message(), type)) },
                OtlpFields.TEMPORALITY to { field -> temporality = field.uint64() },
                OtlpFields.MONOTONIC to { field -> isMonotonic = field.bool() },
            ),
        )
        return DecodedData(type = type, points = decoded, temporality = temporality, isMonotonic = isMonotonic)
    }

    private fun pointOf(reader: ProtoReader, type: DecodedType): DecodedPoint {
        points += 1
        if (points > limits.maximumPointsPerRequest) throw Oversized()
        if (type == DecodedType.Histogram) return histogramPointOf(reader)
        return numberPointOf(reader)
    }

    private fun numberPointOf(reader: ProtoReader): DecodedPoint {
        var point = DecodedPoint(attributes = emptyList())
        reader.fields(
            mapOf(
                OtlpFields.NUMBER_ATTRIBUTES to { field ->
                    point = point.copy(
                        attributes = withAttribute(point, field),
                    )
                },
                OtlpFields.NUMBER_START to { field -> point = point.copy(start = field.fixed64()) },
                OtlpFields.NUMBER_TIME to { field -> point = point.copy(time = field.fixed64()) },
                OtlpFields.NUMBER_DOUBLE to { field -> point = point.copy(value = field.double()) },
                OtlpFields.NUMBER_INT to { field -> point = point.copy(value = field.fixed64().toDouble()) },
                OtlpFields.NUMBER_FLAGS to { field -> point = point.copy(flags = field.uint64()) },
            ),
        )
        return point
    }

    private fun histogramPointOf(reader: ProtoReader): DecodedPoint {
        var point = DecodedPoint(attributes = emptyList())
        val buckets = limits.maximumBuckets
        reader.fields(
            mapOf(
                OtlpFields.HISTOGRAM_ATTRIBUTES to { field ->
                    point = point.copy(
                        attributes = withAttribute(point, field),
                    )
                },
                OtlpFields.HISTOGRAM_START to { field -> point = point.copy(start = field.fixed64()) },
                OtlpFields.HISTOGRAM_TIME to { field -> point = point.copy(time = field.fixed64()) },
                OtlpFields.HISTOGRAM_COUNT to { field -> point = point.copy(count = field.fixed64()) },
                OtlpFields.HISTOGRAM_SUM to { field -> point = point.copy(sum = field.double()) },
                OtlpFields.HISTOGRAM_BUCKETS to { field ->
                    point = point.copy(
                        buckets = point.buckets + field.packedFixed64(buckets),
                    )
                },
                OtlpFields.HISTOGRAM_BOUNDS to { field ->
                    val bounds = field.packedFixed64(buckets).map(Double::fromBits)
                    point = point.copy(bounds = point.bounds + bounds)
                },
                OtlpFields.HISTOGRAM_FLAGS to { field -> point = point.copy(flags = field.uint64()) },
            ),
        )
        if (point.buckets.size > buckets + 1 || point.bounds.size > buckets) {
            throw Malformed(
                "a histogram has too many buckets",
            )
        }
        return point
    }

    private fun withAttribute(point: DecodedPoint, reader: ProtoReader): List<Pair<String, String>>? {
        val attribute = attributeOf(reader.message())
        val attributes = point.attributes ?: return null
        val isWithinLimit = attributes.size < limits.maximumAttributes
        return attribute?.takeIf { isWithinLimit }?.let { valid -> attributes + valid }
    }

    private fun attributeOf(reader: ProtoReader): Pair<String, String>? {
        var key = ""
        var value: String? = null
        reader.fields(
            mapOf(
                OtlpFields.KEY_VALUE_KEY to { field -> key = field.string(limits.maximumStringBytes) },
                OtlpFields.KEY_VALUE_VALUE to { field -> value = valueOf(field.message()) },
            ),
        )
        return value?.let { text -> key to text }
    }

    private fun valueOf(reader: ProtoReader): String? {
        var value: String? = null
        reader.fields(
            mapOf(
                OtlpFields.ANY_STRING to { field -> value = field.string(limits.maximumStringBytes) },
                OtlpFields.ANY_BOOL to { field -> value = field.bool().toString() },
                OtlpFields.ANY_INT to { field -> value = field.uint64().toString() },
                OtlpFields.ANY_DOUBLE to { field -> value = Decimals.text(field.double()) },
            ),
        )
        return value
    }

    private fun ProtoReader.fields(handlers: Map<Int, (ProtoReader) -> Unit>) {
        while (hasMore) {
            next()
            val handler = handlers[field]
            if (handler == null) skip()
            handler?.invoke(this)
        }
    }
}
