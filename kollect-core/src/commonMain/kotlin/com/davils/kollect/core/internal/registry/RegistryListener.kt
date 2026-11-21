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

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.Unreadable

internal interface RegistryListener {
    fun declared(descriptor: Descriptor) = Unit

    fun removed(name: MetricName) = Unit

    fun refused(name: String, refusal: Refusal) = Unit

    fun overflowed(name: MetricName) = Unit

    fun unreadable(entry: Unreadable) = Unit

    fun rejected(rejection: Rejection, count: Long) = Unit

    fun flush() = Unit

    object None : RegistryListener
}
