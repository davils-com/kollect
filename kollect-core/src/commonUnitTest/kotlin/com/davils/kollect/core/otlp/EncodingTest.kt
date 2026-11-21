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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.fixture.Mode
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gaugesOf
import com.davils.kollect.core.fixture.histogramOf
import com.davils.kollect.core.fixture.sumsOf
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.fixture.text
import com.davils.kollect.core.fixture.withoutOwnFamilies
import com.davils.kollect.core.limit.exportLimits
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.orThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.milliseconds

private const val REQUESTS: String = "client.requests"
private const val DURATION: String = "client.duration"
private const val TEMPERATURE: String = "client.temperature"
private const val PROTOBUF: String = "application/x-protobuf"
private const val METHOD: String = "method"
private const val VERSION: String = "service.version"
private const val REGION: String = "cloud.region"
private const val SHOP: String = "shop"
private const val HALF: Long = 500L
private const val MANY: Int = 5
private const val LARGE_SPLIT: Int = 3
private const val TINY: Int = 600
private const val ATTRIBUTES: Int = 32

private fun MeterRegistry.clientSample() {
    val requests = counterOf(REQUESTS, METHOD)
    requests.with("GET").increment()
    val timer = timer(DURATION) { bounds = ExplicitBounds.of(1.0) }
    timer.orThrow().meter.record(HALF.milliseconds)
    val temperature = gauge(TEMPERATURE) { unit = MeasureUnit.CELSIUS }
    temperature.orThrow().meter.set(1.0)
    stateSet("client.mode", Mode.Busy).orThrow()
    val build = info("client.build") { labelKeys("version") }
    build.orThrow().with("1")
}

class EncodingTest : FunSpec({
    test("a snapshot travels through OTLP into the exposition of a server") {
        val clock = testClock()
        val registry = testRegistry(clock)
        registry.clientSample()
        val batch = encodeOtlp(registry.snapshot().withoutOwnFamilies(), resource(SHOP))
        val ingestion = otlpIngestion(clock) {
            acceptFamily(REQUESTS, MeterKind.Counter, MeasureUnit.NONE, METHOD)
            acceptFamily(DURATION, MeterKind.Histogram, MeasureUnit.SECONDS)
            acceptFamily(TEMPERATURE, MeterKind.Gauge, MeasureUnit.CELSIUS)
        }

        val result = ingestion.ingest(Source.of("tenant-1"), PROTOBUF, batch.payloads.single().copyBytes())

        result.verdict shouldBe IngestVerdict.PartiallyAccepted
        result.rejected shouldBe mapOf(IngestRejection.UndeclaredFamily to (Mode.entries.size + 1).toLong())
        val snapshot = ingestion.snapshot()
        snapshot.sumsOf(REQUESTS) shouldContainExactly listOf(1.0)
        snapshot.gaugesOf(TEMPERATURE) shouldContainExactly listOf(1.0)
        snapshot.histogramOf(DURATION).count shouldBe 1L
        snapshot.text() shouldContain """client_requests_total{kollect_source="tenant-1",method="GET"} 1"""
        batch.points shouldBe MANY + Mode.entries.size - 1
        batch.toString() shouldContain "1 requests"
    }

    test("a batch splits by points and drops what never fits") {
        val registry = testRegistry()
        val family = registry.counterOf(REQUESTS, METHOD)
        repeat(MANY) { index -> family.with("m$index").increment() }
        val snapshot = registry.snapshot().withoutOwnFamilies()
        val split = encodeOtlp(snapshot, resource(SHOP), exportLimits { maximumPointsPerPayload = 2 })
        split.payloads.size shouldBe LARGE_SPLIT
        val dropped = encodeOtlp(snapshot, resource(SHOP), exportLimits { maximumPayloadBytes = TINY })
        dropped.droppedPoints shouldBe MANY
        dropped.payloads.size shouldBe 0
    }

    context("a resource") {
        test("carries its own attributes and the allowed ones") {
            val resource = resource(SHOP) {
                attribute(key = VERSION, value = "1.0")
                allow(REGION)
                attribute(key = REGION, value = "eu")
            }
            resource["service.name"] shouldBe SHOP
            resource[REGION] shouldBe "eu"
            resource["telemetry.sdk.name"] shouldBe "kollect"
            resource.toString() shouldContain "service.name=\"shop\""
            resource shouldBe resource
            resource.hashCode() shouldBe resource.attributes.hashCode()
            resource.equals("resource") shouldBe false
        }

        test("refuses attributes that are not allowed or invalid") {
            shouldThrow<IllegalArgumentException> { resource("") }
            shouldThrow<IllegalArgumentException> { resource(SHOP) { attribute(key = "host.name", value = "a") } }
            shouldThrow<IllegalArgumentException> { resource(SHOP) { allow("service.name") } }
            shouldThrow<IllegalArgumentException> { resource(SHOP) { attribute(key = VERSION, value = "a\u0000") } }
            shouldThrow<IllegalArgumentException> {
                resource(SHOP) {
                    attribute(key = VERSION, value = "1")
                    attribute(key = VERSION, value = "2")
                }
            }
            shouldThrow<IllegalArgumentException> {
                resource(SHOP) {
                    repeat(ATTRIBUTES + 1) { index ->
                        allow("key.k$index")
                        attribute(key = "key.k$index", value = "v")
                    }
                }
            }
        }
    }
})
