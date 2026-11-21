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

package com.davils.kollect.core.binder

import com.davils.kollect.core.fixture.OrderPlaced
import com.davils.kollect.core.fixture.labelValues
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testRegistry
import com.davils.konfetti.core.TopicKey
import com.davils.konfetti.testing.testEventBus
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.coroutines.testScheduler
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext

private const val ORDERS: String = "shop.orders"
private const val PUBLISHED: String = "konfetti.topic.published"

private val orders: TopicKey<OrderPlaced> = TopicKey(ORDERS)

@OptIn(ExperimentalKotest::class, ExperimentalCoroutinesApi::class)
class BinderTest : FunSpec({
    coroutineTestScope = true

    test("the bus metrics report the topics and consumers of a bus") {
        val scope = CoroutineScope(coroutineContext + Job())
        val bus = testEventBus(scope) { topic(orders) }
        bus[orders].consume("shop.billing") { handle { } }
        bus[orders].offer(OrderPlaced())
        val registry = testRegistry()
        registry.bindBusMetrics(bus).use { binding ->
            registry.sums(PUBLISHED) shouldContain 1.0
            registry.points("konfetti.consumer.received").labelValues() shouldContainExactly
                listOf(listOf(ORDERS, "shop.billing"))
            registry.sums("konfetti.bus.observer_failures") shouldContainExactly listOf(0.0)
            registry.sums(PUBLISHED) shouldContain 1.0
            binding.toString() shouldBe "BusMetrics(16 families)"
        }
        registry.points(PUBLISHED).shouldBeEmpty()
        scope.cancel()
    }

    test("a metered dispatcher measures the tasks it runs") {
        val registry = testRegistry()
        val delegate: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
        val metered = delegate.metered(registry, "test")
        withContext(metered) { }
        val scope = CoroutineScope(coroutineContext + Job())
        val done = CompletableDeferred<Unit>()
        scope.launch(metered) { done.complete(Unit) }
        done.await()
        metered.toString() shouldContain "MeteredDispatcher"
        registry.sums("coroutine.dispatcher.completed") shouldContainExactly listOf(2.0)
        scope.cancel()
    }

    test("a job monitor classifies how jobs end") {
        val registry = testRegistry()
        val monitor = registry.jobMonitor("imports")
        val scope = CoroutineScope(coroutineContext + Job())
        monitor.watch(scope.launch { }).join()
        val waiting = monitor.watch(scope.launch { CompletableDeferred<Unit>().await() })
        waiting.cancel()
        waiting.join()
        val failing = CoroutineScope(coroutineContext + SupervisorJob())
        val silent = CoroutineExceptionHandler { _, _ -> }
        monitor.watch(failing.launch(silent) { error("failed") }).join()
        val outcomes = registry.points("coroutine.jobs.ended").map { point -> point.labels.values[1] }
        outcomes shouldContainExactly listOf("cancelled", "completed", "failed")
        monitor.toString() shouldBe "JobMonitor(imports)"
        scope.cancel()
        failing.cancel()
    }
})
