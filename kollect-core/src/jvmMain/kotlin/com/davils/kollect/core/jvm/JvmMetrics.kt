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

import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The JVM metrics [bindJvmMetrics] registered: observed families read while a snapshot is taken, and a listener of
 * garbage collections.
 *
 * Closing the binding removes the families and the listener.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class JvmMetrics internal constructor(
    registrations: List<Registration<ObservedFamily>>,
    private val onClose: () -> Unit,
) : AutoCloseable {
    /**
     * The registration of every observed family.
     *
     * @since 1.0.0
     */
    public val registrations: List<Registration<ObservedFamily>> = registrations.immutableList()

    /**
     * Removes the families and stops listening to garbage collections.
     *
     * @since 1.0.0
     */
    override fun close() {
        registrations.forEach { registration -> registration.family.close() }
        onClose()
    }

    /**
     * Shows the binding, such as `JvmMetrics(13 families)`.
     *
     * @return the binding as text.
     * @since 1.0.0
     */
    override fun toString(): String = "JvmMetrics(${registrations.size} families)"
}
