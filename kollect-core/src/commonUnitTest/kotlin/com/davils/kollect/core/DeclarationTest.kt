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

import com.davils.kollect.core.fixture.Mode
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.firstLabels
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.refusal
import com.davils.kollect.core.fixture.refusedAs
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.fixture.userFamilies
import com.davils.kollect.core.limit.RegistryLimits
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.SumPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val REQUESTS: String = "http.requests"
private const val METHOD: String = "method"
private const val SMALL: Int = 3
private const val MANY: Int = 200
private const val OWN_FAMILIES: Int = 5
private const val INVALID: String = "Bad"

class DeclarationTest : FunSpec({
    context("declaring a family") {
        test("returns the existing family for an equal declaration") {
            val registry = testRegistry()
            val first = registry.counter(REQUESTS) { labelKeys(METHOD) }
            val second = registry.counter(REQUESTS) { labelKeys(METHOD) }
            first.shouldBeInstanceOf<Registration.Declared<*>>()
            second.shouldBeInstanceOf<Registration.Existing<*>>().family shouldBeSameInstanceAs first.family
            first.toString() shouldBe "Declared"
            second.toString() shouldBe "Existing"
        }

        test("refuses a conflicting declaration") {
            val registry = testRegistry()
            registry.counterOf(REQUESTS)
            val conflict = registry.gauge(REQUESTS).refusedAs<Refusal.Conflict>()
            conflict.existing.name.value shouldBe REQUESTS
            registry.gauge("http_requests").refusedAs<Refusal.Conflict>()
            registry.observedGauge(REQUESTS) { }.refusedAs<Refusal.Conflict>()
        }

        test("refuses invalid names, keys, descriptions, bounds and states") {
            val registry = testRegistry { limits = registryLimits { maximumLabels = 2 } }
            val tooManyBounds = List(RegistryLimits.Default.maximumBuckets + 1) { index -> index.toDouble() }
            val invalid = listOf(
                registry.counter("Http"),
                registry.counter("http.requests_total"),
                registry.counter(REQUESTS) { labelKeys("le") },
                registry.counter(REQUESTS) { labelKeys("Method") },
                registry.counter(REQUESTS) { labelKeys("a", "b", "c") },
                registry.counter(REQUESTS) { labelKeys("a.b", "a_b") },
                registry.counter(REQUESTS) { description = "line\nbreak" },
                registry.histogram(REQUESTS) { bounds = ExplicitBounds.of(tooManyBounds) },
                registry.stateSet("mode", Mode.Idle) { labelKeys("mode") },
                registry.longTaskTimer("a".repeat(RegistryLimits.Default.maximumNameLength - 1)),
            )
            invalid.forEach { registration -> registration.refusedAs<Refusal.Invalid>() }
            val first = invalid.first()
            first.family.descriptor.shouldBeNull()
            first.refusal().toString() shouldContain "Invalid("
            registry.observedGauge(INVALID) { }.refusedAs<Refusal.Invalid>()
        }

        test("refuses families beyond the limit") {
            val registry = testRegistry { limits = registryLimits { maximumFamilies = OWN_FAMILIES + 1 } }
            registry.counterOf("first")
            val refusal = registry.counter("second").refusedAs<Refusal.LimitReached>()
            refusal.limit shouldBe "maximumFamilies"
            refusal.toString() shouldBe "LimitReached(maximumFamilies)"
        }

        test("throws for a refused declaration on demand") {
            val registry = testRegistry()
            val exception = shouldThrow<IllegalStateException> { registry.counter(INVALID).orThrow() }
            exception.message shouldContain "refused"
        }

        test("counts every refusal by reason") {
            val registry = testRegistry()
            registry.counter(INVALID)
            val refused = registry.points("kollect.declarations.refused")
            val invalid = refused.first { point -> point.labels.values.single() == "invalid" }
            invalid.shouldBeInstanceOf<SumPoint>().value shouldBe 1.0
        }
    }

    context("a family") {
        test("rejects label values of the wrong number, too long or with control characters") {
            val registry = testRegistry { limits = registryLimits { maximumLabelValueLength = SMALL } }
            val family = registry.counterOf(REQUESTS, METHOD)
            family.with().increment()
            family.with("GET", "POST").increment()
            family.with("DELETE").increment()
            family.with("A\u0000").increment()
            family.with("A\uD800").increment()
            family.size shouldBe 0
            registry.points(REQUESTS).shouldBeEmpty()
        }

        test("records new series beyond its limit in its overflow series") {
            val registry = testRegistry { limits = registryLimits { maximumSeriesPerFamily = SMALL } }
            val family = registry.counterOf(REQUESTS, METHOD)
            listOf("A", "B", "C", "D").forEach { method -> family.with(method).increment() }
            family.with("A").increment()
            val points = registry.points(REQUESTS)
            points.firstLabels() shouldContainExactly listOf("A", "B", "true")
            val overflow = points.last().labels
            overflow.keys.single() shouldBe LabelKey.OVERFLOW
            registry.sums(REQUESTS) shouldContainExactly listOf(2.0, 1.0, 2.0)
            family.size shouldBe SMALL
        }

        test("overflows once the whole registry holds its limit of series") {
            val registry = testRegistry { limits = registryLimits { maximumSeries = SMALL } }
            val family = registry.counterOf(REQUESTS, METHOD)
            listOf("A", "B", "C").forEach { method -> family.with(method).increment() }
            registry.counter("second").refusedAs<Refusal.LimitReached>().limit shouldBe "maximumSeries"
            registry.points(REQUESTS).last().labels shouldBe Labels.OVERFLOW
        }

        test("frees the place of a removed series") {
            val registry = testRegistry { limits = registryLimits { maximumSeriesPerFamily = SMALL } }
            val family = registry.counterOf(REQUESTS, METHOD)
            family.with("A").increment()
            family.with("B").increment()
            family.remove("A") shouldBe true
            family.remove("A") shouldBe false
            family.remove("A", "B") shouldBe false
            family.with("C").increment()
            registry.points(REQUESTS).firstLabels() shouldContainExactly listOf("B", "C")
            registry.counterOf("single").remove() shouldBe false
        }

        test("stays correct after many removals") {
            val registry = testRegistry()
            val family = registry.counterOf(REQUESTS, METHOD)
            repeat(MANY) { index ->
                family.with("m$index").increment()
                family.remove("m$index")
            }
            family.with("kept").increment()
            registry.points(REQUESTS).firstLabels() shouldContainExactly listOf("kept")
        }

        test("shows its name and size") {
            val registry = testRegistry()
            registry.counterOf(REQUESTS).toString() shouldBe "Family($REQUESTS, 1 series)"
            registry.counter(INVALID).family.toString() shouldBe "Family(refused, 0 series)"
        }
    }

    context("removing a family") {
        test("drops it with every series") {
            val registry = testRegistry()
            registry.counterOf(REQUESTS).meter.increment()
            registry.remove(MetricName.of(REQUESTS)) shouldBe true
            registry.remove(MetricName.of(REQUESTS)) shouldBe false
            registry.points(REQUESTS).shouldBeEmpty()
            registry.counter(REQUESTS).shouldBeInstanceOf<Registration.Declared<*>>()
        }

        test("keeps the registry correct after many removals") {
            val registry = testRegistry()
            repeat(MANY) { index ->
                registry.counterOf("family.f$index")
                registry.remove(MetricName.of("family.f$index"))
            }
            registry.counterOf("kept").meter.increment()
            val names = registry.snapshot().userFamilies.map { family -> family.descriptor.name.value }
            names shouldContainExactly listOf("kept")
        }
    }

    context("closing the registry") {
        test("refuses every later declaration and drops the families") {
            val registry = testRegistry()
            registry.counterOf(REQUESTS)
            registry.close()
            registry.counter("later").refusal() shouldBe Refusal.Closed
            registry.observedGauge("later.observed") { }.refusal() shouldBe Refusal.Closed
            registry.snapshot().families.shouldBeEmpty()
            Refusal.Closed.label shouldBe "closed"
        }
    }

    context("the registry") {
        test("shows its size and reports its own families") {
            val registry = testRegistry()
            registry.counterOf(REQUESTS)
            registry.toString() shouldContain "families"
            val families = registry.points("kollect.registry.families")
            families.single().shouldBeInstanceOf<GaugePoint>()
            registry.limits shouldBe RegistryLimits.Default
        }

        test("uses the unit to name a family in Prometheus") {
            val registry = testRegistry()
            registry.gauge("disk.free") { unit = MeasureUnit.BYTES }.orThrow()
            registry.gauge("disk_free_bytes").refusedAs<Refusal.Conflict>()
        }
    }
})
