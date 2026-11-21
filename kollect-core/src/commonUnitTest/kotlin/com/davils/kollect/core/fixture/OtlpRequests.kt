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

package com.davils.kollect.core.fixture

import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.wire.ProtoWriter

internal const val INSTANCE: String = "instance-1"
internal const val NANOS_PER_MILLISECOND: Long = 1_000_000L
internal const val NOW_NANOS: Long = EPOCH_MILLISECONDS * NANOS_PER_MILLISECOND

internal fun otlpRequest(instance: String = INSTANCE, metrics: ProtoWriter.() -> Unit): ByteArray {
    val writer = ProtoWriter()
    writer.message(OtlpFields.REQUEST_RESOURCE_METRICS) {
        message(
            OtlpFields.RESOURCE_METRICS_RESOURCE,
        ) { attribute(field = OtlpFields.RESOURCE_ATTRIBUTES, key = "service.instance.id", value = instance) }
        message(OtlpFields.RESOURCE_METRICS_SCOPE_METRICS) { metrics() }
    }
    return writer.toByteArray()
}

internal fun ProtoWriter.metric(name: String, unit: String = "", data: ProtoWriter.() -> Unit) {
    message(OtlpFields.SCOPE_METRICS_METRICS) {
        string(OtlpFields.METRIC_NAME, name)
        string(OtlpFields.METRIC_UNIT, unit)
        data()
    }
}

internal fun ProtoWriter.sum(isMonotonic: Boolean, temporality: Long, points: ProtoWriter.() -> Unit) {
    message(OtlpFields.METRIC_SUM) {
        points()
        uint64(OtlpFields.TEMPORALITY, temporality)
        bool(OtlpFields.MONOTONIC, isMonotonic)
    }
}

internal fun ProtoWriter.gauge(points: ProtoWriter.() -> Unit) {
    message(OtlpFields.METRIC_GAUGE) { points() }
}

internal fun ProtoWriter.number(point: NumberSpec) {
    message(OtlpFields.DATA_POINTS) {
        point.attributes.forEach { (key, value) -> attribute(OtlpFields.NUMBER_ATTRIBUTES, key, value) }
        fixed64(OtlpFields.NUMBER_START, point.start)
        fixed64(OtlpFields.NUMBER_TIME, point.time)
        double(OtlpFields.NUMBER_DOUBLE, point.value)
        uint64(OtlpFields.NUMBER_FLAGS, point.flags)
    }
}

internal fun ProtoWriter.histogram(temporality: Long, point: HistogramSpec) {
    message(OtlpFields.METRIC_HISTOGRAM) {
        message(OtlpFields.DATA_POINTS) {
            point.attributes.forEach { (key, value) -> attribute(OtlpFields.HISTOGRAM_ATTRIBUTES, key, value) }
            fixed64(OtlpFields.HISTOGRAM_START, point.start)
            fixed64(OtlpFields.HISTOGRAM_TIME, point.time)
            fixed64(OtlpFields.HISTOGRAM_COUNT, point.count)
            double(OtlpFields.HISTOGRAM_SUM, point.sum)
            packedFixed64(OtlpFields.HISTOGRAM_BUCKETS, point.buckets)
            packedDoubles(OtlpFields.HISTOGRAM_BOUNDS, point.bounds)
        }
        uint64(OtlpFields.TEMPORALITY, temporality)
    }
}

internal fun ProtoWriter.attribute(field: Int, key: String, value: String) {
    message(field) {
        string(OtlpFields.KEY_VALUE_KEY, key)
        message(OtlpFields.KEY_VALUE_VALUE) { string(OtlpFields.ANY_STRING, value) }
    }
}
