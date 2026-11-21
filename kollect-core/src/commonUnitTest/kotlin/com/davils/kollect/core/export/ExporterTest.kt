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

package com.davils.kollect.core.export

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.filter.MeterFilter
import com.davils.kollect.core.fixture.ScriptedTransport
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.limit.exportLimits
import com.davils.kollect.core.otlp.Resource
import com.davils.kollect.core.otlp.resource
import com.davils.kollect.core.snapshot.Temporality
import com.davils.konfetti.core.bus.Entropy
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val REQUESTS: String = "app.requests"
private const val TINY: Int = 520

private val INTERVAL: Duration = 1.seconds
private val LONG: Duration = 1.hours
private val shop: Resource = resource("shop")

private fun ExportAccounting.isBalanced(): Boolean = produced == delivered + rejected + dropped.values.sum() + pending

private fun registryWithRequests(): MeterRegistry {
    val registry = testRegistry { filter(MeterFilter.denyPrefix("kollect.")) }
    registry.counterOf(REQUESTS).meter.increment()
    return registry
}

private fun CoroutineScope.exporterOf(
    transport: Transport,
    registry: MeterRegistry = registryWithRequests(),
    configure: ExporterBuilder.() -> Unit = {},
): MetricExporter = metricExporter(
    scope = this,
    registry = registry,
    resource = shop,
    transport = transport,
    configure = configure,
)

private suspend fun withScope(block: suspend CoroutineScope.() -> Unit) {
    val scope = CoroutineScope(currentCoroutineContext() + Job())
    try {
        scope.block()
    } finally {
        scope.cancel()
    }
}

@OptIn(ExperimentalKotest::class)
class ExporterTest : FunSpec({
    coroutineTestScope = true

    test("sends a snapshot at every interval") {
        withScope {
            val transport = ScriptedTransport(listOf(Delivery.Accepted()))
            val events = mutableListOf<ExportEvent>()
            val exporter = exporterOf(transport) {
                interval = INTERVAL
                observer = ExportObserver { event -> events.add(event) }
            }
            delay(INTERVAL * 2 + 1.milliseconds)
            transport.sent.size shouldBe 2
            val accounting = exporter.accounting()
            accounting.delivered shouldBe 2L
            accounting.isBalanced() shouldBe true
            events.first().shouldBeInstanceOf<ExportEvent.Delivered>().points shouldBe 1L
            exporter.toString() shouldContain "delivered=2"
        }
    }

    test("retries with backoff until it is delivered") {
        withScope {
            val answers = listOf(Delivery.Retryable, Delivery.Throttled(INTERVAL), Delivery.Accepted(1L))
            val events = mutableListOf<ExportEvent>()
            val exporter = exporterOf(ScriptedTransport(answers)) {
                interval = LONG
                entropy = Entropy { Long.MAX_VALUE }
                observer = ExportObserver { event -> events.add(event) }
            }
            val accounting = exporter.flush(LONG)
            accounting.rejected shouldBe 1L
            accounting.delivered shouldBe 0L
            val kinds = events.map { event -> event::class.simpleName }
            kinds shouldContainExactly listOf("Retrying", "Retrying", "Delivered")
            events.first().toString() shouldContain "attempt=1"
        }
    }

    test("drops a request after its last attempt or a rejection") {
        withScope {
            val exhausted = exporterOf(ScriptedTransport(listOf(Delivery.Retryable))) {
                limits = exportLimits { maximumAttempts = 2 }
                entropy = Entropy { error("broken") }
                observer = ExportObserver { error("broken") }
            }
            exhausted.flush(LONG).dropped shouldBe mapOf(DropReason.RetriesExhausted to 1L)
            val rejected = exporterOf(ScriptedTransport(listOf(Delivery.Rejected)))
            rejected.flush(LONG).dropped shouldBe mapOf(DropReason.Rejected to 1L)
            val throwing = exporterOf(Transport { error("down") })
            throwing.flush(INTERVAL).isBalanced() shouldBe true
        }
    }

    test("replaces an unsent cumulative request with a newer one") {
        withScope {
            val exporter = exporterOf(ScriptedTransport(listOf(null, Delivery.Accepted()))) { interval = LONG }
            exporter.flush(INTERVAL).pending shouldBe 1L
            val accounting = exporter.flush(LONG)
            accounting.dropped shouldBe mapOf(DropReason.Superseded to 1L)
            accounting.isBalanced() shouldBe true
        }
    }

    test("sends deltas and drops the oldest request when the queue is full") {
        withScope {
            val exporter = exporterOf(ScriptedTransport(listOf(null))) {
                interval = LONG
                temporality = Temporality.Delta
                limits = exportLimits { maximumQueuedBatches = 1 }
            }
            exporter.flush(INTERVAL)
            exporter.flush(INTERVAL).dropped shouldBe mapOf(DropReason.QueueFull to 1L)
            exporter.shutdown(INTERVAL).dropped[DropReason.ShutdownDeadline] shouldBe 1L
        }
    }

    test("drops what never fits into a request") {
        withScope {
            val exporter = exporterOf(ScriptedTransport(listOf(Delivery.Accepted()))) {
                limits = exportLimits { maximumPayloadBytes = TINY }
            }
            exporter.flush(INTERVAL).dropped shouldBe mapOf(DropReason.TooLarge to 1L)
        }
    }
})
