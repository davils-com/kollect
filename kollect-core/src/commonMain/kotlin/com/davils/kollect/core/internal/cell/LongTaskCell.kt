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

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.internal.cell

import com.davils.kollect.core.meter.LongTask
import com.davils.kollect.core.meter.LongTaskTimer
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.Point
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.TimeSource

internal class LongTaskCell(
    private val limit: Int,
    private val timeSource: TimeSource.WithComparableMarks,
    private val rejections: Rejections,
) : LongTaskTimer, Cell {
    private val guard: Guard = guard()
    private val starts: MutableList<ComparableTimeMark?> = mutableListOf()
    private val generations: MutableList<Long> = mutableListOf()
    private val free: MutableList<Int> = mutableListOf()
    private var active: Int = 0

    val activeCount: Int get() = guard.locked { active }

    @Suppress("MissingUseCall")
    override fun start(): LongTask {
        val task = guard.locked { admitted() }
        if (task == null) rejections.reject(Rejection.Untracked)
        return task ?: NoopMeters.Task
    }

    override fun read(labels: Labels, start: Instant, context: ReadContext): Point {
        val total = guard.locked { starts.filterNotNull().sumOf(::secondsSince) }
        return GaugePoint(labels = labels, start = start, value = total, observedAt = context.now)
    }

    private fun secondsSince(mark: ComparableTimeMark): Double = mark.elapsedNow().toDouble(DurationUnit.SECONDS)

    private fun admitted(): LongTask? {
        if (active >= limit) return null
        val slot = free.removeLastOrNull() ?: newSlot()
        starts[slot] = timeSource.markNow()
        active += 1
        return RunningTask(slot = slot, generation = generations[slot])
    }

    private fun newSlot(): Int {
        starts.add(null)
        generations.add(0L)
        return starts.lastIndex
    }

    private fun finish(slot: Int, generation: Long) {
        guard.locked {
            val isCurrent = generations[slot] == generation
            if (isCurrent) release(slot)
        }
    }

    private fun release(slot: Int) {
        starts[slot] = null
        generations[slot] = generations[slot] + 1
        free.add(slot)
        active -= 1
    }

    private inner class RunningTask(private val slot: Int, private val generation: Long) : LongTask {
        override fun close() {
            finish(slot, generation)
        }
    }
}
