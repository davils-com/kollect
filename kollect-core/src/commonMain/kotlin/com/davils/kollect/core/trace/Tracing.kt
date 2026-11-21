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

package com.davils.kollect.core.trace

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.internal.trace.MeteredTracer
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.orThrow

/**
 * Runs [block] in a span of [operation] and ends it with [SpanOutcome.Ok], or with [SpanOutcome.Error] when [block]
 * throws, which it rethrows.
 *
 * @receiver the tracer.
 * @param T what [block] returns.
 * @param operation the name of the operation.
 * @param kind the role of the span.
 * @param block the work, which receives the span.
 * @return what [block] returns.
 * @since 1.0.0
 */
public inline fun <T> Tracer.traced(operation: String, kind: SpanKind = SpanKind.Internal, block: (Span) -> T): T {
    val span = start(operation, kind)
    var outcome = SpanOutcome.Error
    try {
        return block(span).also { outcome = SpanOutcome.Ok }
    } finally {
        span.end(outcome)
    }
}

/**
 * Returns a tracer that starts spans with this tracer and records the duration of every span in the timer family
 * `span.duration` of [registry], labelled with `operation` and `outcome`.
 *
 * The number of operations is bounded by the limits of the family: operations beyond
 * [com.davils.kollect.core.limit.RegistryLimits.maximumSeriesPerFamily] are recorded in its overflow series, and an
 * operation name that is no valid label value is not recorded but counted.
 *
 * @receiver the tracer to wrap, such as [Tracer.None].
 * @param registry the registry of the timer.
 * @param bounds the bucket bounds of the timer.
 * @return the metered tracer.
 * @throws IllegalStateException if the registry refuses the family `span.duration`.
 * @since 1.0.0
 */
public fun Tracer.metered(registry: MeterRegistry, bounds: ExplicitBounds = ExplicitBounds.SECONDS): Tracer {
    val family = registry.timer("span.duration") {
        description = "The duration of spans, by operation and outcome."
        labelKeys("operation", "outcome")
        this.bounds = bounds
    }
    return MeteredTracer(this, family.orThrow(), registry.timeSource)
}
