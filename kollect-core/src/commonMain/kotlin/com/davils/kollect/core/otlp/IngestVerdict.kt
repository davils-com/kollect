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

private const val OK: Int = 200
private const val BAD_REQUEST: Int = 400
private const val CONTENT_TOO_LARGE: Int = 413
private const val UNSUPPORTED_MEDIA_TYPE: Int = 415
private const val TOO_MANY_REQUESTS: Int = 429

/**
 * What an OTLP ingestion made of a request, with the HTTP status an application answers it with.
 *
 * @property statusCode the HTTP status of the answer, as the OTLP/HTTP specification asks for it.
 * @since 1.0.0
 */
public enum class IngestVerdict(public val statusCode: Int) {
    /**
     * Every point was accepted.
     *
     * @since 1.0.0
     */
    Accepted(OK),

    /**
     * Some points were rejected; the answer reports how many in its partial success.
     *
     * @since 1.0.0
     */
    PartiallyAccepted(OK),

    /**
     * The request is no valid OTLP request; nothing was accepted. A sender must not retry it.
     *
     * @since 1.0.0
     */
    Malformed(BAD_REQUEST),

    /**
     * The request exceeds a limit of its size; nothing was accepted.
     *
     * @since 1.0.0
     */
    TooLarge(CONTENT_TOO_LARGE),

    /**
     * The content type is not `application/x-protobuf`; nothing was accepted.
     *
     * @since 1.0.0
     */
    UnsupportedContent(UNSUPPORTED_MEDIA_TYPE),

    /**
     * The source sends too fast or the ingestion holds as many sources as it may; nothing was accepted. A sender may
     * retry later.
     *
     * @since 1.0.0
     */
    Throttled(TOO_MANY_REQUESTS),
}
