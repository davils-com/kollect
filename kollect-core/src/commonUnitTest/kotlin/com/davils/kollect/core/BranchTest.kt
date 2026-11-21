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

package com.davils.kollect.core

import com.davils.kollect.core.export.Delivery
import com.davils.kollect.core.export.OtlpResponses
import com.davils.kollect.core.fixture.Breaker
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.histogramOf
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.fixture.withoutOwnFamilies
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.health.ProbeRenderer
import com.davils.kollect.core.health.healthRegistry
import com.davils.kollect.core.internal.health.Json
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.limit.RegistryLimits
import com.davils.kollect.core.limit.exportLimits
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.otlp.encodeOtlp
import com.davils.kollect.core.otlp.resource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration

private const val CONTINUE: Int = 100
private const val MANY: Int = 20
private const val FAMILIES: Int = 3

@OptIn(ExperimentalKotest::class)
class BranchTest : FunSpec({
    coroutineTestScope = true

    test("JSON escapes quotes, backslashes and controls") {
        val text = listOf('a', '"', 'b', '\\', 'c', '\n', 'd', '\r', 'e', '\t', 'f', '\u0001').joinToString(
            separator = "",
        )
        val escaped = """a\"b\\c\nd\re\tf\u0001"""
        Json.string(text) shouldBe "${'"'}$escaped${'"'}"
    }

    test("health builders refuse a timeout of zero, thresholds that are not finite and rules without meters") {
        shouldThrow<IllegalArgumentException> {
            healthRegistry {
                indicator(
                    "a",
                    timeout = Duration.ZERO,
                ) { Health.Up }
            }
        }
        shouldThrow<IllegalArgumentException> {
            healthRegistry(testRegistry()) {
                threshold("a") {
                    meter = "x"
                    above(down = Double.NaN)
                }
            }
        }
        shouldThrow<IllegalArgumentException> { healthRegistry { state("a") { meter = "x" } } }
        val up = healthRegistry { indicator("a") { Health.Up } }.evaluate()
        up.status shouldBe HealthStatus.Up
        val body = ProbeRenderer().ready(up).body
        body.copyBytes().decodeToString() shouldBe """{"status":"pass"}"""
    }

    test("small rules of text and answers") {
        OtlpResponses.deliveryOf(CONTINUE) shouldBe Delivery.Rejected
        resource("shop")["missing"].shouldBeNull()
        TextRules.firstInvalidIndex("\uD800a") shouldBe 0
    }

    test("a sink kept beyond its callback takes nothing") {
        val registry = testRegistry()
        var kept: ObservationSink? = null
        registry.observedGauge("pool.size") { kept = this }
        registry.snapshot()
        kept?.observe(1.0)
        registry.points("pool.size").shouldBeEmpty()
    }

    test("declarations beyond the states, key length or of another variant are refused") {
        val registry = testRegistry { limits = registryLimits { maximumStates = 1 } }
        registry.stateSet("lever", Breaker.Closed).shouldBeInstanceOf<Registration.Refused<*>>()
        val longKey = "k".repeat(RegistryLimits.Default.maximumNameLength + 1)
        val longKeyed = registry.counter("a") { labelKeys(longKey) }
        longKeyed.shouldBeInstanceOf<Registration.Refused<*>>()
        registry.timer("job.duration").orThrow()
        val other = registry.histogram("job.duration") {
            unit = MeasureUnit.SECONDS
            bounds = ExplicitBounds.SECONDS
        }
        other.shouldBeInstanceOf<Registration.Refused<*>>().refusal.shouldBeInstanceOf<Refusal.Conflict>()
    }

    test("an encoder splits by bytes and writes histograms without values") {
        val registry = testRegistry()
        registry.histogramOf("app.latency")
        repeat(FAMILIES) { familyIndex ->
            val family = registry.counterOf("app.requests.r$familyIndex", "method")
            repeat(MANY) { index -> family.with("method-number-$index").increment() }
        }
        val batch = encodeOtlp(
            registry.snapshot().withoutOwnFamilies(),
            resource("shop"),
            exportLimits { maximumPayloadBytes = SPLIT_BYTES },
        )
        batch.payloads.size shouldBeGreaterThan 1
    }
})

private const val SPLIT_BYTES: Int = 2_048
