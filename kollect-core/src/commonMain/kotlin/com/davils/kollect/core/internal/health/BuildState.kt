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

package com.davils.kollect.core.internal.health

import com.davils.kollect.core.internal.event.HealthEvents
import com.davils.kollect.core.limit.HealthLimits

internal class BuildState(var limits: HealthLimits, var hasMeters: Boolean) {
    var events: HealthEvents? = null

    var members: Int = 0
        private set

    fun admit() {
        members += 1
        require(members <= limits.maximumMembers) { "A health registry has at most ${limits.maximumMembers} members" }
    }
}
