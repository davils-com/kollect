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

package com.davils.kollect.core.internal.text

import com.davils.kollect.core.name.MetricName
import com.davils.kore.core.naming.NameRule
import com.davils.kore.core.naming.nameRule

internal object NameRules {
    val METRIC: NameRule = nameRule {
        separators = "_"
        segmentSeparator = '.'
        isStrict = true
        maxLength = MetricName.MAXIMUM_LENGTH
        example = "http.server.request_duration"
    }
}
