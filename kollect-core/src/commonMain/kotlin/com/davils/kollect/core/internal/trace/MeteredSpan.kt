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

package com.davils.kollect.core.internal.trace

import com.davils.kollect.core.Family
import com.davils.kollect.core.meter.Timer
import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanOutcome
import com.davils.kore.core.failure.contained
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.TimeMark

internal class MeteredSpan(
    private val delegate: Span,
    private val operation: String,
    private val family: Family<Timer>,
    private val started: TimeMark,
) : Span {
    private val ended: AtomicBoolean = AtomicBoolean(false)

    override fun attribute(key: String, value: String) {
        contained { delegate.attribute(key, value) }
    }

    override fun end(outcome: SpanOutcome) {
        if (!ended.compareAndSet(expectedValue = false, newValue = true)) return
        family.with(operation, outcome.label).record(started.elapsedNow())
        contained { delegate.end(outcome) }
    }
}
