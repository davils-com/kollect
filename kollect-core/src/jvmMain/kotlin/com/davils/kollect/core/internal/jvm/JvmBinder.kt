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

package com.davils.kollect.core.internal.jvm

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.jvm.JvmMetricGroup
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.orThrow
import com.davils.kore.core.failure.contained
import kotlin.time.Duration.Companion.milliseconds

private const val POOL_KEYS: String = "jvm.memory.pool.name"
private const val TYPE_KEY: String = "jvm.memory.type"
private val GC_BOUNDS: ExplicitBounds = ExplicitBounds.of(0.01, 0.1, 1.0, 10.0)

internal class JvmBinder(private val registry: MeterRegistry, private val readings: ManagementReadings) {
    fun observed(group: JvmMetricGroup): List<Registration<ObservedFamily>> = when (group) {
        JvmMetricGroup.Memory -> memory()
        JvmMetricGroup.GarbageCollection -> emptyList()
        JvmMetricGroup.Threads -> threads()
        JvmMetricGroup.Classes -> classes()
        JvmMetricGroup.Cpu -> cpu()
        JvmMetricGroup.FileDescriptors -> fileDescriptors()
    }

    fun collections(groups: Set<JvmMetricGroup>): () -> Unit {
        if (JvmMetricGroup.GarbageCollection !in groups) return {}
        val registration = registry.timer("jvm.gc.duration") {
            description = "The duration of the pauses of the garbage collector."
            labelKeys("jvm.gc.name", "jvm.gc.action")
            bounds = GC_BOUNDS
        }
        val family = registration.orThrow()
        return readings.subscribeToCollections { collector, action, milliseconds ->
            contained { family.with(collector, action).record(milliseconds.milliseconds) }
        }
    }

    private fun memory(): List<Registration<ObservedFamily>> = listOf(
        pools(name = "jvm.memory.used", text = "The memory used, by pool.") { pool -> pool.used },
        pools(name = "jvm.memory.committed", text = "The memory committed, by pool.") { pool -> pool.committed },
        pools(
            name = "jvm.memory.limit",
            text = "The most memory a pool may use, if it has a limit.",
        ) { pool -> pool.limit },
        pools(
            name = "jvm.memory.used_after_last_gc",
            text = "The memory used after the last collection, by pool.",
        ) { pool ->
            pool.usedAfterGc
        },
    )

    private fun pools(name: String, text: String, read: (PoolReading) -> Long): Registration<ObservedFamily> =
        registry.observedUpDownCounter(name, describe(text, MeasureUnit.BYTES, TYPE_KEY, POOL_KEYS)) {
            readings.pools().forEach { pool ->
                val value = read(pool)
                if (value >= 0L) observe(value.toDouble(), pool.type, pool.name)
            }
        }

    private fun threads(): List<Registration<ObservedFamily>> = listOf(
        registry.observedUpDownCounter(
            "jvm.thread.count",
            describe(
                "The live threads, by whether they are daemons.",
                MeasureUnit.annotation("thread"),
                "jvm.thread.daemon",
            ),
        ) {
            val (all, daemons) = readings.threads()
            observe(daemons.toDouble(), "true")
            observe((all - daemons).toDouble(), "false")
        },
    )

    private fun classes(): List<Registration<ObservedFamily>> {
        val unit = MeasureUnit.annotation("class")
        return listOf(
            registry.observedCounter("jvm.class.loaded", describe("The classes loaded since the start.", unit)) {
                observe(readings.classes().first.toDouble())
            },
            registry.observedCounter("jvm.class.unloaded", describe("The classes unloaded since the start.", unit)) {
                observe(readings.classes().second.toDouble())
            },
            registry.observedUpDownCounter("jvm.class.count", describe("The classes loaded now.", unit)) {
                observe(readings.classes().third.toDouble())
            },
        )
    }

    private fun cpu(): List<Registration<ObservedFamily>> = listOf(
        registry.observedCounter("jvm.cpu.time", describe("The CPU time the process used.", MeasureUnit.SECONDS)) {
            readings.cpuSeconds()?.let { seconds -> observe(seconds) }
        },
        registry.observedUpDownCounter(
            "jvm.cpu.count",
            describe("The processors available.", MeasureUnit.annotation("cpu")),
        ) {
            observe(readings.processors().toDouble())
        },
        registry.observedGauge(
            "jvm.cpu.recent_utilization",
            describe("The recent CPU utilization of the process.", MeasureUnit.RATIO),
        ) {
            readings.cpuUtilization()?.let { utilization -> observe(utilization) }
        },
    )

    private fun fileDescriptors(): List<Registration<ObservedFamily>> {
        if (readings.fileDescriptors() == null) return emptyList()
        val unit = MeasureUnit.annotation("file_descriptor")
        return listOf(
            registry.observedUpDownCounter("jvm.file_descriptor.count", describe("The open file descriptors.", unit)) {
                readings.fileDescriptors()?.let { (open, _) -> observe(open.toDouble()) }
            },
            registry.observedUpDownCounter("jvm.file_descriptor.limit", describe("The most file descriptors.", unit)) {
                readings.fileDescriptors()?.let { (_, limit) -> observe(limit.toDouble()) }
            },
        )
    }

    private fun describe(text: String, unit: MeasureUnit, vararg keys: String): FamilyBuilder.() -> Unit = {
        description = text
        this.unit = unit
        labelKeys(*keys)
    }
}
