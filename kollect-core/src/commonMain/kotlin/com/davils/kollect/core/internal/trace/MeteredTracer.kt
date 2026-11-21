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

package com.davils.kollect.core.internal.trace

import com.davils.kollect.core.Family
import com.davils.kollect.core.meter.Timer
import com.davils.kollect.core.trace.Span
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.Tracer
import com.davils.kore.core.failure.contained
import kotlin.time.TimeSource

internal class MeteredTracer(
    private val delegate: Tracer,
    private val family: Family<Timer>,
    private val timeSource: TimeSource,
) : Tracer {
    override fun start(operation: String, kind: SpanKind): Span {
        val span = contained { delegate.start(operation, kind) }.getOrNull() ?: NoopSpan
        return MeteredSpan(delegate = span, operation = operation, family = family, started = timeSource.markNow())
    }
}
