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

import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.name.LabelKey
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val MAXIMUM_VALUE_LENGTH: Int = 256
private const val MAXIMUM_ATTRIBUTES: Int = 32

/**
 * Adds attributes to a [Resource] while [resource] builds it.
 *
 * Only allowed keys may be added: `service.version`, `service.namespace` and `deployment.environment.name` are allowed
 * from the start, and [allow] allows another one, so an attribute never reaches a receiver by accident.
 *
 * @since 1.0.0
 */
@DavilsDsl
@OptIn(DavilsInternal::class)
public class ResourceBuilder internal constructor() {
    private val allowed: MutableList<String> = DEFAULT_ALLOWED.toMutableList()
    internal val added: MutableList<Pair<String, String>> = mutableListOf()

    /**
     * Allows the attribute [key].
     *
     * @param key the key, following the grammar of a [LabelKey], such as `cloud.region`.
     * @throws IllegalArgumentException if [key] does not follow the grammar or names an attribute kollect sets.
     * @since 1.0.0
     */
    public fun allow(key: String) {
        LabelKey.of(key)
        require(key !in OWN_KEYS) { "The attribute ${SafeText.quoted(key)} is set by kollect" }
        allowed.add(key)
    }

    /**
     * Adds the attribute [key] with [value].
     *
     * @param key an allowed key.
     * @param value the value, at most 256 characters without control characters.
     * @throws IllegalArgumentException if [key] is not allowed or already added, if [value] is invalid or if the
     *   resource would have more than 32 attributes.
     * @since 1.0.0
     */
    public fun attribute(key: String, value: String) {
        require(key in allowed) { "The attribute ${SafeText.quoted(key)} is not allowed" }
        require(added.none { (name, _) -> name == key }) { "The attribute ${SafeText.quoted(key)} is already added" }
        require(
            TextRules.isValidText(value, MAXIMUM_VALUE_LENGTH),
        ) { "The value of ${SafeText.quoted(key)} is invalid" }
        require(added.size < MAXIMUM_ATTRIBUTES) { "A resource has at most $MAXIMUM_ATTRIBUTES attributes" }
        added.add(key to value)
    }

    internal companion object {
        val DEFAULT_ALLOWED: List<String> = listOf(
            "service.version",
            "service.namespace",
            "deployment.environment.name",
        )
        val OWN_KEYS: Set<String> =
            setOf("service.name", "service.instance.id", "telemetry.sdk.name", "telemetry.sdk.language")
    }
}
