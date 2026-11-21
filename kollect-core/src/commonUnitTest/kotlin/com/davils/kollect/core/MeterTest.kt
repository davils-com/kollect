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

import com.davils.kollect.core.fixture.Breaker
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gauges
import com.davils.kollect.core.fixture.histogramOf
import com.davils.kollect.core.fixture.histogramPoint
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.meter.time
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.InfoPoint
import com.davils.kollect.core.snapshot.StateSetPoint
import com.davils.kollect.core.snapshot.SumPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val REQUESTS: String = "http.requests"
private const val REJECTED: String = "kollect.measurements.rejected"
private const val SIZE: String = "payload.size"
private const val TEMPERATURE: String = "room.temperature"
private const val DURATION: String = "job.duration"
private const val RUNNING: String = "migration.running"
private const val AMOUNT: Long = 5L
private const val CELSIUS: Double = 21.5
private const val LARGE: Double = 3.0
private const val HALF: Double = 0.5
private const val RECORDED: Long = 4L

private fun MeterRegistry.rejected(rejection: Rejection): Double {
    val point = points(REJECTED).first { candidate -> candidate.labels.values.single() == rejection.label }
    return point.shouldBeInstanceOf<SumPoint>().value
}

class MeterTest : FunSpec({
    context("a counter") {
        test("sums what its series add and rejects negative and too large amounts") {
            val registry = testRegistry()
            val counter = registry.counterOf(REQUESTS).meter
            counter.add(AMOUNT)
            counter.increment()
            counter.add(-1L)
            counter.add(Counter.MAXIMUM_INCREMENT + 1L)
            registry.sums(REQUESTS) shouldContainExactly listOf(AMOUNT + 1.0)
            registry.rejected(Rejection.Negative) shouldBe 1.0
            registry.rejected(Rejection.TooLarge) shouldBe 1.0
        }
    }

    context("an up-down counter") {
        test("grows and shrinks within the bound of an increment") {
            val registry = testRegistry()
            val counter = registry.upDownCounter("queue.items").orThrow().meter
            counter.add(AMOUNT)
            counter.increment()
            counter.decrement()
            counter.decrement()
            counter.add(-Counter.MAXIMUM_INCREMENT - 1L)
            registry.sums("queue.items") shouldContainExactly listOf(AMOUNT - 1.0)
            registry.rejected(Rejection.TooLarge) shouldBe 1.0
        }
    }

    context("a gauge") {
        test("reports its last finite value with the time it was set") {
            val clock = testClock()
            val registry = testRegistry(clock)
            val gauge = registry.gauge(TEMPERATURE) { unit = MeasureUnit.CELSIUS }.orThrow().meter
            registry.points(TEMPERATURE).shouldBeEmpty()
            gauge.set(CELSIUS)
            gauge.set(Double.NaN)
            val point = registry.points(TEMPERATURE).single()
            point.shouldBeInstanceOf<GaugePoint>()
            point.value shouldBe CELSIUS
            point.observedAt shouldBe clock.now()
            registry.rejected(Rejection.NotFinite) shouldBe 1.0
        }
    }

    context("a histogram") {
        test("counts a value into the bucket whose bound is at least the value") {
            val registry = testRegistry()
            val histogram = registry.histogram(SIZE) { bounds = ExplicitBounds.of(1.0, 2.0) }.orThrow().meter
            listOf(HALF, 1.0, 1.0 + HALF, LARGE).forEach(histogram::record)
            val point = registry.histogramPoint(SIZE)
            point.bucketCounts shouldContainExactly listOf(2L, 1L, 1L)
            point.count shouldBe RECORDED
            point.sum shouldBe HALF + 1.0 + 1.0 + HALF + LARGE
            point.min shouldBe HALF
            point.max shouldBe LARGE
        }

        test("stays cumulative and consistent across snapshots") {
            val registry = testRegistry()
            val histogram = registry.histogramOf(SIZE).meter
            registry.histogramPoint(SIZE).min.shouldBeNull()
            histogram.record(1.0)
            registry.snapshot()
            histogram.record(2.0)
            val point = registry.histogramPoint(SIZE)
            point.count shouldBe 2L
            point.bucketCounts.sum() shouldBe point.count
            point.sum shouldBe LARGE
        }

        test("rejects values that are not finite and negative values it does not allow") {
            val registry = testRegistry()
            val strict = registry.histogramOf(SIZE).meter
            val lenient = registry.histogram("temperature.delta") { isNegativeAllowed = true }.orThrow().meter
            strict.record(-1.0)
            strict.record(Double.POSITIVE_INFINITY)
            lenient.record(-1.0)
            registry.histogramPoint(SIZE).count shouldBe 0L
            registry.histogramPoint("temperature.delta").min shouldBe -1.0
            registry.rejected(Rejection.Negative) shouldBe 1.0
            registry.rejected(Rejection.NotFinite) shouldBe 1.0
        }
    }

    context("a timer") {
        test("records durations in seconds") {
            val clock = testClock()
            val registry = testRegistry(clock)
            val timer = registry.timer(DURATION).orThrow().meter
            timer.record(HALF.seconds)
            timer.time { clock.advanceBy(1.seconds) }
            shouldThrow<IllegalStateException> { timer.time { error("failed") } }
            timer.record(Duration.INFINITE)
            timer.record((-1).milliseconds)
            val point = registry.histogramPoint(DURATION)
            point.count shouldBe LARGE.toLong()
            point.sum shouldBe 1.0 + HALF
        }

        test("measures only seconds") {
            val registry = testRegistry()
            val refused = registry.timer(DURATION) { unit = MeasureUnit.BYTES }
            refused.shouldBeInstanceOf<Registration.Refused<*>>().refusal.shouldBeInstanceOf<Refusal.Invalid>()
        }
    }

    context("a long task timer") {
        test("reports the running tasks and their duration") {
            val clock = testClock()
            val registry = testRegistry(clock)
            val timer = registry.longTaskTimer(RUNNING).orThrow().meter
            timer.start().use {
                timer.start().use { clock.advanceBy(2.seconds) }
                registry.gauges(RUNNING) shouldContainExactly listOf(2.0)
                registry.gauges("$RUNNING.active") shouldContainExactly listOf(1.0)
            }
            registry.gauges("$RUNNING.active") shouldContainExactly listOf(0.0)
        }

        test("does not track tasks beyond its limit and closes a task once") {
            val registry = testRegistry { limits = registryLimits { maximumActiveTasks = 1 } }
            val timer = registry.longTaskTimer(RUNNING).orThrow().meter
            timer.start().use { task ->
                timer.start().use { }
                task.close()
            }
            registry.rejected(Rejection.Untracked) shouldBe 1.0
        }
    }

    context("a state set") {
        test("reports the active state") {
            val registry = testRegistry()
            val breaker = registry.stateSet("breaker.state", Breaker.Closed).orThrow()
            breaker.meter.set(Breaker.Open)
            val point = registry.points("breaker.state").single()
            point.shouldBeInstanceOf<StateSetPoint>().active shouldBe Breaker.Open.ordinal
        }
    }

    context("an info family") {
        test("carries its facts in its labels") {
            val registry = testRegistry()
            val build = registry.info("build") { labelKeys("version") }.orThrow()
            build.with("1.0.0")
            val point = registry.points("build").single()
            point.shouldBeInstanceOf<InfoPoint>()
            point.labels.values shouldContainExactly listOf("1.0.0")
        }
    }
})
