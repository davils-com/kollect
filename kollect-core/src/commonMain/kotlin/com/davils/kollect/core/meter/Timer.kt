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

package com.davils.kollect.core.meter

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A histogram of durations in seconds, such as request latencies.
 *
 * @since 1.0.0
 */
public interface Timer : Meter {
    /**
     * The time source the registry measures durations with, which [time] uses.
     *
     * @since 1.0.0
     */
    public val timeSource: TimeSource

    /**
     * Records [duration], which is finite and not negative; any other duration is rejected and counted.
     *
     * @param duration the duration.
     * @since 1.0.0
     */
    public fun record(duration: Duration)
}

/**
 * Runs [block] and records how long it ran, also when it throws.
 *
 * @param T what [block] returns.
 * @receiver the timer to record into.
 * @param block the work to measure.
 * @return what [block] returns.
 * @since 1.0.0
 */
public inline fun <T> Timer.time(block: () -> T): T {
    val mark = timeSource.markNow()
    try {
        return block()
    } finally {
        record(mark.elapsedNow())
    }
}
