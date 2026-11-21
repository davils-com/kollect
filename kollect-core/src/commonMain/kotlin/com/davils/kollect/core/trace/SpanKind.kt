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
 * The role of a span in a request, as OpenTelemetry names it.
 *
 * @since 1.0.0
 */
public enum class SpanKind {
    /**
     * Work inside the process.
     *
     * @since 1.0.0
     */
    Internal,

    /**
     * The handling of a request the process received.
     *
     * @since 1.0.0
     */
    Server,

    /**
     * A request the process sent.
     *
     * @since 1.0.0
     */
    Client,

    /**
     * A message the process published.
     *
     * @since 1.0.0
     */
    Producer,

    /**
     * A message the process consumed.
     *
     * @since 1.0.0
     */
    Consumer,
}
