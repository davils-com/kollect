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

import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Gauge
import com.davils.kollect.core.meter.Histogram
import com.davils.kollect.core.meter.Info
import com.davils.kollect.core.meter.LongTask
import com.davils.kollect.core.meter.LongTaskTimer
import com.davils.kollect.core.meter.UpDownCounter

internal object NoopMeters : Counter, UpDownCounter, Gauge, Histogram, LongTaskTimer, Info {
    override fun increment() = Unit

    override fun decrement() = Unit

    override fun add(amount: Long) = Unit

    override fun set(value: Double) = Unit

    override fun record(value: Double) = Unit

    override fun start(): LongTask = Task

    object Task : LongTask {
        override fun close() = Unit
    }
}
