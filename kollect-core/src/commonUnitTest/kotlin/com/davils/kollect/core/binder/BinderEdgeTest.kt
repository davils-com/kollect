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

package com.davils.kollect.core.binder

import com.davils.kollect.core.fixture.RefusingDispatcher
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testClock
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.internal.binder.BusReader
import com.davils.konfetti.testing.testEventBus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalKotest::class)
class BinderEdgeTest : FunSpec({
    coroutineTestScope = true

    test("a refused dispatch is counted and rethrown") {
        val registry = testRegistry()
        val dispatcher = RefusingDispatcher.metered(registry, "refusing")
        shouldThrow<IllegalStateException> { dispatcher.dispatch(EmptyCoroutineContext, Runnable { }) }
        dispatcher.isDispatchNeeded(EmptyCoroutineContext) shouldBe true
        registry.sums("coroutine.dispatcher.refused") shouldContainExactly listOf(1.0)
    }

    test("a bus reader reuses a fresh snapshot") {
        val scope = CoroutineScope(coroutineContext + Job())
        val clock = testClock()
        val reader = BusReader(testEventBus(scope), clock.timeSource)
        val first = reader.snapshot()
        reader.snapshot() shouldBeSameInstanceAs first
        clock.advanceBy(1.seconds)
        (reader.snapshot() === first) shouldBe false
        scope.cancel()
    }
})
