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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.Payload
import com.davils.kollect.core.internal.wire.ProtoWriter
import com.davils.kollect.core.limit.ExportLimits
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.otlp.OtlpBatch
import com.davils.kollect.core.otlp.Resource
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Temporality
import kotlin.time.Instant

private const val NANOSECONDS_PER_SECOND: Long = 1_000_000_000L
private const val ENVELOPE_RESERVE: Int = 512
private const val SCOPE_NAME: String = "kollect"
private const val PROMETHEUS_TYPE: String = "prometheus.type"

internal class OtlpWriter(private val resource: Resource, private val limits: ExportLimits) {
    fun write(snapshot: MetricsSnapshot): OtlpBatch {
        val time = nanosOf(snapshot.timestamp)
        val metrics = snapshot.families.flatMap { family -> metricsOf(family, time) }
        val (fitting, oversized) = metrics.partition { metric -> metric.bytes.size <= budget() }
        val requests = packed(fitting)
        return OtlpBatch(
            payloads = requests.map(::payloadOf),
            pointsPerPayload = requests.map { request -> request.sumOf(EncodedMetric::points) },
            points = fitting.sumOf(EncodedMetric::points),
            droppedPoints = oversized.sumOf(EncodedMetric::points),
        )
    }

    private fun budget(): Int = limits.maximumPayloadBytes - ENVELOPE_RESERVE

    private fun packed(metrics: List<EncodedMetric>): List<List<EncodedMetric>> {
        val requests = mutableListOf<MutableList<EncodedMetric>>()
        metrics.forEach { metric ->
            val current = requests.lastOrNull()
            val isFitting = current != null && isFittingInto(current, metric)
            if (!isFitting) requests.add(mutableListOf())
            requests.last().add(metric)
        }
        return requests
    }

    private fun isFittingInto(request: List<EncodedMetric>, metric: EncodedMetric): Boolean {
        val bytes = request.sumOf { encoded -> encoded.bytes.size } + metric.bytes.size
        val points = request.sumOf(EncodedMetric::points) + metric.points
        return bytes <= budget() && points <= limits.maximumPointsPerPayload
    }

    private fun payloadOf(metrics: List<EncodedMetric>): Payload {
        val writer = ProtoWriter()
        writer.message(OtlpFields.REQUEST_RESOURCE_METRICS) {
            message(OtlpFields.RESOURCE_METRICS_RESOURCE) {
                resource.attributes.forEach { (key, value) ->
                    attribute(
                        field = OtlpFields.RESOURCE_ATTRIBUTES,
                        key = key,
                        value = value,
                    )
                }
            }
            message(OtlpFields.RESOURCE_METRICS_SCOPE_METRICS) {
                message(OtlpFields.SCOPE_METRICS_SCOPE) { string(OtlpFields.SCOPE_NAME, SCOPE_NAME) }
                metrics.forEach { metric -> bytes(OtlpFields.SCOPE_METRICS_METRICS, metric.bytes) }
            }
        }
        return Payload(writer.toByteArray(), OtlpFields.CONTENT_TYPE)
    }

    private fun metricsOf(family: FamilySnapshot, time: Long): List<EncodedMetric> {
        val pointsPerMetric = maxOf(a = 1, b = limits.maximumPointsPerPayload)
        return family.points.chunked(pointsPerMetric).map { points -> metricOf(family, points, time) }
    }

    private fun metricOf(family: FamilySnapshot, points: List<Point>, time: Long): EncodedMetric {
        val descriptor = family.descriptor
        val writer = ProtoWriter()
        writer.string(OtlpFields.METRIC_NAME, descriptor.name.value)
        writer.string(OtlpFields.METRIC_DESCRIPTION, descriptor.description)
        writer.string(OtlpFields.METRIC_UNIT, descriptor.unit.ucum)
        writer.data(descriptor = descriptor, temporality = family.temporality, points = points, time = time)
        metadataOf(
            descriptor.kind,
        )?.let { type -> writer.attribute(field = OtlpFields.METRIC_METADATA, key = PROMETHEUS_TYPE, value = type) }
        val count = points.sumOf { point -> descriptor.states.size.takeIf { point is StateSetPoint } ?: 1 }
        return EncodedMetric(writer.toByteArray(), count)
    }

    private fun ProtoWriter.data(descriptor: Descriptor, temporality: Temporality, points: List<Point>, time: Long) {
        val field = when (descriptor.kind) {
            MeterKind.Gauge -> OtlpFields.METRIC_GAUGE
            MeterKind.Histogram -> OtlpFields.METRIC_HISTOGRAM
            MeterKind.Counter, MeterKind.UpDownCounter, MeterKind.StateSet, MeterKind.Info -> OtlpFields.METRIC_SUM
        }
        val content = Content(descriptor = descriptor, temporality = temporality, points = points, time = time)
        message(field) { body(content) }
    }

    private fun ProtoWriter.body(content: Content) {
        when (content.descriptor.kind) {
            MeterKind.Gauge -> numbers(content.descriptor, content.points, content.time)
            MeterKind.Histogram -> histograms(content)
            MeterKind.Counter, MeterKind.UpDownCounter, MeterKind.StateSet, MeterKind.Info -> sums(content)
        }
    }

    private fun ProtoWriter.histograms(content: Content) {
        content.points.filterIsInstance<HistogramPoint>().forEach { point ->
            histogramPoint(
                content.descriptor,
                point,
                content.time,
            )
        }
        uint64(OtlpFields.TEMPORALITY, temporalityOf(content.temporality))
    }

    private fun ProtoWriter.sums(content: Content) {
        numbers(content.descriptor, content.points, content.time)
        uint64(OtlpFields.TEMPORALITY, temporalityOf(content.temporality))
        bool(OtlpFields.MONOTONIC, content.descriptor.kind == MeterKind.Counter)
    }

    private fun ProtoWriter.numbers(descriptor: Descriptor, points: List<Point>, time: Long) {
        points.forEach { point ->
            when (point) {
                is SumPoint -> number(labels = point.labels, start = point.start, time = time, value = point.value)

                is GaugePoint -> number(
                    labels = point.labels,
                    start = point.start,
                    time = nanosOf(point.observedAt),
                    value = point.value,
                )

                is StateSetPoint -> states(descriptor, point, time)

                is InfoPoint -> number(labels = point.labels, start = point.start, time = time, value = 1.0)

                is HistogramPoint -> Unit
            }
        }
    }

    private fun ProtoWriter.states(descriptor: Descriptor, point: StateSetPoint, time: Long) {
        descriptor.states.forEachIndexed { index, state ->
            val value = 1.0.takeIf { index == point.active } ?: 0.0
            message(OtlpFields.DATA_POINTS) {
                labels(OtlpFields.NUMBER_ATTRIBUTES, point.labels)
                attribute(field = OtlpFields.NUMBER_ATTRIBUTES, key = descriptor.name.value, value = state)
                timed(point.start, time)
                double(OtlpFields.NUMBER_DOUBLE, value)
            }
        }
    }

    private fun ProtoWriter.number(labels: Labels, start: Instant, time: Long, value: Double) {
        message(OtlpFields.DATA_POINTS) {
            labels(OtlpFields.NUMBER_ATTRIBUTES, labels)
            timed(start, time)
            double(OtlpFields.NUMBER_DOUBLE, value)
        }
    }

    private fun ProtoWriter.histogramPoint(descriptor: Descriptor, histogram: HistogramPoint, time: Long) {
        message(OtlpFields.DATA_POINTS) {
            labels(OtlpFields.HISTOGRAM_ATTRIBUTES, histogram.labels)
            fixed64(OtlpFields.HISTOGRAM_START, nanosOf(histogram.start))
            fixed64(OtlpFields.HISTOGRAM_TIME, time)
            fixed64(OtlpFields.HISTOGRAM_COUNT, histogram.count)
            double(OtlpFields.HISTOGRAM_SUM, histogram.sum)
            packedFixed64(OtlpFields.HISTOGRAM_BUCKETS, histogram.bucketCounts)
            packedDoubles(OtlpFields.HISTOGRAM_BOUNDS, descriptor.boundValues)
            histogram.min?.let { min -> double(OtlpFields.HISTOGRAM_MIN, min) }
            histogram.max?.let { max -> double(OtlpFields.HISTOGRAM_MAX, max) }
        }
    }

    private fun ProtoWriter.timed(start: Instant, time: Long) {
        fixed64(OtlpFields.NUMBER_START, nanosOf(start))
        fixed64(OtlpFields.NUMBER_TIME, time)
    }

    private fun ProtoWriter.labels(field: Int, labels: Labels) {
        labels.keys.forEachIndexed { index, key ->
            attribute(
                field = field,
                key = key.value,
                value = labels.values[index],
            )
        }
    }

    private fun ProtoWriter.attribute(field: Int, key: String, value: String) {
        message(field) {
            string(OtlpFields.KEY_VALUE_KEY, key)
            message(OtlpFields.KEY_VALUE_VALUE) { string(OtlpFields.ANY_STRING, value) }
        }
    }

    private fun metadataOf(kind: MeterKind): String? = when (kind) {
        MeterKind.StateSet -> "stateset"
        MeterKind.Info -> "info"
        MeterKind.Counter, MeterKind.UpDownCounter, MeterKind.Gauge, MeterKind.Histogram -> null
    }

    private fun temporalityOf(temporality: Temporality): Long = when (temporality) {
        Temporality.Cumulative -> OtlpFields.TEMPORALITY_CUMULATIVE
        Temporality.Delta -> OtlpFields.TEMPORALITY_DELTA
    }

    private fun nanosOf(instant: Instant): Long =
        instant.epochSeconds * NANOSECONDS_PER_SECOND + instant.nanosecondsOfSecond

    private data class Content(
        val descriptor: Descriptor,
        val temporality: Temporality,
        val points: List<Point>,
        val time: Long,
    )

    @Suppress("UseDataClass")
    private class EncodedMetric(val bytes: ByteArray, val points: Int)
}
