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

package com.davils.kollect.core

import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * Creates a meter registry.
 *
 * @param clock the clock that stamps snapshots, the start of series and the observation of gauges; the system clock
 *   by default.
 * @param timeSource the monotonic time source that measures durations; the monotonic time source by default.
 * @param configure sets the limits and filters of the registry.
 * @return the registry.
 * @throws IllegalArgumentException if [configure] adds more filters than the limits allow.
 * @since 1.0.0
 */
public fun meterRegistry(
    clock: Clock = Clock.System,
    timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    configure: MeterRegistryBuilder.() -> Unit = {},
): MeterRegistry {
    val builder = MeterRegistryBuilder()
    builder.configure()
    return MeterRegistry(clock, timeSource, builder)
}
