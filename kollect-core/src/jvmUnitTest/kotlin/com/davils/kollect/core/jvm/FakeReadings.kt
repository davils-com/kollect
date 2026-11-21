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

package com.davils.kollect.core.jvm

import com.davils.kollect.core.internal.jvm.ManagementReadings
import com.davils.kollect.core.internal.jvm.PoolReading

private const val THREADS: Long = 3L

internal class FakeReadings(var fileDescriptors: Pair<Long, Long>?) : ManagementReadings {
    var listener: ((String, String, Long) -> Unit)? = null
    var isUnsubscribed: Boolean = false

    override fun pools(): List<PoolReading> = listOf(
        PoolReading(name = "eden", type = "heap", used = 1L, committed = 2L, limit = -1L, usedAfterGc = -1L),
    )

    override fun threads(): Pair<Long, Long> = THREADS to 1L

    override fun classes(): Triple<Long, Long, Long> = Triple(first = 3L, second = 1L, third = 2L)

    override fun cpuSeconds(): Double? = null

    override fun processors(): Int = 2

    override fun cpuUtilization(): Double? = null

    override fun fileDescriptors(): Pair<Long, Long>? = fileDescriptors

    override fun subscribeToCollections(listener: (String, String, Long) -> Unit): () -> Unit {
        this.listener = listener
        return { isUnsubscribed = true }
    }
}
