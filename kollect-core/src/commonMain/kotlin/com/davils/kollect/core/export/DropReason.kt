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

/**
 * Why an exporter dropped points instead of delivering them.
 *
 * @since 1.0.0
 */
public enum class DropReason {
    /**
     * The queue was full; the oldest request made room.
     *
     * @since 1.0.0
     */
    QueueFull,

    /**
     * A newer cumulative snapshot replaced a request that was not sent yet; its sums reach the receiver with the
     * newer one.
     *
     * @since 1.0.0
     */
    Superseded,

    /**
     * The receiver rejected the request for good.
     *
     * @since 1.0.0
     */
    Rejected,

    /**
     * Every attempt failed.
     *
     * @since 1.0.0
     */
    RetriesExhausted,

    /**
     * The exporter shut down before the request was sent.
     *
     * @since 1.0.0
     */
    ShutdownDeadline,

    /**
     * A single metric would not fit into one request.
     *
     * @since 1.0.0
     */
    TooLarge,
}
