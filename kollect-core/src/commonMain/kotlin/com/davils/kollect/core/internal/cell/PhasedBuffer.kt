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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kollect.core.internal.cell

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicLongArray
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class PhasedBuffer(bucketCount: Int) {
    val buckets: AtomicLongArray = AtomicLongArray(bucketCount)
    val count: AtomicLong = AtomicLong(0L)
    val sum: AtomicLong = AtomicLong(0.0.toRawBits())
    val min: AtomicLong = AtomicLong(Double.POSITIVE_INFINITY.toRawBits())
    val max: AtomicLong = AtomicLong(Double.NEGATIVE_INFINITY.toRawBits())

    fun record(bucket: Int, value: Double) {
        buckets.addAndFetchAt(bucket, 1L)
        sum.addDouble(value)
        min.lowerTo(value)
        max.raiseTo(value)
        count.addAndFetch(1L)
    }

    fun mergeInto(other: PhasedBuffer, copy: LongArray) {
        copy.forEachIndexed { bucket, counted -> other.buckets.addAndFetchAt(bucket, counted) }
        other.sum.addDouble(sum.loadDouble())
        other.min.lowerTo(min.loadDouble())
        other.max.raiseTo(max.loadDouble())
        other.count.addAndFetch(count.load())
    }

    fun copyBuckets(): LongArray = LongArray(buckets.size) { bucket -> buckets.loadAt(bucket) }

    fun reset() {
        for (bucket in 0 until buckets.size) buckets.storeAt(index = bucket, newValue = 0L)
        sum.storeDouble(0.0)
        min.storeDouble(Double.POSITIVE_INFINITY)
        max.storeDouble(Double.NEGATIVE_INFINITY)
        count.store(0L)
    }
}
