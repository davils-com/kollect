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

package com.davils.kollect.core.trace

import com.davils.kollect.core.internal.trace.NoopSpan

/**
 * Starts spans; the bridge to whatever tracing an application uses.
 *
 * kollect ships no exporter of traces: an application adapts its tracing library to this interface, and
 * [metered] adds a duration timer to any tracer. [None] traces nothing.
 *
 * @since 1.0.0
 */
public interface Tracer {
    /**
     * Starts a span of [operation].
     *
     * @param operation the name of the operation, such as `checkout`.
     * @param kind the role of the span.
     * @return the span.
     * @since 1.0.0
     */
    public fun start(operation: String, kind: SpanKind = SpanKind.Internal): Span

    /**
     * The tracer that traces nothing.
     *
     * @since 1.0.0
     */
    public companion object None : Tracer {
        /**
         * Returns a span that does nothing.
         *
         * @param operation ignored.
         * @param kind ignored.
         * @return the span.
         * @since 1.0.0
         */
        override fun start(operation: String, kind: SpanKind): Span = NoopSpan
    }
}
