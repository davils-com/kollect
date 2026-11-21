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

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.export

import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.wire.ProtoReader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.failure.contained
import kotlin.time.Duration

private const val OK_FROM: Int = 200
private const val OK_UNTIL: Int = 299
private const val TOO_MANY_REQUESTS: Int = 429
private const val UNAVAILABLE: Int = 503
private const val MAXIMUM_BODY: Int = 65_536
private val RETRYABLE: Set<Int> = setOf(TOO_MANY_REQUESTS, 502, UNAVAILABLE, 504)

/**
 * Maps the answer of an OTLP/HTTP receiver to a [Delivery], following the OTLP/HTTP specification.
 *
 * @since 1.0.0
 */
public object OtlpResponses {
    /**
     * Returns the delivery an OTLP/HTTP answer means.
     *
     * A 2xx answer is [Delivery.Accepted] with the rejected points of its partial success, read from at most 64 KiB of
     * [body]; a body that cannot be read counts no rejected points. 429 and 503 are [Delivery.Throttled] with
     * [retryAfter], 502 and 504 are [Delivery.Retryable], and every other status is [Delivery.Rejected]. The function
     * never throws.
     *
     * @param statusCode the HTTP status.
     * @param body the body of the answer, or null if it had none.
     * @param retryAfter the value of its `Retry-After` header as a duration, or null.
     * @return the delivery.
     * @since 1.0.0
     */
    public fun deliveryOf(statusCode: Int, body: ByteArray? = null, retryAfter: Duration? = null): Delivery {
        if (statusCode in OK_FROM..OK_UNTIL) return Delivery.Accepted(rejectedOf(body))
        val isThrottled = statusCode == TOO_MANY_REQUESTS || statusCode == UNAVAILABLE
        val failed = Delivery.Retryable.takeIf { statusCode in RETRYABLE } ?: Delivery.Rejected
        return Delivery.Throttled(retryAfter).takeIf { isThrottled } ?: failed
    }

    private fun rejectedOf(body: ByteArray?): Long {
        val bytes = body?.takeIf { it.size <= MAXIMUM_BODY } ?: return 0L
        val budget = WorkBudget(maximum = bytes.size.toLong() + 1L, limit = "otlp response")
        val rejected = contained { partialOf(ProtoReader.of(bytes, budget)) }
        return rejected.getOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    private fun partialOf(reader: ProtoReader): Long {
        var rejected = 0L
        while (reader.hasMore) {
            reader.next()
            val isPartial = reader.field == OtlpFields.RESPONSE_PARTIAL_SUCCESS
            if (isPartial) rejected = rejectedIn(reader.message())
            if (!isPartial) reader.skip()
        }
        return rejected
    }

    private fun rejectedIn(reader: ProtoReader): Long {
        var rejected = 0L
        while (reader.hasMore) {
            reader.next()
            val isRejected = reader.field == OtlpFields.PARTIAL_REJECTED
            if (isRejected) rejected = reader.uint64()
            if (!isRejected) reader.skip()
        }
        return rejected
    }
}
