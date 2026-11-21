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

package com.davils.kollect.core.internal.export

import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gaugeOf
import com.davils.kollect.core.fixture.histogramOf
import com.davils.kollect.core.fixture.sumsOf
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.fixture.withoutOwnFamilies
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.Temporality
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

private const val REQUESTS: String = "app.requests"
private const val LATENCY: String = "app.latency"
private const val AMOUNT: Long = 3L

class DeltaConverterTest : FunSpec({
    test("turns cumulative sums and histograms into deltas and keeps gauges") {
        val clock = testClock()
        val registry = testRegistry(clock)
        val requests = registry.counterOf(REQUESTS).meter
        val latency = registry.histogramOf(LATENCY).meter
        registry.gaugeOf("app.queue").meter.set(1.0)
        val converter = DeltaConverter()
        requests.add(AMOUNT)
        latency.record(1.0)
        val first = converter.convert(registry.snapshot().withoutOwnFamilies())
        first.sumsOf(REQUESTS) shouldContainExactly listOf(AMOUNT.toDouble())
        clock.advanceBy(1.seconds)
        requests.increment()
        latency.record(2.0)
        val delta = converter.convert(registry.snapshot().withoutOwnFamilies())
        delta.sumsOf(REQUESTS) shouldContainExactly listOf(1.0)
        val latencies = delta.family(MetricName.of(LATENCY)).shouldNotBeNull()
        val histogram = latencies.points.single().shouldBeInstanceOf<HistogramPoint>()
        histogram.count shouldBe 1L
        histogram.sum shouldBe 2.0
        delta.family(MetricName.of(REQUESTS))?.temporality shouldBe Temporality.Delta
        delta.family(MetricName.of("app.queue"))?.temporality shouldBe Temporality.Cumulative
    }
})
