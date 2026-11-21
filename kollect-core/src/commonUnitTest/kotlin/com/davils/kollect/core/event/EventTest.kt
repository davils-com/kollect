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

package com.davils.kollect.core.event

import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.sumsOf
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthStatus
import com.davils.kollect.core.health.healthRegistry
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.Unreadability
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.testing.testEventBus
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext

private const val REQUESTS: String = "http.requests"
private const val PUBLISHED: Long = 7L

private suspend fun withBus(block: suspend (EventBus) -> Unit) {
    val scope = CoroutineScope(coroutineContext + Job())
    try {
        block(testEventBus(scope))
    } finally {
        scope.cancel()
    }
}

@OptIn(ExperimentalKotest::class)
class EventTest : FunSpec({
    coroutineTestScope = true

    test("a registry publishes what happens to its families") {
        withBus { bus ->
            val registry = testRegistry {
                limits = registryLimits { maximumSeriesPerFamily = 2 }
                events(bus)
            }
            registry.counter("Bad")
            val family = registry.counterOf(REQUESTS, "method")
            family.with("a").increment()
            family.with("b").increment()
            family.with("c").add(-1L)
            registry.observedGauge("broken") { error("broken") }
            registry.snapshot()
            registry.remove(MetricName.of(REQUESTS))
            bus[MetricTopics.REGISTRY].metrics.published shouldBeGreaterThan PUBLISHED
        }
    }

    test("a registry counts the events a closed bus rejects") {
        withBus { bus ->
            val registry = testRegistry { events(bus) }
            bus.abort()
            registry.counter("Bad")
            val rejected = registry.snapshot().sumsOf("kollect.events.rejected")
            rejected.single().toLong() shouldBeGreaterThan 0L
        }
    }

    test("a health registry publishes changes of a status") {
        withBus { bus ->
            var health = Health.Up
            val registry = healthRegistry {
                events(bus)
                indicator("pool") { health }
            }
            registry.evaluate()
            registry.evaluate()
            health = Health.down()
            registry.evaluate()
            bus[MetricTopics.HEALTH].metrics.published shouldBe 2L
        }
    }

    test("events show themselves") {
        val name = MetricName.of(REQUESTS)
        listOf(
            RegistryEvent.FamilyRemoved(name),
            RegistryEvent.FamilyOverflowed(name),
            RegistryEvent.MeasurementsRejected(Rejection.Negative, 2L),
            RegistryEvent.FamilyUnreadable(name, Unreadability.Stalled),
            HealthEvent.StatusChanged(path = null, from = HealthStatus.Up, to = HealthStatus.Down),
            HealthEvent.StatusChanged(path = "pool", from = HealthStatus.Up, to = HealthStatus.Down),
        ).map(Any::toString) shouldContainExactly listOf(
            "FamilyRemoved($REQUESTS)",
            "FamilyOverflowed($REQUESTS)",
            "MeasurementsRejected(Negative, 2)",
            "FamilyUnreadable($REQUESTS, Stalled)",
            "StatusChanged(registry, Up -> Down)",
            "StatusChanged(pool, Up -> Down)",
        )
        MetricTopics.HEALTH.name.value shouldBe "kollect.health"
    }
})
