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

package com.davils.kollect.core.internal.otlp

import com.davils.kollect.core.otlp.IngestRejection

internal class IngestCounts {
    var accepted: Long = 0L
        private set
    private val rejected: LongArray = LongArray(IngestRejection.entries.size)

    val rejectedTotal: Long get() = rejected.sum()

    fun record(rejection: IngestRejection?) {
        if (rejection == null) accepted += 1
        if (rejection != null) rejected[rejection.ordinal] += 1
    }

    fun rejections(): Map<IngestRejection, Long> = IngestRejection.entries
        .filter { rejection -> rejected[rejection.ordinal] > 0 }
        .associateWith { rejection -> rejected[rejection.ordinal] }
}
