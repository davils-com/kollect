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
import com.davils.kore.core.identity.UuidV4

private const val MAXIMUM_NAME_LENGTH: Int = 256

/**
 * Creates the resource of a process.
 *
 * `service.instance.id` is a random UUID version 4, new for every resource, so two processes of a service never mix
 * their series and the id says nothing about the machine.
 *
 * @param serviceName the name of the service, at most 256 characters without control characters.
 * @param configure adds allowed attributes.
 * @return the resource.
 * @throws IllegalArgumentException if [serviceName] is empty or invalid, or [configure] adds an invalid attribute.
 * @since 1.0.0
 */
public fun resource(serviceName: String, configure: ResourceBuilder.() -> Unit = {}): Resource {
    val isValid = serviceName.isNotEmpty() && TextRules.isValidText(serviceName, MAXIMUM_NAME_LENGTH)
    require(
        isValid,
    ) { "The name of a service is between 1 and $MAXIMUM_NAME_LENGTH characters without control characters" }
    val builder = ResourceBuilder().apply(configure)
    val own = listOf(
        "service.name" to serviceName,
        "service.instance.id" to UuidV4.random().toString(),
        "telemetry.sdk.name" to "kollect",
        "telemetry.sdk.language" to "kotlin",
    )
    return Resource(own + builder.added)
}
