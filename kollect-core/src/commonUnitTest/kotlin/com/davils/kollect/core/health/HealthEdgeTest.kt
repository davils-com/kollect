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

package com.davils.kollect.core.health

import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gaugeOf
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.health.rule.Statistic
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.internal.health.BuildState
import com.davils.kollect.core.limit.healthLimits
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

private const val ERROR_RULE: String = "errors"

private const val FREE: String = "disk.free"
private const val ERRORS: String = "app.errors"
private const val LOW: Double = 10.0
private const val CRITICAL: Double = 1.0

@OptIn(ExperimentalKotest::class)
class HealthEdgeTest : FunSpec({
    coroutineTestScope = true

    test("a rule below a threshold warns about values that are too low") {
        val meters = testRegistry()
        val free = meters.gaugeOf(FREE).meter
        val registry = healthRegistry(meters) {
            threshold("disk") {
                meter = FREE
                below(degraded = LOW, down = CRITICAL)
            }
        }
        free.set(LOW * 2)
        registry.evaluate().member("disk")?.status shouldBe HealthStatus.Up
        free.set(LOW)
        registry.evaluate().member("disk")?.status shouldBe HealthStatus.Degraded
        free.set(0.0)
        registry.evaluate().member("disk")?.status shouldBe HealthStatus.Down
    }

    test("a rule over a family the snapshot cannot read is undetermined") {
        val meters = testRegistry()
        meters.observedGauge(FREE) { error("broken") }
        val registry = healthRegistry(meters) {
            threshold("disk") {
                meter = FREE
                undetermined = Undetermined.Degraded
                above(down = 1.0)
            }
        }
        registry.evaluate().member("disk")?.reasons shouldBe listOf(ReasonCode.Unreadable)
    }

    test("a rate starts anew when its counter restarts and keeps a bounded history") {
        val clock = testClock()
        val meters = testRegistry(clock)
        val errors = meters.counterOf(ERRORS, "kind")
        val registry = healthRegistry(meters, clock) {
            limits = healthLimits { maximumCheckpoints = 1 }
            threshold(ERROR_RULE) {
                meter = ERRORS
                statistic = Statistic.Rate(1.minutes)
                above(down = 1.0)
            }
            threshold("gauge_count") {
                meter = FREE
                statistic = Statistic.Count
                above(down = 1.0)
            }
        }
        errors.with("a").increment()
        meters.gaugeOf(FREE).meter.set(1.0)
        registry.evaluate()
        clock.advanceBy(2.minutes)
        registry.evaluate().member(ERROR_RULE)?.status shouldBe HealthStatus.Up
        errors.remove("a")
        errors.with("a").increment()
        registry.evaluate().member(ERROR_RULE)?.reasons shouldBe listOf(ReasonCode.InsufficientSamples)
        registry.evaluate().member("gauge_count")?.reasons shouldBe listOf(ReasonCode.NoData)
    }

    test("a degrading member that is up leaves its group up") {
        val registry = healthRegistry {
            indicator("cache", impact = Impact.Degrading) { Health.Up }
        }
        registry.evaluate().status shouldBe HealthStatus.Up
        val builder = HealthRegistryBuilder(BuildState(healthLimits { }, hasMeters = false))
        builder.limits shouldBe healthLimits { }
    }
})
