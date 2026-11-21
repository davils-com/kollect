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

package com.davils.kollect.core.internal.cell

import com.davils.kollect.core.internal.platform.Striped
import com.davils.kollect.core.internal.platform.stripedCounter
import com.davils.kollect.core.meter.Rejection

internal class Rejections {
    private val counts: List<Striped> = Rejection.entries.map { stripedCounter() }

    fun reject(rejection: Rejection) {
        counts[rejection.ordinal].add(1)
    }

    fun count(rejection: Rejection): Long = counts[rejection.ordinal].sum
}
