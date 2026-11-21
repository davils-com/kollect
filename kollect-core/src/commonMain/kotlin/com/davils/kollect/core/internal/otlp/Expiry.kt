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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.meter.MeterKind
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlin.time.Instant

internal object Expiry {
    fun expire(state: SourceState, now: Instant, limits: IngestLimits, kindOf: (String) -> MeterKind?) {
        val series = OrderedMap<SeriesId, SeriesState>()
        state.series.entries.forEach { (id, current) ->
            val isSilent = current.lastSeen < now - limits.instanceExpiry
            if (!isSilent) series[id] = current
            if (isSilent && isAccumulating(kindOf(id.family))) retire(state, id, current)
        }
        state.series = series
        val retired = OrderedMap<SeriesId, SeriesState>()
        state.retired.entries.forEach { (id, base) ->
            val isRecent = base.lastSeen >= now - limits.seriesExpiry
            val isLive = isRecent || series.keys.any { live -> live.retiredId == id }
            if (isLive) retired[id] = base
        }
        state.retired = retired
    }

    private fun retire(state: SourceState, id: SeriesId, current: SeriesState) {
        val base = state.retired[id.retiredId] ?: SeriesState(created = current.created, lastSeen = current.lastSeen)
        current.addInto(base)
        state.retired[id.retiredId] = base
    }

    private fun isAccumulating(kind: MeterKind?): Boolean = kind == MeterKind.Counter || kind == MeterKind.Histogram
}
