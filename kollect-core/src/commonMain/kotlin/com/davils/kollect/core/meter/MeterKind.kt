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

package com.davils.kollect.core.meter

/**
 * What a family measures and how its points aggregate.
 *
 * The kind decides the point type of a family in a snapshot, its type in Prometheus and OpenMetrics and its data type
 * in OTLP.
 *
 * @since 1.0.0
 */
public enum class MeterKind {
    /**
     * A sum that only grows, such as requests served: a monotonic sum in OTLP, a `counter` in Prometheus.
     *
     * @since 1.0.0
     */
    Counter,

    /**
     * A sum that grows and shrinks, such as requests in flight: a non-monotonic sum in OTLP, a `gauge` in Prometheus.
     *
     * @since 1.0.0
     */
    UpDownCounter,

    /**
     * The last value of something, such as a temperature: a gauge everywhere.
     *
     * @since 1.0.0
     */
    Gauge,

    /**
     * The distribution of values over explicit bucket bounds, such as request durations: a histogram everywhere.
     *
     * @since 1.0.0
     */
    Histogram,

    /**
     * Which one of a fixed set of states is active, such as the state of a circuit breaker: a `stateset` in
     * OpenMetrics, a gauge per state elsewhere.
     *
     * @since 1.0.0
     */
    StateSet,

    /**
     * Constant facts about a process, such as its version, carried in labels: an `info` in OpenMetrics, a gauge of 1
     * elsewhere.
     *
     * @since 1.0.0
     */
    Info,
}
