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

package com.davils.kollect.core.internal.event

import com.davils.kollect.core.event.MetricTopics
import com.davils.kollect.core.event.RegistryEvent
import com.davils.kollect.core.name.MetricName
import com.davils.konfetti.testing.testEventBus
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

private const val MANY: Int = 1_100

@OptIn(ExperimentalKotest::class)
class OutboxTest : FunSpec({
    coroutineTestScope = true

    test("an outbox drops what exceeds its capacity and reuses a declared topic") {
        val scope = CoroutineScope(coroutineContext + Job())
        val bus = testEventBus(scope) { topic(MetricTopics.REGISTRY) }
        val outbox = Outbox(bus, MetricTopics.REGISTRY)
        repeat(MANY) { outbox.add(RegistryEvent.FamilyRemoved(MetricName.of("a"))) }
        outbox.flush()
        outbox.rejected shouldBeGreaterThan 0L
        outbox.flush()
        bus[MetricTopics.REGISTRY].metrics.published shouldBeGreaterThan 0L
        outbox.rejected shouldBe outbox.rejected
        scope.cancel()
    }
})
