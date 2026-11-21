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

package com.davils.kollect.core.internal.wire

private const val INITIAL_CAPACITY: Int = 64
private const val SEVEN_BITS: Long = 0x7FL
private const val CONTINUATION: Int = 0x80
private const val BYTE_MASK: Long = 0xFFL
private const val BITS_PER_BYTE: Int = 8
private const val BYTES_PER_LONG: Int = 8
private const val SEVEN: Int = 7
private const val TAG_SHIFT: Int = 3

internal class ProtoWriter {
    private var bytes: ByteArray = ByteArray(INITIAL_CAPACITY)
    var size: Int = 0
        private set

    fun uint64(field: Int, value: Long) {
        if (value == 0L) return
        tag(field, WireType.VARINT)
        varint(value)
    }

    fun bool(field: Int, value: Boolean) {
        if (!value) return
        tag(field, WireType.VARINT)
        varint(1L)
    }

    fun fixed64(field: Int, value: Long) {
        if (value == 0L) return
        tag(field, WireType.FIXED64)
        rawFixed64(value)
    }

    fun double(field: Int, value: Double) {
        tag(field, WireType.FIXED64)
        rawFixed64(value.toRawBits())
    }

    fun string(field: Int, text: String) {
        if (text.isEmpty()) return
        bytes(field, text.encodeToByteArray())
    }

    fun bytes(field: Int, value: ByteArray) {
        tag(field, WireType.LENGTH)
        varint(value.size.toLong())
        raw(value)
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit) {
        val child = ProtoWriter()
        child.block()
        bytes(field, child.toByteArray())
    }

    fun packedFixed64(field: Int, values: List<Long>) {
        if (values.isEmpty()) return
        tag(field, WireType.LENGTH)
        varint((values.size * BYTES_PER_LONG).toLong())
        values.forEach(::rawFixed64)
    }

    fun packedDoubles(field: Int, values: List<Double>) {
        packedFixed64(field, values.map(Double::toRawBits))
    }

    fun toByteArray(): ByteArray = bytes.copyOf(size)

    private fun tag(field: Int, wireType: Int) {
        varint((field shl TAG_SHIFT or wireType).toLong())
    }

    private fun varint(value: Long) {
        var remaining = value
        while (remaining and SEVEN_BITS.inv() != 0L) {
            byte(((remaining and SEVEN_BITS).toInt() or CONTINUATION).toByte())
            remaining = remaining ushr SEVEN
        }
        byte(remaining.toByte())
    }

    private fun rawFixed64(value: Long) {
        for (shift in 0 until BYTES_PER_LONG) byte((value ushr shift * BITS_PER_BYTE and BYTE_MASK).toByte())
    }

    private fun raw(value: ByteArray) {
        ensure(value.size)
        value.copyInto(bytes, destinationOffset = size)
        size += value.size
    }

    private fun byte(value: Byte) {
        ensure(1)
        bytes[size] = value
        size += 1
    }

    private fun ensure(extra: Int) {
        val needed = size + extra
        if (needed <= bytes.size) return
        bytes = bytes.copyOf(maxOf(a = needed, b = bytes.size * 2))
    }
}
