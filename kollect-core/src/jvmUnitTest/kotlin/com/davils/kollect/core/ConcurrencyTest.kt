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

import com.davils.kollect.core.internal.cell.HistogramCell
import com.davils.kollect.core.limit.registryLimits
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.SumPoint
import com.davils.kollect.core.snapshot.Unreadability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

private const val THREADS: Int = 16
private const val RECORDS: Int = 20_000
private const val SERIES: Int = 1_000
private const val WAIT_SECONDS: Long = 60L
private const val VALUES: Int = 3
private const val LATENCY: String = "latency"
private const val REQUESTS: String = "requests"

private fun MetricsSnapshot.pointsNamed(name: String): List<Point> {
    val family = family(MetricName.of(name))
    return family.shouldNotBeNull().points
}

private fun MetricsSnapshot.latency(): HistogramPoint {
    val point = pointsNamed(LATENCY).single()
    return point.shouldBeInstanceOf<HistogramPoint>()
}

class ConcurrencyTest : FunSpec({
    test("a histogram stays consistent while many threads record and a reader reads") {
        val registry = meterRegistry()
        val histogram = registry.histogram(LATENCY) { bounds = ExplicitBounds.of(1.0, 2.0) }.orThrow().meter
        val pool = Executors.newFixedThreadPool(THREADS)
        val start = CountDownLatch(1)
        repeat(THREADS) { writer ->
            pool.execute {
                start.await()
                repeat(RECORDS) { index -> histogram.record(((writer + index) % VALUES).toDouble()) }
            }
        }
        start.countDown()
        var previous = 0L
        while (!pool.isTerminated) {
            val point = registry.snapshot().latency()
            point.bucketCounts.sum() shouldBe point.count
            (point.count >= previous) shouldBe true
            previous = point.count
            pool.shutdown()
        }
        pool.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)
        val final = registry.snapshot().latency()
        final.count shouldBe (THREADS * RECORDS).toLong()
        final.bucketCounts.sum() shouldBe final.count
    }

    test("a recording in flight stalls a snapshot, and the next one catches up") {
        val registry = meterRegistry { limits = registryLimits { maximumSnapshotStall = 1.milliseconds } }
        val histogram = registry.histogram(LATENCY).orThrow().meter as HistogramCell
        val inFlight = CountDownLatch(1)
        val release = CountDownLatch(1)
        histogram.afterStart = {
            inFlight.countDown()
            release.await()
        }
        val writer = thread { histogram.record(1.0) }
        inFlight.await()
        histogram.afterStart = null
        val stalled = registry.snapshot()
        stalled.unreadable.single().reason shouldBe Unreadability.Stalled
        stalled.family(MetricName.of(LATENCY)) shouldBe null
        val again = registry.snapshot()
        again.unreadable.single().reason shouldBe Unreadability.Stalled
        release.countDown()
        writer.join()
        histogram.record(2.0)
        val caughtUp = registry.snapshot()
        caughtUp.unreadable.shouldBeEmpty()
        caughtUp.latency().count shouldBe 1L
        registry.snapshot().latency().count shouldBe 2L
    }

    test("series created by many threads are counted exactly and stay within the limit") {
        val registry = meterRegistry { limits = registryLimits { maximumSeriesPerFamily = SERIES } }
        val family = registry.counter(REQUESTS) { labelKeys("id") }.orThrow()
        val threads = List(THREADS) { writer ->
            thread {
                repeat(SERIES) { index ->
                    val id = (writer * SERIES + index) % (SERIES * 2)
                    family.with("s$id").increment()
                }
            }
        }
        threads.forEach(Thread::join)
        val points = registry.snapshot().pointsNamed(REQUESTS)
        points.size shouldBe SERIES
        points.sumOf { point -> (point as SumPoint).value } shouldBe (THREADS * SERIES).toDouble()
    }
})
