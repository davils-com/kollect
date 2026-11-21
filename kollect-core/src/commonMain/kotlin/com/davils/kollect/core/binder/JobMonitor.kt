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

import com.davils.kollect.core.Family
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.UpDownCounter
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

/**
 * Counts the jobs an application hands to it: how many are active and how each one ended, completed, cancelled or
 * failed.
 *
 * @property name the name of the monitor, the value of its `monitor` label.
 * @since 1.0.0
 */
public class JobMonitor internal constructor(
    public val name: String,
    private val active: UpDownCounter,
    private val ended: Family<Counter>,
) {
    /**
     * Watches [job] until it ends.
     *
     * @param job the job.
     * @return [job].
     * @since 1.0.0
     */
    public fun watch(job: Job): Job {
        active.increment()
        job.invokeOnCompletion { cause -> end(cause) }
        return job
    }

    /**
     * Shows the monitor, such as `JobMonitor(imports)`.
     *
     * @return the monitor as text.
     * @since 1.0.0
     */
    override fun toString(): String = "JobMonitor($name)"

    private fun end(cause: Throwable?) {
        active.decrement()
        val failed = "cancelled".takeIf { cause is CancellationException } ?: "failed"
        val outcome = "completed".takeIf { cause == null } ?: failed
        ended.with(name, outcome).increment()
    }
}
