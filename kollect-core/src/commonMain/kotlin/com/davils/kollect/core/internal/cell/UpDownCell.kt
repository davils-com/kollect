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

package com.davils.kollect.core.internal.cell

import com.davils.kollect.core.internal.platform.Striped
import com.davils.kollect.core.internal.platform.stripedCounter
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.meter.UpDownCounter
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.Point
import com.davils.kollect.core.snapshot.SumPoint
import kotlin.time.Instant

internal class UpDownCell(private val rejections: Rejections) : UpDownCounter, Cell {
    private val sum: Striped = stripedCounter()

    override fun increment() {
        sum.add(1)
    }

    override fun decrement() {
        sum.add(-1)
    }

    override fun add(amount: Long) {
        if (amount !in -Counter.MAXIMUM_INCREMENT..Counter.MAXIMUM_INCREMENT) {
            return rejections.reject(Rejection.TooLarge)
        }
        sum.add(amount)
    }

    override fun read(labels: Labels, start: Instant, context: ReadContext): Point =
        SumPoint(labels = labels, start = start, value = sum.sum.toDouble())
}
