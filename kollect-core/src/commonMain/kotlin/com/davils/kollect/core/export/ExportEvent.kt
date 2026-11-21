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
 * What an exporter reports to its [ExportObserver].
 *
 * @since 1.0.0
 */
public sealed interface ExportEvent {
    /**
     * A request was delivered.
     *
     * @property points the points of the request.
     * @property rejectedPoints the points the receiver rejected.
     * @since 1.0.0
     */
    public class Delivered internal constructor(
        public val points: Long,
        public val rejectedPoints: Long,
    ) : ExportEvent {
        /**
         * Shows the event, such as `Delivered(points=120, rejectedPoints=0)`.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Delivered(points=$points, rejectedPoints=$rejectedPoints)"
    }

    /**
     * An attempt failed and the request is retried after [delay].
     *
     * @property attempt the number of the failed attempt, from 1.
     * @property delay the wait before the next attempt.
     * @since 1.0.0
     */
    public class Retrying internal constructor(public val attempt: Int, public val delay: Duration) : ExportEvent {
        /**
         * Shows the event, such as `Retrying(attempt=1, delay=700ms)`.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Retrying(attempt=$attempt, delay=$delay)"
    }

    /**
     * Points were dropped.
     *
     * @property points the points dropped.
     * @property reason why they were dropped.
     * @since 1.0.0
     */
    public class Dropped internal constructor(public val points: Long, public val reason: DropReason) : ExportEvent {
        /**
         * Shows the event, such as `Dropped(points=120, reason=Rejected)`.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Dropped(points=$points, reason=$reason)"
    }
}
