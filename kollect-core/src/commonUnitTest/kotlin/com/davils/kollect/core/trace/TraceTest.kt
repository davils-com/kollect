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

import com.davils.kollect.core.fixture.RecordingTracer
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.snapshot.HistogramPoint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

private const val DURATION: String = "span.duration"
private const val CHECKOUT: String = "checkout"

class TraceTest : FunSpec({
    test("a metered tracer times every span by operation and outcome") {
        val clock = testClock()
        val registry = testRegistry(clock)
        val delegate = RecordingTracer()
        val tracer = delegate.metered(registry)
        tracer.traced(CHECKOUT, SpanKind.Server) { span ->
            span.attribute(key = "cart", value = "1")
            clock.advanceBy(1.seconds)
        }
        shouldThrow<IllegalStateException> { tracer.traced(CHECKOUT) { error("failed") } }
        val span = tracer.start("payment")
        span.end(SpanOutcome.Ok)
        span.end(SpanOutcome.Error)
        delegate.ended shouldContainExactly listOf(SpanOutcome.Ok, SpanOutcome.Error, SpanOutcome.Ok)
        val points = registry.points(DURATION).map { point -> point as HistogramPoint }
        points.map { point -> point.labels.values } shouldContainExactly
            listOf(listOf(CHECKOUT, "error"), listOf(CHECKOUT, "ok"), listOf("payment", "ok"))
        points[1].sum shouldBe 1.0
    }

    test("a tracer that throws starts a span that does nothing") {
        val registry = testRegistry()
        val broken = object : Tracer {
            override fun start(operation: String, kind: SpanKind): Span = error("broken")
        }
        val span = broken.metered(registry).start("work")
        span.attribute(key = "a", value = "b")
        span.end(SpanOutcome.Ok)
        Tracer.None.start("work").end(SpanOutcome.Ok)
        Tracer.None.traced("work") { span2 -> span2.attribute(key = "a", value = "b") }
        SpanOutcome.Error.label shouldBe "error"
    }
})
