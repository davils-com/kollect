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

package com.davils.kollect.core.internal.limit

import kotlin.reflect.KProperty0

internal data class Bound(val property: KProperty0<*>, val value: Long, val ceiling: Long) {
    constructor(property: KProperty0<*>, value: Int, ceiling: Int) : this(property, value.toLong(), ceiling.toLong())
}
