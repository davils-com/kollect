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

import com.davils.kollect.core.fixture.HistogramSpec
import com.davils.kollect.core.fixture.NOW_NANOS
import com.davils.kollect.core.fixture.NumberSpec
import com.davils.kollect.core.fixture.gauge
import com.davils.kollect.core.fixture.histogram
import com.davils.kollect.core.fixture.metric
import com.davils.kollect.core.fixture.number
import com.davils.kollect.core.fixture.sum
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.wire.ProtoWriter
import com.davils.kollect.core.limit.ingestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val PROTOBUF: String = "application/x-protobuf"
private const val CUMULATIVE: Long = 2L
private const val ITEMS: String = "app.items"
private val tenant: Source = Source.of("tenant")

private fun requestWithResource(resource: ProtoWriter.() -> Unit, metrics: ProtoWriter.() -> Unit): ByteArray {
    val writer = ProtoWriter()
    writer.message(OtlpFields.REQUEST_RESOURCE_METRICS) {
        message(OtlpFields.RESOURCE_METRICS_RESOURCE) { resource() }
        message(OtlpFields.RESOURCE_METRICS_SCOPE_METRICS) { metrics() }
    }
    return writer.toByteArray()
}

class DecodingTest : FunSpec({
    test("an instance without a string id, descriptions that are invalid and histograms with attributes are read") {
        val ingestion = otlpIngestion(testClock()) { acceptAnyFamily("app.") }
        val histogram = HistogramSpec(
            count = 1L,
            sum = 1.0,
            buckets = listOf(1L, 0L),
            bounds = listOf(1.0),
            attributes = listOf("route" to "/"),
        )
        val body = requestWithResource(
            resource = {
                message(OtlpFields.RESOURCE_ATTRIBUTES) {
                    string(OtlpFields.KEY_VALUE_KEY, "service.instance.id")
                    message(OtlpFields.KEY_VALUE_VALUE) { uint64(OtlpFields.ANY_INT, 1L) }
                }
            },
        ) {
            message(OtlpFields.SCOPE_METRICS_METRICS) {
                string(OtlpFields.METRIC_NAME, ITEMS)
                string(OtlpFields.METRIC_DESCRIPTION, "bad\u0000text")
                message(OtlpFields.METRIC_GAUGE) { number(NumberSpec(1.0)) }
            }
            metric(name = "app.latency", unit = "s") { histogram(CUMULATIVE, histogram) }
        }
        ingestion.ingest(tenant, PROTOBUF, body).verdict shouldBe IngestVerdict.Accepted
    }

    test("buckets spread over several fields still count against the limit") {
        val ingestion = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumBuckets = 1 }
            acceptAnyFamily("app.")
        }
        val body = requestWithResource(resource = {}) {
            metric(name = "app.latency", unit = "s") {
                message(OtlpFields.METRIC_HISTOGRAM) {
                    message(OtlpFields.DATA_POINTS) {
                        packedFixed64(OtlpFields.HISTOGRAM_BUCKETS, listOf(1L))
                        packedFixed64(OtlpFields.HISTOGRAM_BUCKETS, listOf(1L))
                        packedFixed64(OtlpFields.HISTOGRAM_BUCKETS, listOf(1L))
                    }
                }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, body).verdict shouldBe IngestVerdict.Malformed
    }

    test("a counter that restarts twice keeps its total") {
        val ingestion = otlpIngestion(testClock()) { acceptFamily(ITEMS, MeterKind.Counter, MeasureUnit.NONE) }
        listOf(
            THREE to NOW_NANOS - THREE_NANOS,
            1.0 to NOW_NANOS - 2L,
            HALF to NOW_NANOS - 1L,
        ).forEach { (value, time) ->
            val body = requestWithResource(resource = {}) {
                metric(
                    ITEMS,
                ) { sum(isMonotonic = true, temporality = CUMULATIVE) { number(NumberSpec(value, time = time)) } }
            }
            ingestion.ingest(tenant, PROTOBUF, body)
        }
        ingestion.sums(ITEMS) shouldContainExactly listOf(THREE + 1.0 + HALF)
    }

    test("hostile histograms are invalid") {
        val ingestion = otlpIngestion(testClock()) { acceptAnyFamily("app.") }
        val specs = listOf(
            HistogramSpec(count = 1L, sum = 1.0, buckets = listOf(1L, 0L), bounds = listOf(Double.POSITIVE_INFINITY)),
            HistogramSpec(count = 1L, sum = 1.0, buckets = listOf(1L, 0L, 0L), bounds = listOf(2.0, 1.0)),
            HistogramSpec(count = 0L, sum = 0.0, buckets = listOf(-1L, 1L), bounds = listOf(1.0)),
            HistogramSpec(count = 1L, sum = Double.NaN, buckets = listOf(1L, 0L), bounds = listOf(1.0)),
            HistogramSpec(count = 0L, sum = 0.0, buckets = listOf(Long.MAX_VALUE, 1L), bounds = listOf(1.0)),
        )
        val body = requestWithResource(resource = {}) {
            specs.forEachIndexed { index, spec ->
                metric(
                    name = "app.latency.h$index",
                    unit = "s",
                ) { histogram(CUMULATIVE, spec) }
            }
            metric(
                "app.counter",
            ) { sum(isMonotonic = true, temporality = CUMULATIVE) { number(NumberSpec(Double.POSITIVE_INFINITY)) } }
            metric("app.gauge") { gauge { number(NumberSpec(1.0, attributes = listOf("otel.x" to "1"))) } }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe
            mapOf(IngestRejection.InvalidValue to SIX, IngestRejection.InvalidLabels to 1L)
    }
})

private const val THREE: Double = 3.0
private const val HALF: Double = 0.5
private const val THREE_NANOS: Long = 3L
private const val SIX: Long = 6L
