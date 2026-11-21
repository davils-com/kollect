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

package com.davils.kollect.core.otlp

/**
 * Why an OTLP ingestion rejected a point of an otherwise valid request.
 *
 * @since 1.0.0
 */
public enum class IngestRejection {
    /**
     * The family is not one the ingestion accepts.
     *
     * @since 1.0.0
     */
    UndeclaredFamily,

    /**
     * The point does not match its family: another type, unit, set of attributes or bucket bounds.
     *
     * @since 1.0.0
     */
    Mismatch,

    /**
     * An attribute is reserved, such as `kollect.source`, `le` or one starting with `otel.`, or is no valid label.
     *
     * @since 1.0.0
     */
    InvalidLabels,

    /**
     * A value is not finite, a counter is negative or the buckets of a histogram do not add up to its count.
     *
     * @since 1.0.0
     */
    InvalidValue,

    /**
     * The point lies too far ahead of or behind the clock of the ingestion.
     *
     * @since 1.0.0
     */
    OutOfTime,

    /**
     * A delta overlaps a delta accepted before, such as a replayed request.
     *
     * @since 1.0.0
     */
    OutOfOrder,

    /**
     * The source or the ingestion holds as many series as it may.
     *
     * @since 1.0.0
     */
    SeriesLimit,

    /**
     * The point is of a type kollect does not ingest: an exponential histogram or a summary.
     *
     * @since 1.0.0
     */
    UnsupportedType,
}
