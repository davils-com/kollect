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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kollect.core.internal.cell

import com.davils.kollect.core.internal.platform.Guard
import com.davils.kollect.core.internal.platform.guard
import com.davils.kollect.core.internal.platform.spinPause
import com.davils.kollect.core.meter.Histogram
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.HistogramPoint
import com.davils.kollect.core.snapshot.Point
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Instant

private const val ACTIVE_BIT: Int = 63
private const val STARTED_MASK: Long = Long.MAX_VALUE
private const val FLIP: Long = Long.MIN_VALUE
private const val NOTHING_PENDING: Long = -1L

internal open class HistogramCell(
    private val bounds: DoubleArray,
    private val isNegativeAllowed: Boolean,
    private val rejections: Rejections,
) : Histogram, Cell {
    private val buffers: Array<PhasedBuffer> = Array(2) { PhasedBuffer(bounds.size + 1) }
    private val phase: AtomicLong = AtomicLong(0L)
    private val readGuard: Guard = guard()
    private var pendingStarted: Long = NOTHING_PENDING
    private var pendingCold: Int = 0

    internal var afterStart: (() -> Unit)? = null

    override fun record(value: Double) {
        if (!value.isFinite()) return rejections.reject(Rejection.NotFinite)
        if (value < 0.0 && !isNegativeAllowed) return rejections.reject(Rejection.Negative)
        recordAccepted(value)
    }

    private fun recordAccepted(value: Double) {
        val started = phase.fetchAndAdd(1L)
        afterStart?.invoke()
        val buffer = buffers[(started ushr ACTIVE_BIT).toInt()]
        buffer.record(bucketOf(value), value)
    }

    override fun read(labels: Labels, start: Instant, context: ReadContext): Point? = readGuard.locked {
        if (pendingStarted == NOTHING_PENDING) flip()
        val cold = buffers[pendingCold]
        val isSettled = awaitSettled(cold, context)
        if (!isSettled) context.stalled()
        pointOrNull(isSettled = isSettled, cold = cold, labels = labels, start = start)
    }

    private fun flip() {
        val before = phase.fetchAndAdd(FLIP)
        pendingCold = (before ushr ACTIVE_BIT).toInt()
        pendingStarted = before and STARTED_MASK
    }

    private fun awaitSettled(cold: PhasedBuffer, context: ReadContext): Boolean {
        val deadline = context.timeSource.markNow() + context.maximumStall
        while (cold.count.load() != pendingStarted && deadline.hasNotPassedNow()) spinPause()
        return cold.count.load() == pendingStarted
    }

    private fun pointOrNull(isSettled: Boolean, cold: PhasedBuffer, labels: Labels, start: Instant): Point? {
        if (!isSettled) return null
        val counts = cold.copyBuckets()
        val count = cold.count.load()
        val point = HistogramPoint(
            labels = labels,
            start = start,
            count = count,
            sum = cold.sum.loadDouble(),
            min = cold.min.loadDouble().takeIf { count > 0 },
            max = cold.max.loadDouble().takeIf { count > 0 },
            bucketCounts = counts.asList(),
        )
        cold.mergeInto(buffers[1 - pendingCold], counts)
        cold.reset()
        pendingStarted = NOTHING_PENDING
        return point
    }

    private fun bucketOf(value: Double): Int {
        var low = 0
        var high = bounds.size
        while (low < high) {
            val middle = low + high ushr 1
            val isAbove = bounds[middle] < value
            low = (middle + 1).takeIf { isAbove } ?: low
            high = high.takeIf { isAbove } ?: middle
        }
        return low
    }
}
