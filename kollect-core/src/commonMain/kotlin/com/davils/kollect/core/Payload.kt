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

package com.davils.kollect.core

private const val CHUNK: Int = 8_192

/**
 * Bytes kollect produced for an application to send, such as an exposition or an OTLP request, with their content
 * type.
 *
 * A payload is immutable: [copyBytes] and [writeTo] hand out copies, so the receiver may keep or change them.
 *
 * @property contentType the media type of the bytes, such as `text/plain; version=0.0.4; charset=utf-8`.
 * @since 1.0.0
 */
public class Payload internal constructor(private val bytes: ByteArray, public val contentType: String) {
    /**
     * The number of bytes.
     *
     * @since 1.0.0
     */
    public val size: Int get() = bytes.size

    /**
     * Returns a copy of the bytes.
     *
     * @return the bytes.
     * @since 1.0.0
     */
    public fun copyBytes(): ByteArray = bytes.copyOf()

    /**
     * Hands the bytes to [sink] in chunks of at most 8 KiB, each a fresh copy.
     *
     * @param sink receives each chunk with the number of its bytes.
     * @since 1.0.0
     */
    public fun writeTo(sink: (chunk: ByteArray, length: Int) -> Unit) {
        (bytes.indices step CHUNK).forEach { from ->
            val until = minOf(a = from + CHUNK, b = bytes.size)
            sink(bytes.copyOfRange(fromIndex = from, toIndex = until), until - from)
        }
    }

    /**
     * Shows the payload, such as `Payload(1532 bytes, text/plain; version=0.0.4; charset=utf-8)`.
     *
     * @return the payload as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Payload($size bytes, $contentType)"
}
