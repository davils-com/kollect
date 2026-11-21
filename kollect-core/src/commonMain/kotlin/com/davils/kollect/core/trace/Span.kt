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

/**
 * A unit of work that a [Tracer] started.
 *
 * A span is ended once; ending it again does nothing. Attribute values may come from a caller, so a tracer that shows
 * them escapes them.
 *
 * @since 1.0.0
 */
public interface Span {
    /**
     * Adds the attribute [key] with [value].
     *
     * @param key the key, such as `http.request.method`.
     * @param value the value.
     * @since 1.0.0
     */
    public fun attribute(key: String, value: String)

    /**
     * Ends the span with [outcome].
     *
     * @param outcome how the work ended.
     * @since 1.0.0
     */
    public fun end(outcome: SpanOutcome)
}
