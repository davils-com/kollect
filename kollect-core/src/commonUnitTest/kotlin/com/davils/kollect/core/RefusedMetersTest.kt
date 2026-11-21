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

import com.davils.kollect.core.event.RegistryEvent
import com.davils.kollect.core.export.Delivery
import com.davils.kollect.core.export.ExportEvent
import com.davils.kollect.core.fixture.Mode
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gauges
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.health.rule.Statistic
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.time
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private const val BAD: String = "Bad"
private const val AMOUNT: Long = 2L
private const val SEVEN: Int = 7
private const val HOURS_PER_DAY: Int = 24
private const val POOL: String = "pool.size"
private const val ITEM: String = "item"

class RefusedMetersTest : FunSpec({
    test("the meters of a refused family record nothing") {
        val registry = testRegistry()
        registry.counter(BAD).family.meter.apply {
            increment()
            add(1L)
        }
        registry.upDownCounter(BAD).family.meter.apply {
            increment()
            decrement()
            add(1L)
        }
        val gauge = registry.gauge(BAD).family
        gauge.meter.set(1.0)
        val histogram = registry.histogram(BAD).family
        histogram.meter.record(1.0)
        registry.timer(BAD).family.meter.apply {
            record(1.seconds)
            time { }
        }
        val longTaskTimer = registry.longTaskTimer(BAD).family
        longTaskTimer.meter.start().use { }
        val stateSet = registry.stateSet(BAD, Mode.Idle).family
        stateSet.meter.set(Mode.Idle)
        registry.info(BAD).family.with()
        registry.counter(BAD).family.remove() shouldBe false
        registry.counter(BAD).family.size shouldBe 0
    }

    test("meters keep their own arithmetic") {
        val registry = testRegistry()
        val counter = registry.upDownCounter("queue.items").orThrow().meter
        counter.add(Counter.MAXIMUM_INCREMENT + 1L)
        counter.add(AMOUNT)
        registry.sums("queue.items") shouldContainExactly listOf(AMOUNT.toDouble())
        val timer = registry.longTaskTimer("job.running").orThrow().meter
        timer.start().use { }
        timer.start().use { registry.gauges("job.running.active") shouldContainExactly listOf(1.0) }
        registry.info("build").orThrow()
        registry.points("build").size shouldBe 1
    }

    test("an equal observed declaration returns the existing family") {
        val registry = testRegistry()
        val first = registry.observedGauge(POOL) { observe(1.0) }
        val second = registry.observedGauge(POOL) { observe(2.0) }
        second.shouldBeInstanceOf<Registration.Existing<*>>().family shouldBe first.family
    }

    test("results and events show themselves") {
        val registry = testRegistry()
        registry.counterOf("a.b")
        registry.gauge("a.b").toString() shouldContain "Refused(Conflict("
        Refusal.Closed.toString() shouldBe "Closed"
        val statistics = listOf(Statistic.Value, Statistic.Count, Statistic.Mean)
        statistics.map(Any::toString) shouldContainExactly listOf("Value", "Count", "Mean")
        Delivery.Throttled(1.seconds).toString() shouldBe "Throttled(retryAfter=1s)"
        val delivered = ExportEvent.Delivered(points = 1L, rejectedPoints = 0L)
        delivered.toString() shouldBe "Delivered(points=1, rejectedPoints=0)"
        val descriptor = registry.counterOf("c").descriptor.shouldNotBeNull()
        RegistryEvent.FamilyDeclared(descriptor).toString() shouldBe "FamilyDeclared(c)"
        val refused = RegistryEvent.DeclarationRefused(BAD, Refusal.Denied)
        refused.toString() shouldBe """DeclarationRefused("Bad", Denied)"""
        MeasureUnit.annotation(ITEM).hashCode() shouldBe MeasureUnit.annotation(ITEM).hashCode()
        IngestLimits.HARD_INSTANCE_EXPIRY shouldBe 1.days
        IngestLimits.HARD_SERIES_EXPIRY shouldBe SEVEN.days
        IngestLimits.HARD_CLOCK_SKEW shouldBe 1.days / HOURS_PER_DAY
    }
})
