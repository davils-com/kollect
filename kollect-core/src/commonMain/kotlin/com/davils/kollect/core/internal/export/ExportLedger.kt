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

package com.davils.kollect.core.internal.export

import com.davils.kollect.core.export.DropReason
import com.davils.kollect.core.export.ExportAccounting
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class ExportLedger {
    private val produced: AtomicLong = AtomicLong(0L)
    private val delivered: AtomicLong = AtomicLong(0L)
    private val rejected: AtomicLong = AtomicLong(0L)
    private val pending: AtomicLong = AtomicLong(0L)
    private val dropped: List<AtomicLong> = DropReason.entries.map { AtomicLong(0L) }

    fun produced(points: Long) {
        produced.addAndFetch(points)
        pending.addAndFetch(points)
    }

    fun delivered(points: Long, rejectedPoints: Long) {
        val refused = rejectedPoints.coerceIn(minimumValue = 0L, maximumValue = points)
        pending.addAndFetch(-points)
        delivered.addAndFetch(points - refused)
        rejected.addAndFetch(refused)
    }

    fun dropped(points: Long, reason: DropReason) {
        pending.addAndFetch(-points)
        dropped[reason.ordinal].addAndFetch(points)
    }

    fun accounting(): ExportAccounting = ExportAccounting(
        produced = produced.load(),
        delivered = delivered.load(),
        rejected = rejected.load(),
        dropped = DropReason.entries
            .filter { reason -> dropped[reason.ordinal].load() > 0L }
            .associateWith { reason -> dropped[reason.ordinal].load() },
        pending = pending.load(),
    )
}
