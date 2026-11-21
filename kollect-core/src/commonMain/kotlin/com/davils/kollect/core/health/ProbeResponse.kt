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

package com.davils.kollect.core.health

import com.davils.kollect.core.Payload

/**
 * What to answer a health probe with.
 *
 * @property statusCode the HTTP status: 200 while the application is Up or Degraded, 503 while it is Down.
 * @property body the body, `application/health+json`.
 * @since 1.0.0
 */
public class ProbeResponse internal constructor(public val statusCode: Int, public val body: Payload) {
    /**
     * Shows the response, such as `ProbeResponse(503, Payload(…))`.
     *
     * @return the response as text.
     * @since 1.0.0
     */
    override fun toString(): String = "ProbeResponse($statusCode, $body)"
}
