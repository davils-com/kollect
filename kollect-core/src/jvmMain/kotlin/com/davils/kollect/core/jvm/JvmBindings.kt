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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kollect.core.internal.jvm.JvmBinder
import com.davils.kollect.core.internal.jvm.PlatformReadings

/**
 * Reports the JVM this process runs on in this registry: memory pools, garbage collection pauses, threads, classes,
 * CPU and file descriptors, named after the semantic conventions of OpenTelemetry.
 *
 * Readings are taken while a snapshot is taken, from the management beans of the JVM; a reading that fails leaves its
 * family unreadable for that snapshot. Threads are counted without thread dumps, so a snapshot never stops the JVM.
 * Garbage collections are recorded as they are announced, on a thread of the JVM, into the timer `jvm.gc.duration`.
 *
 * @receiver the registry.
 * @param groups the groups to report; all by default.
 * @return the binding, which removes the families and the listener when it is closed.
 * @since 1.0.0
 */
public fun MeterRegistry.bindJvmMetrics(groups: Set<JvmMetricGroup> = JvmMetricGroup.entries.toSet()): JvmMetrics {
    val binder = JvmBinder(this, PlatformReadings)
    val registrations: List<Registration<ObservedFamily>> = groups.flatMap(binder::observed)
    val unsubscribe = binder.collections(groups)
    return JvmMetrics(registrations, unsubscribe)
}
