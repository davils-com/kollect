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

import com.davils.kollect.core.Payload

/**
 * Sends one OTLP request to a receiver, with whatever the application chose: an HTTP client, `fetch` in a browser or a
 * message queue.
 *
 * kollect has no transport of its own. A transport sends the payload with its content type, maps the answer to a
 * [Delivery], for OTLP/HTTP with [OtlpResponses.deliveryOf], and adds the authentication and transport security the
 * receiver needs. A transport that throws is treated as [Delivery.Retryable].
 *
 * @since 1.0.0
 */
public fun interface Transport {
    /**
     * Sends [payload] and reports how the receiver answered.
     *
     * @param payload the request and its content type.
     * @return how the receiver answered.
     * @since 1.0.0
     */
    public suspend fun send(payload: Payload): Delivery
}
