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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds

private const val OK: Int = 200
private const val BAD_REQUEST: Int = 400
private const val TOO_MANY_REQUESTS: Int = 429
private const val BAD_GATEWAY: Int = 502
private const val UNAVAILABLE: Int = 503
private const val SERVER_ERROR: Int = 500
private const val BIG: Int = 70_000

class DeliveryTest : FunSpec({
    test("maps the status codes of OTLP over HTTP") {
        OtlpResponses.deliveryOf(OK).shouldBeInstanceOf<Delivery.Accepted>().rejectedPoints shouldBe 0L
        OtlpResponses.deliveryOf(BAD_REQUEST) shouldBe Delivery.Rejected
        OtlpResponses.deliveryOf(SERVER_ERROR) shouldBe Delivery.Rejected
        OtlpResponses.deliveryOf(BAD_GATEWAY) shouldBe Delivery.Retryable
        OtlpResponses
            .deliveryOf(TOO_MANY_REQUESTS, retryAfter = 2.seconds)
            .shouldBeInstanceOf<Delivery.Throttled>()
            .retryAfter shouldBe 2.seconds
        OtlpResponses.deliveryOf(UNAVAILABLE).shouldBeInstanceOf<Delivery.Throttled>()
    }

    test("reads what it can of the body of an answer") {
        val cut = byteArrayOf(0x0A, 0x7F)
        val partial = OtlpResponses.deliveryOf(OK, cut)
        partial.shouldBeInstanceOf<Delivery.Accepted>().rejectedPoints shouldBe 0L
        OtlpResponses.deliveryOf(OK, ByteArray(BIG)).shouldBeInstanceOf<Delivery.Accepted>().rejectedPoints shouldBe 0L
        val skipped = byteArrayOf(0x10, 0x01, 0x0A, 0x04, 0x08, 0x05, 0x10, 0x01)
        OtlpResponses.deliveryOf(OK, skipped).shouldBeInstanceOf<Delivery.Accepted>().rejectedPoints shouldBe FIVE
    }

    test("show themselves") {
        Delivery.Accepted(1L).toString() shouldBe "Accepted(rejectedPoints=1)"
        Delivery.Throttled().toString() shouldBe "Throttled(retryAfter=unknown)"
        Delivery.Retryable.toString() shouldBe "Retryable"
        Delivery.Rejected.toString() shouldBe "Rejected"
        ExportObserver.None.observe(ExportEvent.Dropped(1L, DropReason.Rejected))
        ExportEvent.Dropped(1L, DropReason.Rejected).toString() shouldBe "Dropped(points=1, reason=Rejected)"
    }
})

private const val FIVE: Long = 5L
