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

package com.davils.kollect.core.export

import kotlin.time.Duration

/**
 * How a receiver answered one OTLP request.
 *
 * @since 1.0.0
 */
public sealed interface Delivery {
    /**
     * The receiver took the request; it may have rejected some of its points.
     *
     * @property rejectedPoints the points the receiver reported as rejected in its partial success.
     * @since 1.0.0
     */
    public class Accepted(public val rejectedPoints: Long = 0L) : Delivery {
        /**
         * Shows the answer, such as `Accepted(rejectedPoints=0)`.
         *
         * @return the answer as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Accepted(rejectedPoints=$rejectedPoints)"
    }

    /**
     * The request failed in a way that may pass later, such as a lost connection; the exporter retries it.
     *
     * @since 1.0.0
     */
    public object Retryable : Delivery {
        /**
         * Returns `Retryable`.
         *
         * @return the answer as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Retryable"
    }

    /**
     * The receiver asked to slow down; the exporter retries after [retryAfter], bounded by
     * [com.davils.kollect.core.limit.ExportLimits.maximumRetryAfter].
     *
     * @property retryAfter how long the receiver asked to wait, or null if it did not say.
     * @since 1.0.0
     */
    public class Throttled(public val retryAfter: Duration? = null) : Delivery {
        /**
         * Shows the answer, such as `Throttled(retryAfter=30s)`.
         *
         * @return the answer as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Throttled(retryAfter=${retryAfter ?: "unknown"})"
    }

    /**
     * The receiver refused the request for good, such as a malformed or unauthorized one; it is dropped.
     *
     * @since 1.0.0
     */
    public object Rejected : Delivery {
        /**
         * Returns `Rejected`.
         *
         * @return the answer as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Rejected"
    }
}
