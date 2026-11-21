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

package com.davils.kollect.core.jvm

import com.davils.kollect.core.internal.jvm.JvmBinder
import com.davils.kollect.core.internal.jvm.PlatformReadings
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Point
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val MILLISECONDS: Long = 20L
private const val EXPECTED_FAMILIES: Int = 11
private const val GC_DURATION: String = "jvm.gc.duration"
private const val FILE_DESCRIPTOR_LIMIT: String = "jvm.file_descriptor.limit"

private fun MetricsSnapshot.names(): List<String> = families.map { family -> family.descriptor.name.value }

private fun MetricsSnapshot.pointsNamed(name: String): List<Point> {
    val family: FamilySnapshot? = family(MetricName.of(name))
    return family?.points.orEmpty()
}

class JvmMetricsTest : FunSpec({
    test("reports the JVM this process runs on") {
        val registry = meterRegistry()
        registry.bindJvmMetrics().use { metrics ->
            System.gc()
            registry.snapshot().names() shouldContainAll listOf(
                "jvm.memory.used",
                "jvm.thread.count",
                "jvm.class.loaded",
                "jvm.cpu.count",
                GC_DURATION,
            )
            metrics.toString() shouldContain "JvmMetrics("
        }
        registry.snapshot().names() shouldContainAll listOf(GC_DURATION)
        PlatformReadings.subscribeToCollections { _, _, _ -> }()
    }

    test("records collections and skips readings that are missing") {
        val registry = meterRegistry()
        val readings = FakeReadings(fileDescriptors = null)
        val binder = JvmBinder(registry, readings)
        JvmMetricGroup.entries.sumOf { group -> binder.observed(group).size } shouldBe EXPECTED_FAMILIES
        val unsubscribe = binder.collections(JvmMetricGroup.entries.toSet())
        readings.listener.shouldNotBeNull().invoke("G1 Young", "end of minor GC", MILLISECONDS)
        unsubscribe()
        readings.isUnsubscribed shouldBe true
        val snapshot = registry.snapshot()
        val pauses = snapshot.pointsNamed(GC_DURATION).single()
        pauses.shouldBeInstanceOf<HistogramPoint>().count shouldBe 1L
        snapshot.pointsNamed("jvm.cpu.time").shouldBeEmpty()
        snapshot.pointsNamed("jvm.memory.limit").shouldBeEmpty()
        binder.collections(emptySet())()
    }

    test("reports file descriptors where the system has them") {
        val registry = meterRegistry()
        val readings = FakeReadings(fileDescriptors = 1L to 2L)
        JvmBinder(registry, readings).observed(JvmMetricGroup.FileDescriptors).size shouldBe 2
        val reported = registry.snapshot()
        reported.family(MetricName.of(FILE_DESCRIPTOR_LIMIT)).shouldNotBeNull()
        readings.fileDescriptors = null
        val missing = registry.snapshot()
        missing.pointsNamed(FILE_DESCRIPTOR_LIMIT).shouldBeEmpty()
    }
})
