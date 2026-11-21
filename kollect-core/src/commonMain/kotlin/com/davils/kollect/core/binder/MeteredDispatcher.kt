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

import com.davils.kollect.core.internal.binder.DispatcherMeters
import com.davils.kollect.core.meter.time
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A dispatcher that runs every task on its delegate and measures it: the tasks dispatched, refused and completed, the
 * tasks that wait and run, how long they waited and how long they ran.
 *
 * kollect chooses no dispatcher: the application wraps its own with [metered]. A task the delegate refuses is counted
 * and its refusal rethrown unchanged.
 *
 * @since 1.0.0
 */
public class MeteredDispatcher internal constructor(
    private val delegate: CoroutineDispatcher,
    private val meters: DispatcherMeters,
    private val timeSource: TimeSource,
) : CoroutineDispatcher() {
    /**
     * Asks the delegate whether [context] needs a dispatch.
     *
     * @param context the context of the coroutine.
     * @return what the delegate answers.
     * @since 1.0.0
     */
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)

    /**
     * Dispatches [block] on the delegate and measures it.
     *
     * @param context the context of the coroutine.
     * @param block the task.
     * @since 1.0.0
     */
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val queuedAt = timeSource.markNow()
        meters.queued.increment()
        val measured = Runnable { run(block, queuedAt) }
        var isDispatched = false
        try {
            delegate.dispatch(context, measured)
            isDispatched = true
        } finally {
            settle(isDispatched)
        }
    }

    /**
     * Shows the dispatcher, such as `MeteredDispatcher(Dispatchers.Default)`.
     *
     * @return the dispatcher as text.
     * @since 1.0.0
     */
    override fun toString(): String = "MeteredDispatcher($delegate)"

    private fun run(block: Runnable, queuedAt: TimeMark) {
        meters.queued.decrement()
        meters.wait.record(queuedAt.elapsedNow())
        meters.running.increment()
        try {
            meters.duration.time { block.run() }
        } finally {
            meters.running.decrement()
            meters.completed.increment()
        }
    }

    private fun settle(isDispatched: Boolean) {
        if (isDispatched) meters.dispatched.increment()
        if (isDispatched) return
        meters.queued.decrement()
        meters.refused.increment()
    }
}
