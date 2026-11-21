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

package com.davils.kollect.core.internal.binder

import com.davils.kollect.core.meter.MeterKind
import com.davils.konfetti.core.observe.BusSnapshot
import com.davils.konfetti.core.observe.ConsumerMetrics
import com.davils.konfetti.core.observe.TopicMetrics

internal sealed interface BusFamily {
    val name: String
    val kind: MeterKind
    val description: String

    data class Topic(
        override val name: String,
        override val kind: MeterKind,
        override val description: String,
        val read: (TopicMetrics) -> Long,
    ) : BusFamily

    data class Consumer(
        override val name: String,
        override val kind: MeterKind,
        override val description: String,
        val read: (ConsumerMetrics) -> Long,
    ) : BusFamily

    data class Bus(
        override val name: String,
        override val kind: MeterKind,
        override val description: String,
        val read: (BusSnapshot) -> Long,
    ) : BusFamily
}
