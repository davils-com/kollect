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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.otlp.IngestRejection

internal object SeriesUpdate {
    fun apply(
        series: SeriesState,
        kind: MeterKind,
        temporality: Long,
        point: DecodedPoint,
        retire: (SeriesState) -> Unit,
    ): IngestRejection? {
        val isDelta = temporality == OtlpFields.TEMPORALITY_DELTA
        if (isDelta) return delta(series, point)
        cumulative(series = series, kind = kind, point = point, retire = retire)
        return null
    }

    private fun delta(series: SeriesState, point: DecodedPoint): IngestRejection? {
        val isOverlapping = point.start < series.lastTime || point.time < point.start
        if (isOverlapping) return IngestRejection.OutOfOrder
        series.value += point.value
        series.count += point.count
        series.sum += point.sum
        series.buckets = series.buckets.takeIf { it.isNotEmpty() }?.zip(point.buckets, Long::plus) ?: point.buckets
        series.lastTime = point.time
        if (series.start == 0L) series.start = point.start
        return null
    }

    private fun cumulative(series: SeriesState, kind: MeterKind, point: DecodedPoint, retire: (SeriesState) -> Unit) {
        if (point.time <= series.lastTime) return
        if (isReset(series, kind, point)) retire(copyOf(series))
        series.value = point.value
        series.count = point.count
        series.sum = point.sum
        series.buckets = point.buckets
        series.start = point.start
        series.lastTime = point.time
    }

    private fun isReset(series: SeriesState, kind: MeterKind, point: DecodedPoint): Boolean {
        val isAccumulating = kind == MeterKind.Counter || kind == MeterKind.Histogram
        val isRestarted = series.start != 0L && point.start != series.start
        val isLower = point.value < series.value || point.count < series.count
        return isAccumulating && series.lastTime > 0L && (isRestarted || isLower)
    }

    fun copyOf(series: SeriesState): SeriesState {
        val copy = SeriesState(created = series.created, lastSeen = series.lastSeen)
        series.addInto(copy)
        return copy
    }
}
