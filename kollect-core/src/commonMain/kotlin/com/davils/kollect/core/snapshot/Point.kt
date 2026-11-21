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

package com.davils.kollect.core.snapshot

import com.davils.kollect.core.name.Labels
import kotlin.time.Instant

/**
 * The value of one series of a family at the time of a snapshot.
 *
 * A point is immutable. Its type follows the kind of its family: [SumPoint] for counters and up-down counters,
 * [GaugePoint] for gauges, [HistogramPoint] for histograms, [StateSetPoint] for state sets and [InfoPoint] for info
 * families.
 *
 * @since 1.0.0
 */
public sealed interface Point {
    /**
     * The labels of the series.
     *
     * @since 1.0.0
     */
    public val labels: Labels

    /**
     * When the series started to accumulate the value of this point: when it was created, or when it was last reset.
     * OTLP carries it as the start time of a point and OpenMetrics as the `_created` sample.
     *
     * @since 1.0.0
     */
    public val start: Instant
}
