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

package com.davils.kollect.core.fixture

import com.davils.kollect.core.Family
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.MeterRegistryBuilder
import com.davils.kollect.core.Refusal
import com.davils.kollect.core.Registration
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.Gauge
import com.davils.kollect.core.meter.Histogram
import com.davils.kollect.core.meterRegistry
import com.davils.kollect.core.orThrow
import com.davils.kore.testing.TestClock
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant

internal const val EPOCH_MILLISECONDS: Long = 1_700_000_000_000L

internal fun testClock(): TestClock = TestClock(Instant.fromEpochMilliseconds(EPOCH_MILLISECONDS))

internal fun testRegistry(
    clock: TestClock = testClock(),
    configure: MeterRegistryBuilder.() -> Unit = {
    },
): MeterRegistry = meterRegistry(clock = clock, timeSource = clock.timeSource, configure = configure)

internal fun MeterRegistry.counterOf(name: String, vararg keys: String): Family<Counter> {
    val registration = counter(name) { labelKeyList(keys.asList()) }
    return registration.orThrow()
}

internal fun MeterRegistry.gaugeOf(name: String, vararg keys: String): Family<Gauge> {
    val registration = gauge(name) { labelKeyList(keys.asList()) }
    return registration.orThrow()
}

internal fun MeterRegistry.histogramOf(name: String, vararg keys: String): Family<Histogram> {
    val registration = histogram(name) { labelKeyList(keys.asList()) }
    return registration.orThrow()
}

internal fun Registration<*>.refusal(): Refusal = shouldBeInstanceOf<Registration.Refused<*>>().refusal

internal inline fun <reified T : Refusal> Registration<*>.refusedAs(): T = refusal().shouldBeInstanceOf<T>()
