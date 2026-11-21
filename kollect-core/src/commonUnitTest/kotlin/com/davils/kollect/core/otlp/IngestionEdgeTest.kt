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
import com.davils.kollect.core.fixture.histogramPoint
import com.davils.kollect.core.fixture.metric
import com.davils.kollect.core.fixture.number
import com.davils.kollect.core.fixture.otlpRequest
import com.davils.kollect.core.fixture.sum
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.wire.ProtoWriter
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.limit.ingestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

private const val PROTOBUF: String = "application/x-protobuf"
private const val CUMULATIVE: Long = 2L
private const val DELTA: Long = 1L
private const val LATENCY: String = "app.latency"
private const val ITEMS: String = "app.items"

private val tenant: Source = Source.of("tenant")

private fun ProtoWriter.typedAttribute(key: String, value: ProtoWriter.() -> Unit) {
    message(OtlpFields.NUMBER_ATTRIBUTES) {
        string(OtlpFields.KEY_VALUE_KEY, key)
        message(OtlpFields.KEY_VALUE_VALUE) { value() }
    }
}

class IngestionEdgeTest : FunSpec({
    test("attributes of every scalar type become label values and others are invalid") {
        val ingestion = otlpIngestion(testClock()) { acceptAnyFamily("app.") }
        val body = otlpRequest {
            metric("app.values") {
                gauge {
                    message(OtlpFields.DATA_POINTS) {
                        typedAttribute("flag") { uint64(OtlpFields.ANY_BOOL, 1L) }
                        typedAttribute("count") { uint64(OtlpFields.ANY_INT, 2L) }
                        typedAttribute("ratio") { double(OtlpFields.ANY_DOUBLE, 0.5) }
                        fixed64(OtlpFields.NUMBER_TIME, NOW_NANOS)
                        double(OtlpFields.NUMBER_DOUBLE, 1.0)
                    }
                    message(OtlpFields.DATA_POINTS) {
                        typedAttribute("list") { bytes(KVLIST, ByteArray(0)) }
                        fixed64(OtlpFields.NUMBER_TIME, NOW_NANOS)
                        fixed64(OtlpFields.NUMBER_INT, 1L)
                    }
                }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe mapOf(IngestRejection.InvalidLabels to 1L)
    }

    test("too many attributes or buckets are refused") {
        val few = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumAttributes = 1 }
            acceptAnyFamily("app.")
        }
        val attributes = listOf("a" to "1", "b" to "2")
        val body = otlpRequest { metric("app.values") { gauge { number(NumberSpec(1.0, attributes = attributes)) } } }
        few.ingest(tenant, PROTOBUF, body).rejected shouldBe mapOf(IngestRejection.InvalidLabels to 1L)
        val narrow = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumBuckets = 1 }
            acceptAnyFamily("app.")
        }
        val histogram = HistogramSpec(count = 1L, sum = 1.0, buckets = listOf(1L, 0L, 0L), bounds = listOf(1.0))
        val wide = otlpRequest { metric(LATENCY) { histogram(CUMULATIVE, histogram) } }
        narrow.ingest(tenant, PROTOBUF, wide).verdict shouldBe IngestVerdict.Malformed
    }

    test("families, units and series beyond their limits are rejected") {
        val ingestion = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumSeries = 1 }
            acceptAnyFamily("app.")
        }
        val body = otlpRequest {
            metric("app.one") { gauge { number(NumberSpec(1.0)) } }
            metric("app.two") { gauge { number(NumberSpec(1.0)) } }
            metric(name = "app.parsec", unit = "pc") { gauge { number(NumberSpec(1.0)) } }
            metric("App.Bad") { gauge { number(NumberSpec(1.0)) } }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe mapOf(IngestRejection.UndeclaredFamily to THREE)
        val few = otlpIngestion(testClock()) {
            limits = ingestLimits { maximumSeries = 1 }
            acceptFamily(ITEMS, MeterKind.Gauge, MeasureUnit.NONE, "id")
        }
        val two = otlpRequest {
            metric(ITEMS) {
                gauge {
                    number(NumberSpec(1.0, attributes = listOf("id" to "1")))
                    number(NumberSpec(1.0, attributes = listOf("id" to "2")))
                }
            }
        }
        few.ingest(tenant, PROTOBUF, two).rejected shouldBe mapOf(IngestRejection.SeriesLimit to 1L)
    }

    test("histograms that restart keep their total and retire when their instance falls silent") {
        val clock = testClock()
        val ingestion = otlpIngestion(clock) { acceptFamily(LATENCY, MeterKind.Histogram, MeasureUnit.SECONDS) }
        val first = HistogramSpec(
            count = 2L,
            sum = 2.0,
            buckets = listOf(2L, 0L),
            bounds = listOf(1.0),
            time = NOW_NANOS - 2L,
        )
        val restarted = first.copy(count = 1L, sum = 1.0, buckets = listOf(1L, 0L), time = NOW_NANOS - 1L)
        ingestion.ingest(
            tenant,
            PROTOBUF,
            otlpRequest { metric(name = LATENCY, unit = "s") { histogram(CUMULATIVE, first) } },
        )
        ingestion.ingest(
            tenant,
            PROTOBUF,
            otlpRequest { metric(name = LATENCY, unit = "s") { histogram(CUMULATIVE, restarted) } },
        )
        ingestion.histogramPoint(LATENCY).count shouldBe THREE
        clock.advanceBy(IngestLimits.Default.instanceExpiry + 1.minutes)
        ingestion.histogramPoint(LATENCY).count shouldBe THREE
    }

    test("up-down sums are replaced and silent instances leave them") {
        val clock = testClock()
        val ingestion = otlpIngestion(clock) { acceptFamily(ITEMS, MeterKind.UpDownCounter) }
        val up = otlpRequest {
            metric(
                ITEMS,
            ) { sum(isMonotonic = false, temporality = CUMULATIVE) { number(NumberSpec(FIVE, time = NOW_NANOS - 1L)) } }
        }
        val down = otlpRequest {
            metric(
                ITEMS,
            ) { sum(isMonotonic = false, temporality = CUMULATIVE) { number(NumberSpec(1.0, start = 1L)) } }
        }
        ingestion.ingest(tenant, PROTOBUF, up)
        ingestion.ingest(tenant, PROTOBUF, down)
        ingestion.sums(ITEMS) shouldContainExactly listOf(1.0)
        clock.advanceBy(IngestLimits.Default.instanceExpiry + 1.minutes)
        ingestion.sums(ITEMS).shouldBeEmpty()
    }

    test("a delta that ends before it starts is out of order") {
        val ingestion = otlpIngestion(testClock()) { acceptFamily(ITEMS, MeterKind.Counter) }
        val body = otlpRequest {
            metric(
                ITEMS,
            ) {
                sum(
                    isMonotonic = true,
                    temporality = DELTA,
                ) { number(NumberSpec(1.0, time = NOW_NANOS, start = NOW_NANOS + 1L)) }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe mapOf(IngestRejection.OutOfOrder to 1L)
    }

    test("a declared family refuses points of another kind or keys") {
        val ingestion = otlpIngestion(testClock()) { acceptFamily(ITEMS, MeterKind.Counter) }
        val body = otlpRequest {
            metric(ITEMS) { gauge { number(NumberSpec(1.0)) } }
            metric(
                ITEMS,
            ) {
                sum(
                    isMonotonic = true,
                    temporality = CUMULATIVE,
                ) { number(NumberSpec(1.0, attributes = listOf("x" to "1"))) }
            }
        }
        ingestion.ingest(tenant, PROTOBUF, body).rejected shouldBe mapOf(IngestRejection.Mismatch to 2L)
        val open = otlpIngestion(testClock()) { acceptAnyFamily("app.") }
        val twice = listOf("b" to "1", "b" to "2")
        val repeated = otlpRequest { metric(ITEMS) { gauge { number(NumberSpec(1.0, attributes = twice)) } } }
        open.ingest(tenant, PROTOBUF, repeated).rejected shouldBe mapOf(IngestRejection.InvalidLabels to 1L)
    }
})

private const val THREE: Long = 3L
private const val FIVE: Double = 5.0
private const val KVLIST: Int = 6
