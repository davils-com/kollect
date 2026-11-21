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

package com.davils.kollect.core.otlp

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

/**
 * What an OTLP request says about the process that sent it: its service name, a random instance id and the
 * attributes the application allowed.
 *
 * kollect detects nothing on its own, neither the host, the operating system, a user agent, an address nor a user, so
 * nothing personal leaves the process unless the application adds it. A resource is immutable.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class Resource internal constructor(attributes: List<Pair<String, String>>) {
    /**
     * The attributes, ordered by key: `service.name`, `service.instance.id`, `telemetry.sdk.name`,
     * `telemetry.sdk.language` and those the application added.
     *
     * @since 1.0.0
     */
    public val attributes: List<Pair<String, String>> = attributes.sortedBy { (key, _) -> key }.immutableList()

    /**
     * Returns the value of the attribute [key], or null if the resource has none.
     *
     * @param key the key.
     * @return the value, or null.
     * @since 1.0.0
     */
    public operator fun get(key: String): String? = attributes.firstOrNull { (name, _) -> name == key }?.second

    /**
     * Tells whether [other] has the same attributes.
     *
     * @param other the object to compare with.
     * @return true if [other] is an equal resource.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is Resource && attributes == other.attributes

    /**
     * Returns a hash of the attributes.
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = attributes.hashCode()

    /**
     * Shows the attributes, each value escaped.
     *
     * @return the resource as text.
     * @since 1.0.0
     */
    override fun toString(): String = attributes.joinToString(
        prefix = "Resource(",
        postfix = ")",
    ) { (key, value) -> "$key=${SafeText.quoted(value)}" }
}
