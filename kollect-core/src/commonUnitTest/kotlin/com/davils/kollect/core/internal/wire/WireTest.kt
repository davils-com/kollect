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

package com.davils.kollect.core.internal.wire

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.failure.contained
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val FIELD: Int = 3
private const val LARGE: Long = 1L shl 62
private const val BUDGET: Long = 10_000L
private const val LIMIT: Int = 64
private const val MAXIMUM_FUZZ_LENGTH: Int = 64
private const val RATIO: Double = 1.5
private const val GROUP_START: Byte = 0x0B
private const val FIXED64_TAG: Byte = 0x09
private const val VARINT_TAG: Byte = 0x08

private fun readerOf(bytes: ByteArray, budget: Long = BUDGET): ProtoReader =
    ProtoReader.of(bytes, WorkBudget(maximum = budget, limit = "test"))

private fun written(block: ProtoWriter.() -> Unit): ByteArray {
    val writer = ProtoWriter()
    writer.block()
    return writer.toByteArray()
}

private fun ProtoReader.skipAll() {
    while (hasMore) {
        next()
        skip()
    }
}

class WireTest : FunSpec({
    test("values written are read back") {
        val bytes = written {
            uint64(FIELD, LARGE)
            fixed64(FIELD, -1L)
            double(FIELD, RATIO)
            string(FIELD, "text")
            bool(FIELD, true)
            packedFixed64(FIELD, listOf(1L, 2L))
        }
        val reader = readerOf(bytes)
        reader.next()
        reader.uint64() shouldBe LARGE
        reader.next()
        reader.fixed64() shouldBe -1L
        reader.next()
        reader.double() shouldBe RATIO
        reader.next()
        reader.string(LIMIT) shouldBe "text"
        reader.next()
        reader.bool() shouldBe true
        reader.next()
        reader.packedFixed64(LIMIT) shouldContainExactly listOf(1L, 2L)
        reader.hasMore shouldBe false
    }

    test("defaults are left out") {
        written {
            uint64(FIELD, 0L)
            fixed64(FIELD, 0L)
            string(FIELD, "")
            bool(FIELD, false)
            packedFixed64(FIELD, emptyList())
        }.size shouldBe 0
    }

    test("an unpacked repeated value is read as one element") {
        val reader = readerOf(written { fixed64(FIELD, 2L) })
        reader.next()
        reader.packedFixed64(LIMIT) shouldContainExactly listOf(2L)
    }

    test("unknown fields of every wire type are skipped") {
        val bytes = written {
            uint64(1, 1L)
            fixed64(2, 1L)
            string(FIELD, "skipped")
        } +
            byteArrayOf(0x25, 0, 0, 0, 0)
        readerOf(bytes).skipAll()
    }

    context("a hostile message") {
        test("with a length beyond its end is malformed") {
            val reader = readerOf(byteArrayOf(0x1A, 0x10, 0x41))
            reader.next()
            shouldThrow<Malformed> { reader.string(LIMIT) }
        }

        test("with a varint of more than ten bytes is malformed") {
            shouldThrow<Malformed> { readerOf(ByteArray(LIMIT) { -1 }).next() }
        }

        test("with a group, field 0 or a cut value is malformed") {
            shouldThrow<Malformed> { readerOf(byteArrayOf(GROUP_START)).skipAll() }
            shouldThrow<Malformed> { readerOf(byteArrayOf(0x00)).next() }
            shouldThrow<Malformed> { readerOf(byteArrayOf(FIXED64_TAG, 1)).skipAll() }
            shouldThrow<Malformed> { readerOf(byteArrayOf(VARINT_TAG)).skipAll() }
        }

        test("with a wrong wire type, a long string or invalid UTF-8 is malformed") {
            val wrongType = readerOf(written { uint64(FIELD, 1L) })
            wrongType.next()
            shouldThrow<Malformed> { wrongType.string(LIMIT) }
            val long = readerOf(written { string(FIELD, "x".repeat(LIMIT + 1)) })
            long.next()
            shouldThrow<Malformed> { long.string(LIMIT) }
            val invalid = readerOf(written { bytes(FIELD, byteArrayOf(-1, -2)) })
            invalid.next()
            shouldThrow<Malformed> { invalid.string(LIMIT) }
        }

        test("with too many or misaligned packed values is malformed") {
            val many = readerOf(written { packedFixed64(FIELD, List(LIMIT + 1) { 1L }) })
            many.next()
            shouldThrow<Malformed> { many.packedFixed64(LIMIT) }
            val misaligned = readerOf(written { bytes(FIELD, ByteArray(FIELD)) })
            misaligned.next()
            shouldThrow<Malformed> { misaligned.packedFixed64(LIMIT) }
        }

        test("that nests too deep or works too hard is malformed") {
            var nested = written { string(FIELD, "leaf") }
            repeat(ProtoReader.HARD_DEPTH + 1) { nested = written { bytes(FIELD, nested) } }
            var reader = readerOf(nested)
            val descend: () -> Unit = {
                reader.next()
                reader = reader.message()
            }
            shouldThrow<Malformed> { repeat(ProtoReader.HARD_DEPTH + 1) { descend() } }
            shouldThrow<Malformed> { readerOf(written { repeat(LIMIT) { uint64(FIELD, 1L) } }, budget = 1L).skipAll() }
            val packed = readerOf(written { packedFixed64(FIELD, List(LIMIT) { 1L }) }, budget = 2L)
            packed.next()
            shouldThrow<Malformed> { packed.packedFixed64(LIMIT) }
        }

        test("of random bytes is read or refused, never anything else") {
            checkAll(Arb.byteArray(Arb.int(0..MAXIMUM_FUZZ_LENGTH), Arb.byte())) { bytes ->
                val outcome = contained { readerOf(bytes).skipAll() }
                val failure = outcome.exceptionOrNull()
                (failure == null || failure is Malformed) shouldBe true
            }
        }
    }
})
