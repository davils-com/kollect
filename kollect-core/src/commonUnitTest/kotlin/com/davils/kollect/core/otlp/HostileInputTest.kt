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
import com.davils.kollect.core.fixture.NumberSpec
import com.davils.kollect.core.fixture.attribute
import com.davils.kollect.core.fixture.gauge
import com.davils.kollect.core.fixture.histogram
import com.davils.kollect.core.fixture.metric
import com.davils.kollect.core.fixture.number
import com.davils.kollect.core.fixture.otlpRequest
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.internal.otlp.DecodedPoint
import com.davils.kollect.core.internal.otlp.IngestedFamily
import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.otlp.PointChecks
import com.davils.kollect.core.internal.wire.ProtoWriter
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.limit.ingestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.MetricName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val LATENCY: String = "app.latency"
private const val PROTOBUF: String = "application/x-protobuf"
private const val CUMULATIVE: Long = 2L
private const val ITEMS: String = "app.items"
private val tenant: Source = Source.of("tenant")
private val other: Source = Source.of("other")

class HostileInputTest : FunSpec({
    test("invalid keys, other bounds and invalid attributes are rejected") {
        val ingestion = otlpIngestion(testClock()) {
            acceptFamily(ITEMS, MeterKind.Gauge)
            acceptAnyFamily("app.")
        }
        val bounded = HistogramSpec(count = 1L, sum = 1.0, buckets = listOf(1L, 0L), bounds = listOf(1.0))
        val body = otlpRequest {
            metric(ITEMS) { gauge { number(NumberSpec(1.0, attributes = listOf("Bad" to "1"))) } }
            metric(name = LATENCY, unit = "s") { histogram(CUMULATIVE, bounded) }
            metric(name = LATENCY, unit = "s") { histogram(CUMULATIVE, bounded.copy(bounds = listOf(2.0))) }
            metric(ITEMS) {
                gauge {
                    message(OtlpFields.DATA_POINTS) {
                        message(OtlpFields.NUMBER_ATTRIBUTES) {
                            string(OtlpFields.KEY_VALUE_KEY, "list")
                            message(OtlpFields.KEY_VALUE_VALUE) { bytes(KVLIST, ByteArray(0)) }
                        }
                        attribute(field = OtlpFields.NUMBER_ATTRIBUTES, key = "valid", value = "1")
                        double(OtlpFields.NUMBER_DOUBLE, 1.0)
                    }
                }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe
            mapOf(IngestRejection.Mismatch to 1L, IngestRejection.InvalidLabels to 2L)
    }

    test("bounds spread over several fields and negative lengths are malformed") {
        val ingestion = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumBuckets = 1 }
            acceptAnyFamily("app.")
        }
        val spread = otlpRequest {
            metric(name = LATENCY, unit = "s") {
                message(OtlpFields.METRIC_HISTOGRAM) {
                    message(OtlpFields.DATA_POINTS) {
                        packedDoubles(OtlpFields.HISTOGRAM_BOUNDS, listOf(1.0))
                        packedDoubles(OtlpFields.HISTOGRAM_BOUNDS, listOf(2.0))
                    }
                }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, spread).verdict shouldBe IngestVerdict.Malformed
        val negative = byteArrayOf(0x0A) + ByteArray(NINE) { -1 } + byteArrayOf(1)
        ingestion.ingest(tenant, PROTOBUF, negative).verdict shouldBe IngestVerdict.Malformed
    }

    test("other resource attributes are ignored and forgetting one source keeps the others") {
        val ingestion = otlpIngestion(testClock()) { acceptFamily(ITEMS, MeterKind.Gauge, MeasureUnit.NONE) }
        val writer = ProtoWriter()
        writer.message(OtlpFields.REQUEST_RESOURCE_METRICS) {
            message(
                OtlpFields.RESOURCE_METRICS_RESOURCE,
            ) { attribute(field = OtlpFields.RESOURCE_ATTRIBUTES, key = "service.name", value = "shop") }
            message(OtlpFields.RESOURCE_METRICS_SCOPE_METRICS) { metric(ITEMS) { gauge { number(NumberSpec(1.0)) } } }
        }
        ingestion.ingest(tenant, PROTOBUF, writer.toByteArray()).verdict shouldBe IngestVerdict.Accepted
        ingestion.ingest(other, PROTOBUF, writer.toByteArray())
        ingestion.forget(tenant) shouldBe true
        ingestion.sourceCount shouldBe 1
    }

    test("a point without attributes has no label values") {
        val family = IngestedFamily(
            name = MetricName.of(ITEMS),
            kind = MeterKind.Gauge,
            unit = MeasureUnit.NONE,
            description = "",
            keys = emptyList(),
            bounds = null,
        )
        val checks = PointChecks(IngestLimits.Default)
        checks.valuesOf(family, DecodedPoint(attributes = null)).shouldBeEmpty()
    }
})

private const val KVLIST: Int = 6
private const val NINE: Int = 9
