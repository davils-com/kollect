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

import com.davils.kollect.core.export.Delivery
import com.davils.kollect.core.export.OtlpResponses
import com.davils.kollect.core.fixture.HistogramSpec
import com.davils.kollect.core.fixture.NANOS_PER_MILLISECOND
import com.davils.kollect.core.fixture.NOW_NANOS
import com.davils.kollect.core.fixture.NumberSpec
import com.davils.kollect.core.fixture.gauge
import com.davils.kollect.core.fixture.gauges
import com.davils.kollect.core.fixture.histogram
import com.davils.kollect.core.fixture.histogramPoint
import com.davils.kollect.core.fixture.metric
import com.davils.kollect.core.fixture.number
import com.davils.kollect.core.fixture.otlpRequest
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sum
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.limit.ingestLimits
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kore.testing.TestClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val PROTOBUF: String = "application/x-protobuf"
private const val REQUESTS: String = "client.requests"
private const val TEMPERATURE: String = "client.temperature"
private const val DURATION: String = "client.duration"
private const val CUMULATIVE: Long = 2L
private const val DELTA: Long = 1L
private const val THREE: Double = 3.0
private const val TEN: Double = 10.0
private const val METHOD: String = "method"
private val TRUNCATED: ByteArray = byteArrayOf(FIELD_ONE_LENGTH, LONG_LENGTH)

private val tenant: Source = Source.of("tenant-1")
private val other: Source = Source.of("tenant-2")

private fun ingestionOf(
    clock: TestClock = testClock(),
    configure: OtlpIngestionBuilder.() -> Unit = {
    },
): OtlpIngestion = otlpIngestion(clock) {
    acceptFamily(REQUESTS, MeterKind.Counter, MeasureUnit.NONE, METHOD)
    acceptFamily(TEMPERATURE, MeterKind.Gauge)
    acceptFamily(DURATION, MeterKind.Histogram, MeasureUnit.SECONDS)
    configure()
}

private fun counter(value: Double, temporality: Long = CUMULATIVE, spec: NumberSpec = NumberSpec(value)): ByteArray =
    otlpRequest {
        metric(
            REQUESTS,
        ) {
            sum(
                isMonotonic = true,
                temporality = temporality,
            ) { number(spec.copy(attributes = listOf(METHOD to "GET"))) }
        }
    }

private fun OtlpIngestion.send(body: ByteArray, source: Source = tenant): Ingestion = ingest(source, PROTOBUF, body)

class IngestionTest : FunSpec({
    context("a request") {
        test("of another content type, too large or malformed is refused whole") {
            val ingestion = ingestionOf { limits = ingestLimits { maximumPayloadBytes = SMALL_BODY } }
            ingestion.ingest(tenant, "application/json", counter(1.0)).verdict shouldBe IngestVerdict.UnsupportedContent
            ingestion.send(ByteArray(SMALL_BODY + 1)).verdict shouldBe IngestVerdict.TooLarge
            ingestion.send(TRUNCATED).verdict shouldBe IngestVerdict.Malformed
            val mixedCase = ingestionOf().ingest(tenant, "Application/X-Protobuf; charset=x", counter(1.0))
            mixedCase.verdict shouldBe IngestVerdict.Accepted
            IngestVerdict.Throttled.statusCode shouldBe TOO_MANY_REQUESTS
        }

        test("with more points than allowed is too large") {
            val ingestion = ingestionOf { limits = ingestLimits { maximumPointsPerRequest = 1 } }
            val body = otlpRequest {
                metric(TEMPERATURE) {
                    gauge {
                        number(NumberSpec(1.0))
                        number(NumberSpec(2.0))
                    }
                }
            }
            ingestion.send(body).verdict shouldBe IngestVerdict.TooLarge
        }

        test("of a metric with two types is malformed") {
            val body = otlpRequest {
                metric(TEMPERATURE) {
                    gauge { number(NumberSpec(1.0)) }
                    gauge { number(NumberSpec(1.0)) }
                }
            }
            ingestionOf().send(body).verdict shouldBe IngestVerdict.Malformed
            ingestionOf().send(otlpRequest { metric(TEMPERATURE) { } }).verdict shouldBe IngestVerdict.Malformed
        }

        test("beyond the rate of its source or the number of sources is throttled") {
            val clock = testClock()
            val slow = ingestionOf(clock) { limits = ingestLimits { maximumPointsPerMinute = 1 } }
            slow.send(counter(1.0)).verdict shouldBe IngestVerdict.Accepted
            slow.send(counter(2.0)).verdict shouldBe IngestVerdict.Throttled
            clock.advanceBy(1.minutes)
            val later = counter(2.0, spec = NumberSpec(2.0, time = NOW_NANOS + 1L))
            slow.send(later).verdict shouldBe IngestVerdict.Accepted
            val few = ingestionOf { limits = ingestLimits { maximumSources = 1 } }
            few.send(counter(1.0)).verdict shouldBe IngestVerdict.Accepted
            few.send(counter(1.0), other).verdict shouldBe IngestVerdict.Throttled
        }

        test("is answered with its partial success") {
            val result = ingestionOf().send(otlpRequest { metric("unknown") { gauge { number(NumberSpec(1.0)) } } })
            result.verdict shouldBe IngestVerdict.PartiallyAccepted
            val delivery = OtlpResponses.deliveryOf(result.verdict.statusCode, result.response.copyBytes())
            delivery.shouldBeInstanceOf<Delivery.Accepted>().rejectedPoints shouldBe 1L
            result.toString() shouldContain "UndeclaredFamily"
        }
    }

    context("a point") {
        test("is rejected for every reason it can be") {
            val ingestion = ingestionOf { limits = ingestLimits { maximumSeriesPerSource = 2 } }
            val body = otlpRequest {
                metric(name = TEMPERATURE, unit = "s") { gauge { number(NumberSpec(1.0)) } }
                metric(TEMPERATURE) { gauge { number(NumberSpec(1.0, attributes = listOf("kollect.source" to "x"))) } }
                metric(TEMPERATURE) { gauge { number(NumberSpec(1.0, attributes = listOf("room" to "a"))) } }
                metric(TEMPERATURE) { gauge { number(NumberSpec(Double.NaN)) } }
                metric(TEMPERATURE) { gauge { number(NumberSpec(1.0, time = NOW_NANOS + 1.hours.inWholeNanoseconds)) } }
                metric(TEMPERATURE) { gauge { number(NumberSpec(1.0, time = 0L)) } }
                metric(
                    REQUESTS,
                ) {
                    sum(
                        isMonotonic = true,
                        temporality = CUMULATIVE,
                    ) { number(NumberSpec(-1.0, attributes = listOf(METHOD to "GET"))) }
                }
                metric(REQUESTS) { sum(isMonotonic = false, temporality = CUMULATIVE) { number(NumberSpec(1.0)) } }
                metric("client.summary") { message(SUMMARY) { } }
                metric(
                    DURATION,
                    unit = "s",
                ) {
                    histogram(
                        CUMULATIVE,
                        HistogramSpec(count = 2L, sum = 1.0, buckets = listOf(1L, 0L), bounds = listOf(1.0)),
                    )
                }
                metric(
                    DURATION,
                    unit = "s",
                ) {
                    histogram(
                        CUMULATIVE,
                        HistogramSpec(count = 1L, sum = 1.0, buckets = listOf(1L), bounds = listOf(1.0)),
                    )
                }
            }
            val result = ingestion.send(body)
            result.rejected shouldBe mapOf(
                IngestRejection.Mismatch to THREE.toLong(),
                IngestRejection.InvalidLabels to 1L,
                IngestRejection.InvalidValue to FOUR,
                IngestRejection.OutOfTime to 2L,
                IngestRejection.UnsupportedType to 1L,
            )
        }

        test("beyond the series of its source is rejected") {
            val ingestion = ingestionOf { limits = ingestLimits { maximumSeriesPerSource = 1 } }
            val body = otlpRequest {
                metric(REQUESTS) {
                    sum(isMonotonic = true, temporality = CUMULATIVE) {
                        number(NumberSpec(1.0, attributes = listOf(METHOD to "GET")))
                        number(NumberSpec(1.0, attributes = listOf("method" to "PUT")))
                    }
                }
            }
            ingestion.send(body).rejected shouldBe mapOf(IngestRejection.SeriesLimit to 1L)
        }

        test("without a recorded value changes nothing") {
            val ingestion = ingestionOf()
            val empty = counter(1.0, spec = NumberSpec(1.0, flags = OtlpFields.FLAG_NO_RECORDED_VALUE))
            ingestion.send(empty).accepted shouldBe 1L
            ingestion.points(REQUESTS).shouldBeEmpty()
        }
    }

    context("sums") {
        test("of deltas add up and a replayed delta is rejected") {
            val ingestion = ingestionOf()
            val start = NOW_NANOS - NANOS_PER_MILLISECOND
            ingestion.send(counter(1.0, DELTA, NumberSpec(1.0, time = start + 1L, start = start)))
            ingestion.send(counter(2.0, DELTA, NumberSpec(2.0, time = start + 2L, start = start + 1L)))
            val replayed = counter(2.0, DELTA, NumberSpec(2.0, time = start + 2L, start = start + 1L))
            ingestion.send(replayed).rejected shouldBe mapOf(IngestRejection.OutOfOrder to 1L)
            ingestion.sums(REQUESTS) shouldContainExactly listOf(THREE)
        }

        test("that restart keep the total monotonic and repeated points change nothing") {
            val ingestion = ingestionOf()
            ingestion.send(counter(TEN, spec = NumberSpec(TEN, time = NOW_NANOS - 2L)))
            ingestion.send(counter(TEN, spec = NumberSpec(TEN, time = NOW_NANOS - 2L)))
            ingestion.send(counter(1.0, spec = NumberSpec(1.0, time = NOW_NANOS - 1L, start = NOW_NANOS - 1L)))
            ingestion.sums(REQUESTS) shouldContainExactly listOf(TEN + 1.0)
        }

        test("of instances of one source are summed and stay when an instance falls silent") {
            val clock = testClock()
            val ingestion = ingestionOf(clock)
            ingestion.send(counter(1.0))
            ingestion.send(
                otlpRequest("instance-2") {
                    metric(
                        REQUESTS,
                    ) {
                        sum(
                            isMonotonic = true,
                            temporality = CUMULATIVE,
                        ) { number(NumberSpec(2.0, attributes = listOf(METHOD to "GET"))) }
                    }
                },
            )
            ingestion.sums(REQUESTS) shouldContainExactly listOf(THREE)
            clock.advanceBy(IngestLimits.Default.instanceExpiry + 1.minutes)
            ingestion.sums(REQUESTS) shouldContainExactly listOf(THREE)
            clock.advanceBy(IngestLimits.Default.seriesExpiry)
            ingestion.sums(REQUESTS).shouldBeEmpty()
            ingestion.sourceCount shouldBe 0
        }
    }

    context("gauges and histograms") {
        test("report the latest gauge and add up histograms") {
            val ingestion = ingestionOf()
            ingestion.send(otlpRequest { metric(TEMPERATURE) { gauge { number(NumberSpec(TEN)) } } })
            ingestion.send(
                otlpRequest(
                    "instance-2",
                ) { metric(TEMPERATURE) { gauge { number(NumberSpec(THREE, time = NOW_NANOS + 1L)) } } },
            )
            val histogram = HistogramSpec(count = 2L, sum = THREE, buckets = listOf(1L, 1L), bounds = listOf(1.0))
            ingestion.send(otlpRequest { metric(name = DURATION, unit = "s") { histogram(DELTA, histogram) } })
            ingestion.send(
                otlpRequest {
                    metric(
                        name = DURATION,
                        unit = "s",
                    ) { histogram(DELTA, histogram.copy(time = NOW_NANOS + 1L, start = NOW_NANOS)) }
                },
            )
            ingestion.gauges(TEMPERATURE) shouldContainExactly listOf(THREE)
            val point = ingestion.histogramPoint(DURATION)
            point.count shouldBe FOUR
            point.bucketCounts shouldContainExactly listOf(2L, 2L)
            point.min.shouldBeNull()
        }
    }

    context("the ingestion") {
        test("forgets a source on demand") {
            val ingestion = ingestionOf()
            ingestion.send(counter(1.0))
            ingestion.forget(tenant) shouldBe true
            ingestion.forget(tenant) shouldBe false
            ingestion.points(REQUESTS).shouldBeEmpty()
            ingestion.toString() shouldBe "OtlpIngestion(0 sources)"
        }

        test("opens a prefix to every family with the attributes of its first point") {
            val ingestion = otlpIngestion(testClock()) { acceptAnyFamily("app.") }
            val body = otlpRequest {
                metric("app.clicks") {
                    sum(
                        isMonotonic = true,
                        temporality = CUMULATIVE,
                    ) { number(NumberSpec(1.0, attributes = listOf("page" to "home"))) }
                }
                metric("app.views") { gauge { number(NumberSpec(1.0, attributes = listOf("le" to "x"))) } }
                metric("other.clicks") { gauge { number(NumberSpec(1.0)) } }
            }
            ingestion.send(body).rejected shouldBe
                mapOf(IngestRejection.UndeclaredFamily to 1L, IngestRejection.InvalidLabels to 1L)
            ingestion.sums("app.clicks") shouldContainExactly listOf(1.0)
        }

        test("refuses invalid declarations") {
            shouldThrow<IllegalArgumentException> { otlpIngestion { acceptFamily(REQUESTS, MeterKind.Info) } }
            shouldThrow<IllegalArgumentException> {
                otlpIngestion {
                    acceptFamily(
                        REQUESTS,
                        MeterKind.Counter,
                        MeasureUnit.NONE,
                        "le",
                    )
                }
            }
            shouldThrow<IllegalArgumentException> {
                otlpIngestion {
                    acceptFamily(
                        REQUESTS,
                        MeterKind.Counter,
                        MeasureUnit.NONE,
                        "a",
                        "a",
                    )
                }
            }
            shouldThrow<IllegalArgumentException> {
                otlpIngestion {
                    acceptFamily(REQUESTS, MeterKind.Counter)
                    acceptFamily(REQUESTS, MeterKind.Counter)
                }
            }
            shouldThrow<IllegalArgumentException> { otlpIngestion { acceptAnyFamily("") } }
        }
    }

    context("a source") {
        test("is a short name of safe characters") {
            Source.of("Tenant-1.a_b").id shouldBe "Tenant-1.a_b"
            Source.ofOrNull("").shouldBeNull()
            Source.ofOrNull("a b").shouldBeNull()
            Source.ofOrNull("x".repeat(SOURCE_LENGTH + 1)).shouldBeNull()
            shouldThrow<IllegalArgumentException> { Source.of("ä") }
            (tenant < other) shouldBe true
            tenant.toString() shouldBe "tenant-1"
        }
    }
})

private const val SMALL_BODY: Int = 64
private const val TOO_MANY_REQUESTS: Int = 429
private const val SUMMARY: Int = 11
private const val FOUR: Long = 4L
private const val SOURCE_LENGTH: Int = 128

private const val FIELD_ONE_LENGTH: Byte = 0x0A
private const val LONG_LENGTH: Byte = 0x7F
