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

import com.davils.kollect.core.fixture.Breaker
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gaugeOf
import com.davils.kollect.core.fixture.histogramOf
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.health.rule.Statistic
import com.davils.kollect.core.health.rule.Undetermined
import com.davils.kollect.core.limit.healthLimits
import com.davils.kollect.core.orThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val ERRORS: String = "http.errors"
private const val LOAD: String = "cpu.load"
private const val HIGH: Double = 0.9
private const val WARN: Double = 0.7
private const val FEW: Long = 30L
private const val MISSING: String = "missing"
private const val LATENCY: String = "http.latency"
private const val WRONG: String = "wrong"
private const val BREAKER: String = "breaker"
private const val BREAKER_STATE: String = "breaker.state"
private const val ERRORS_IN_TWO_MINUTES: Long = 3_600L

private fun ProbeResponse.text(): String = body.copyBytes().decodeToString()

private fun HealthReport.statusOf(path: String): HealthStatus? = member(path)?.status

@OptIn(ExperimentalKotest::class)
class HealthTest : FunSpec({
    coroutineTestScope = true

    context("indicators") {
        test("report their health, and a failing or slow one is Down") {
            val registry = healthRegistry {
                indicator("up") { Health.Up }
                indicator("degraded") { Health.degraded() }
                indicator("failing") { error("broken") }
                indicator("slow", timeout = 1.seconds) {
                    delay(1.minutes)
                    Health.Up
                }
            }
            val report = registry.evaluate()
            report.statusOf("up") shouldBe HealthStatus.Up
            report.statusOf("degraded") shouldBe HealthStatus.Degraded
            report.member("failing")?.reasons shouldBe listOf(ReasonCode.CheckFailed)
            report.member("slow")?.reasons shouldBe listOf(ReasonCode.TimedOut)
            report.status shouldBe HealthStatus.Down
            report.member(MISSING).shouldBeNull()
            report.toString() shouldBe "HealthReport(Down, 4 members)"
        }
    }

    context("threshold rules") {
        test("decide by the worst series and fail closed without data") {
            val meters = testRegistry()
            val load = meters.gaugeOf(LOAD, "core")
            val registry = healthRegistry(meters) {
                threshold("load") {
                    meter = LOAD
                    above(degraded = WARN, down = HIGH)
                }
                threshold(MISSING) {
                    meter = "absent.meter"
                    below(down = 1.0)
                    undetermined = Undetermined.Degraded
                }
            }
            registry.evaluate().statusOf("load") shouldBe HealthStatus.Down
            load.with("a").set(WARN)
            registry.evaluate().statusOf("load") shouldBe HealthStatus.Degraded
            load.with("b").set(HIGH)
            registry.evaluate().statusOf("load") shouldBe HealthStatus.Down
            load.with("b").set(0.0)
            registry.evaluate().statusOf("load") shouldBe HealthStatus.Degraded
            registry.evaluate().member(MISSING)?.reasons shouldContainExactly listOf(ReasonCode.NoData)
            registry.evaluate().statusOf(MISSING) shouldBe HealthStatus.Degraded
        }

        test("treat a stale gauge and a gauge without points as undetermined") {
            val clock = testClock()
            val meters = testRegistry(clock)
            val load = meters.gaugeOf(LOAD).meter
            val registry = healthRegistry(meters, clock) {
                threshold("load") {
                    meter = LOAD
                    maximumAge = 1.minutes
                    above(down = HIGH)
                }
            }
            registry.evaluate().member("load")?.reasons shouldBe listOf(ReasonCode.NoData)
            load.set(0.0)
            registry.evaluate().statusOf("load") shouldBe HealthStatus.Up
            clock.advanceBy(2.minutes)
            registry.evaluate().member("load")?.reasons shouldBe listOf(ReasonCode.Stale)
        }

        test("read counts, means and rates") {
            val clock = testClock()
            val meters = testRegistry(clock)
            val errors = meters.counterOf(ERRORS).meter
            val latency = meters.histogramOf(LATENCY).meter
            val registry = healthRegistry(meters, clock) {
                threshold("rate") {
                    meter = ERRORS
                    statistic = Statistic.Rate(1.minutes)
                    above(down = 1.0)
                }
                threshold("count") {
                    meter = ERRORS
                    statistic = Statistic.Count
                    above(degraded = FEW.toDouble())
                }
                threshold("mean") {
                    meter = LATENCY
                    statistic = Statistic.Mean
                    above(down = HIGH)
                }
                threshold(WRONG) {
                    meter = LATENCY
                    above(down = 1.0)
                }
            }
            registry.evaluate().member("rate")?.reasons shouldBe listOf(ReasonCode.InsufficientSamples)
            registry.evaluate().member("mean")?.reasons shouldBe listOf(ReasonCode.InsufficientSamples)
            clock.advanceBy(2.minutes)
            errors.add(ERRORS_IN_TWO_MINUTES)
            latency.record(1.0)
            val report = registry.evaluate()
            report.statusOf("rate") shouldBe HealthStatus.Down
            report.statusOf("count") shouldBe HealthStatus.Degraded
            report.statusOf("mean") shouldBe HealthStatus.Down
            report.member(WRONG)?.reasons shouldBe listOf(ReasonCode.NoData)
            Statistic.Rate(1.minutes).toString() shouldBe "Rate(1m)"
            shouldThrow<IllegalArgumentException> { Statistic.Rate(0.milliseconds) }
        }
    }

    context("state rules") {
        test("decide by the active state") {
            val meters = testRegistry()
            val breaker = meters.stateSet(BREAKER_STATE, Breaker.Closed).orThrow().meter
            val registry = healthRegistry(meters) {
                state(BREAKER) {
                    meter = BREAKER_STATE
                    down("Open")
                    degraded("HalfOpen")
                }
                state(WRONG) { meter = LOAD }
            }
            registry.evaluate().statusOf(BREAKER) shouldBe HealthStatus.Up
            breaker.set(Breaker.HalfOpen)
            registry.evaluate().statusOf(BREAKER) shouldBe HealthStatus.Degraded
            breaker.set(Breaker.Open)
            registry.evaluate().member(BREAKER)?.reasons shouldBe listOf(ReasonCode.StateMatched)
            meters.gaugeOf(LOAD).meter.set(1.0)
            registry.evaluate().member(WRONG)?.reasons shouldBe listOf(ReasonCode.NoData)
        }
    }

    context("groups") {
        test("are as healthy as their worst member within its impact") {
            val registry = healthRegistry {
                group("database") {
                    indicator("pool") { Health.Up }
                    indicator("replica", impact = Impact.Degrading) { Health.down() }
                }
                group("empty") { }
            }
            val report = registry.evaluate()
            report.statusOf("database") shouldBe HealthStatus.Degraded
            report.statusOf("database.replica") shouldBe HealthStatus.Down
            report.member("empty")?.reasons shouldBe listOf(ReasonCode.EmptyGroup)
            healthRegistry { }.evaluate().status shouldBe HealthStatus.Down
        }
    }

    context("building") {
        test("refuses broken declarations") {
            shouldThrow<IllegalArgumentException> { healthRegistry { indicator("Bad") { Health.Up } } }
            shouldThrow<IllegalArgumentException> {
                healthRegistry {
                    indicator("twice") { Health.Up }
                    indicator("twice") { Health.Up }
                }
            }
            shouldThrow<IllegalArgumentException> {
                healthRegistry {
                    indicator(
                        "slow",
                        timeout = 1.minutes,
                    ) { Health.Up }
                }
            }
            shouldThrow<IllegalArgumentException> { healthRegistry { threshold("rule") { meter = LOAD } } }
            shouldThrow<IllegalArgumentException> {
                healthRegistry(
                    testRegistry(),
                ) { threshold("rule") { above(down = HIGH) } }
            }
            shouldThrow<IllegalArgumentException> {
                healthRegistry(
                    testRegistry(),
                ) { threshold("rule") { meter = LOAD } }
            }
            shouldThrow<IllegalArgumentException> {
                healthRegistry {
                    limits = healthLimits { maximumDepth = 1 }
                    group("a") { group("b") { } }
                }
            }
            shouldThrow<IllegalArgumentException> {
                healthRegistry {
                    limits = healthLimits { maximumMembers = 1 }
                    indicator("a") { Health.Up }
                    indicator("b") { Health.Up }
                }
            }
            shouldThrow<IllegalArgumentException> {
                healthRegistry {
                    indicator("a") { Health.Up }
                    limits = healthLimits { maximumMembers = 1 }
                }
            }
        }

        test("shows its parts") {
            healthRegistry { }.toString() shouldContain "HealthLimits"
            Health.down(ReasonCode.TimedOut).toString() shouldBe "Down([TimedOut])"
            Health.Up shouldBe Health.Up
            Health.down() shouldBe Health.down(ReasonCode.Reported)
            Health.down().hashCode() shouldBe Health.down(ReasonCode.Reported).hashCode()
            Health.Up.equals("up") shouldBe false
            MemberReport("a", HealthStatus.Up, emptyList()).toString() shouldBe "a=Up([])"
            MemberReport("a", HealthStatus.Up, emptyList()) shouldBe MemberReport("a", HealthStatus.Up, emptyList())
            MemberReport(
                "a",
                HealthStatus.Up,
                emptyList(),
            ).hashCode() shouldBe MemberReport("a", HealthStatus.Up, emptyList()).hashCode()
            MemberReport("a", HealthStatus.Up, emptyList()).equals("a") shouldBe false
        }
    }

    context("probes") {
        test("tell the status, and the checks only when asked") {
            val report = healthRegistry {
                indicator("pool") { Health.down(ReasonCode.TimedOut) }
            }.evaluate()
            val quiet = ProbeRenderer().ready(report)
            quiet.statusCode shouldBe UNAVAILABLE
            quiet.text() shouldBe """{"status":"fail"}"""
            quiet.body.contentType shouldBe "application/health+json"
            val detailed = ProbeRenderer(ProbeDetail.Checks).ready(report)
            detailed.text() shouldBe """{"status":"fail","checks":{"pool":[{"status":"fail","output":"TimedOut"}]}}"""
            val live = ProbeRenderer().live()
            live.text() shouldBe """{"status":"pass"}"""
            val degraded = healthRegistry { indicator("a") { Health.degraded() } }.evaluate()
            ProbeRenderer().ready(degraded).statusCode shouldBe OK
            ProbeRenderer().toString() shouldBe "ProbeRenderer(StatusOnly)"
            live.toString() shouldContain "ProbeResponse(200"
        }
    }
})

private const val UNAVAILABLE: Int = 503
private const val OK: Int = 200
