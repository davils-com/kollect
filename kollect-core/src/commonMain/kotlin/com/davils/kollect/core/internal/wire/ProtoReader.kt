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

private const val SEVEN_BITS: Int = 0x7F
private const val CONTINUATION: Int = 0x80
private const val BYTE_MASK: Long = 0xFFL
private const val BITS_PER_BYTE: Int = 8
private const val BYTES_PER_LONG: Int = 8
private const val BYTES_PER_INT: Int = 4
private const val MAXIMUM_VARINT_BYTES: Int = 10
private const val SEVEN: Int = 7
private const val TAG_SHIFT: Int = 3
private const val WIRE_MASK: Int = 7
private val FIXED_SIZES: Map<Int, Int> = mapOf(WireType.FIXED64 to BYTES_PER_LONG, WireType.FIXED32 to BYTES_PER_INT)

internal class ProtoReader(
    private val bytes: ByteArray,
    private var position: Int,
    private val limit: Int,
    private val budget: WorkBudget,
    private val depth: Int,
) {
    val hasMore: Boolean get() = position < limit

    var field: Int = 0
        private set

    var wireType: Int = 0
        private set

    fun next() {
        charge()
        val tag = varint()
        field = (tag ushr TAG_SHIFT).toInt()
        wireType = (tag and WIRE_MASK.toLong()).toInt()
        if (field <= 0) throw Malformed("a field number is not positive")
    }

    fun uint64(): Long {
        expect(WireType.VARINT)
        return varint()
    }

    fun bool(): Boolean = uint64() != 0L

    fun fixed64(): Long {
        expect(WireType.FIXED64)
        return rawFixed64()
    }

    fun double(): Double = Double.fromBits(fixed64())

    fun string(maximumBytes: Int): String {
        val (from, until) = window()
        if (until - from > maximumBytes) throw Malformed("a string is longer than its limit")
        val decoded = contained {
            bytes.decodeToString(
                startIndex = from,
                endIndex = until,
                throwOnInvalidSequence = true,
            )
        }
        return decoded.getOrNull() ?: throw Malformed("a string is not valid UTF-8")
    }

    fun message(): ProtoReader {
        if (depth >= HARD_DEPTH) throw Malformed("messages nest too deep")
        val (from, until) = window()
        return ProtoReader(bytes = bytes, position = from, limit = until, budget = budget, depth = depth + 1)
    }

    fun packedFixed64(maximumCount: Int): List<Long> {
        if (wireType == WireType.FIXED64) return listOf(rawFixed64())
        val (from, until) = window()
        val length = until - from
        val isAligned = length % BYTES_PER_LONG == 0
        if (!isAligned || length / BYTES_PER_LONG > maximumCount) throw Malformed("a packed field is too long")
        if (!budget.tryCharge(length.toLong())) throw Malformed("the message takes too much work")
        val reader = ProtoReader(bytes = bytes, position = from, limit = until, budget = budget, depth = depth)
        return List(length / BYTES_PER_LONG) { reader.rawFixed64() }
    }

    fun skip() {
        val fixed = FIXED_SIZES[wireType]
        if (fixed != null) return advance(fixed)
        val isLength = wireType == WireType.LENGTH
        if (isLength) window()
        if (!isLength) uint64()
    }

    private fun window(): Pair<Int, Int> {
        expect(WireType.LENGTH)
        val length = varint()
        if (length < 0L || length > (limit - position).toLong()) throw Malformed("a length exceeds its message")
        val from = position
        position += length.toInt()
        return from to position
    }

    private fun expect(type: Int) {
        if (wireType != type) throw Malformed("field $field has the wrong wire type")
    }

    private fun charge() {
        if (!budget.tryCharge(1L)) throw Malformed("the message takes too much work")
    }

    private fun varint(): Long {
        var result = 0L
        var shift = 0
        var isDone = false
        while (!isDone) {
            if (shift >= MAXIMUM_VARINT_BYTES * SEVEN) throw Malformed("a varint is too long")
            val next = nextByte()
            result = result or ((next and SEVEN_BITS).toLong() shl shift)
            shift += SEVEN
            isDone = next and CONTINUATION == 0
        }
        return result
    }

    private fun rawFixed64(): Long {
        var result = 0L
        for (index in 0 until BYTES_PER_LONG) {
            val part = nextByte().toLong() and BYTE_MASK
            result = result or (part shl index * BITS_PER_BYTE)
        }
        return result
    }

    private fun advance(count: Int) {
        if (count > limit - position) throw Malformed("a value exceeds its message")
        position += count
    }

    private fun nextByte(): Int {
        if (position >= limit) throw Malformed("the message ends early")
        val value = bytes[position].toInt() and BYTE_MASK.toInt()
        position += 1
        return value
    }

    companion object {
        const val HARD_DEPTH: Int = 16

        fun of(bytes: ByteArray, budget: WorkBudget): ProtoReader =
            ProtoReader(bytes = bytes, position = 0, limit = bytes.size, budget = budget, depth = 0)
    }
}
