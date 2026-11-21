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

package com.davils.kollect.core.internal.binder

import com.davils.kollect.core.internal.platform.Guard
import com.davils.kollect.core.internal.platform.guard
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.observe.BusSnapshot
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val REUSE = 100.milliseconds

internal class BusReader(private val bus: EventBus, private val timeSource: TimeSource.WithComparableMarks) {
    private val guard: Guard = guard()
    private var cached: BusSnapshot? = null
    private var takenAt: ComparableTimeMark? = null

    fun snapshot(): BusSnapshot = guard.locked {
        val isFresh = takenAt?.let { mark -> mark.elapsedNow() < REUSE } == true
        cached?.takeIf { isFresh } ?: bus.snapshot().also { fresh ->
            cached = fresh
            takenAt = timeSource.markNow()
        }
    }
}
