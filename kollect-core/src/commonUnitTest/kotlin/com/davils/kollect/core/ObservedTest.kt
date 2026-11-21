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

import com.davils.kollect.core.fixture.firstLabels
import com.davils.kollect.core.fixture.gauges
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.Unreadability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

private const val POOL: String = "pool.size"
private const val SENT: String = "bytes.sent"
private const val TEN: Double = 10.0
private const val THREE: Double = 3.0

class ObservedTest : FunSpec({
    test("an observed gauge reports what its callback observes at the snapshot") {
        val registry = testRegistry()
        var size = THREE
        registry.observedGauge(POOL, { labelKeys("pool") }) {
            observe(size, "main")
            observe(size + 1.0, "spare")
        }
        size = TEN
        registry.gauges(POOL) shouldContainExactly listOf(TEN, TEN + 1.0)
    }

    test("an observed counter starts its series anew when the sum falls") {
        val clock = testClock()
        val registry = testRegistry(clock)
        var sent = TEN
        registry.observedCounter(SENT) { observe(sent) }
        val first = registry.points(SENT).single().start
        clock.advanceBy(1.seconds)
        sent = TEN + 1.0
        registry.points(SENT).single().start shouldBe first
        sent = 1.0
        clock.advanceBy(1.seconds)
        registry.points(SENT).single().start shouldNotBe first
    }

    test("an observed up-down counter reports sums that fall") {
        val registry = testRegistry()
        registry.observedUpDownCounter("queue.items") { observe(-THREE) }
        registry.sums("queue.items") shouldContainExactly listOf(-THREE)
    }

    test("a callback that throws makes its family unreadable") {
        val registry = testRegistry()
        registry.observedGauge(POOL) { error("broken") }
        val snapshot = registry.snapshot()
        snapshot.family(MetricName.of(POOL)) shouldBe null
        val unreadable = snapshot.unreadable.single()
        unreadable.name shouldBe MetricName.of(POOL)
        unreadable.reason shouldBe Unreadability.CallbackFailed
        unreadable.toString() shouldBe "Unreadable($POOL, CallbackFailed)"
    }

    test("a callback may report each series once and only valid values") {
        val registry = testRegistry { limits = registryLimits { maximumSeriesPerFamily = 2 } }
        registry.observedCounter(SENT, { labelKeys("peer") }) {
            observe(1.0, "a")
            observe(2.0, "a")
            observe(-1.0, "b")
            observe(Double.NaN, "c")
            observe(1.0, "d", "e")
            observe(1.0, "d")
            observe(1.0, "f")
        }
        registry.points(SENT).firstLabels() shouldContainExactly listOf("a", "d")
    }

    test("closing an observed family removes it once") {
        val registry = testRegistry()
        val declared = registry.observedGauge(POOL) { observe(1.0) }
        declared.orThrow().use { family ->
            family.toString() shouldBe "ObservedFamily($POOL)"
            family.close()
        }
        registry.points(POOL).shouldBeEmpty()
        registry.observedGauge(POOL) { observe(1.0) }.shouldBeInstanceOf<Registration.Declared<*>>()
    }

    test("closing an outdated observed family keeps the family that replaced it") {
        val registry = testRegistry()
        val outdated = registry.observedGauge(POOL) { observe(1.0) }
        outdated.orThrow().use {
            registry.remove(MetricName.of(POOL))
            registry.observedGauge(POOL) { observe(2.0) }.orThrow()
        }
        registry.gauges(POOL) shouldContainExactly listOf(2.0)
    }

    test("a refused observed family shows itself and closes quietly") {
        val registry = testRegistry()
        registry.observedGauge("Bad") { observe(1.0) }.family.use { refused ->
            refused.descriptor shouldBe null
            refused.toString() shouldBe "ObservedFamily(refused)"
        }
    }
})
