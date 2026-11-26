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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.kollect.core.internal.event

import com.davils.konfetti.core.Event
import com.davils.konfetti.core.EventBus
import com.davils.konfetti.core.Topic
import com.davils.konfetti.core.TopicKey
import com.davils.konfetti.core.publish.PublishResult
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import com.davils.kore.core.failure.contained
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val MAXIMUM_PENDING: Int = 1_024

internal class Outbox<T : Event>(bus: EventBus, key: TopicKey<T>) {
    private val topic: Topic<T> = topicOf(bus, key)
    private val guard: Guard = guard()
    private val pending: MutableList<T> = mutableListOf()
    private val rejectedCount: AtomicLong = AtomicLong(0L)

    val rejected: Long get() = rejectedCount.load()

    fun add(event: T) {
        val isAdded = guard.locked {
            (pending.size < MAXIMUM_PENDING).also { isRoomy ->
                if (isRoomy) {
                    pending.add(
                        event,
                    )
                }
            }
        }
        if (!isAdded) rejectedCount.addAndFetch(1L)
    }

    fun flush() {
        val events = guard.locked { pending.toList().also { pending.clear() } }
        events.forEach { event ->
            val result = contained { topic.offer(event) }.getOrNull()
            if (result !is PublishResult.Published) rejectedCount.addAndFetch(1L)
        }
    }

    private companion object {
        fun <T : Event> topicOf(bus: EventBus, key: TopicKey<T>): Topic<T> {
            if (key !in bus) bus.declare(key)
            return bus[key]
        }
    }
}
