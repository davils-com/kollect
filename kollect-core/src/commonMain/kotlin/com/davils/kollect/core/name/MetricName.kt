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

package com.davils.kollect.core.name

import com.davils.kollect.core.internal.text.NameRules
import kotlin.jvm.JvmInline

/**
 * The name of a metric family, such as `http.server.request.duration`.
 *
 * A name has one or more parts separated by `.`; each part starts with a lower-case ASCII letter and continues with
 * lower-case ASCII letters, digits and single `_` between them. It has at most [MAXIMUM_LENGTH] characters, and a
 * registry may bound it further with [com.davils.kollect.core.limit.RegistryLimits.maximumNameLength]. The grammar
 * leaves no room for text that could break out of an exposition format, so a valid name is safe to write anywhere.
 *
 * [PrometheusNaming] maps a name to the name Prometheus sees, `.` becoming `_`.
 *
 * @property value the name as text.
 * @since 1.0.0
 */
@JvmInline
public value class MetricName private constructor(public val value: String) : Comparable<MetricName> {
    /**
     * Orders names by their text, character by character.
     *
     * @param other the name to compare with.
     * @return a negative number, zero or a positive number as this name is before, equal to or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: MetricName): Int = value.compareTo(other.value)

    /**
     * Returns the name as text.
     *
     * @return [value].
     * @since 1.0.0
     */
    override fun toString(): String = value

    /**
     * Creates metric names.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The longest name: 255 characters.
         *
         * @since 1.0.0
         */
        public const val MAXIMUM_LENGTH: Int = 255

        /**
         * Returns [text] as a metric name.
         *
         * @param text the name.
         * @return the name.
         * @throws IllegalArgumentException if [text] does not follow the grammar of a metric name; the message shows
         *   [text] escaped.
         * @since 1.0.0
         */
        public fun of(text: String): MetricName = MetricName(
            NameRules.METRIC.requireValid(text = text, kind = "metric name"),
        )

        /**
         * Returns [text] as a metric name, or null if it does not follow the grammar of a metric name.
         *
         * @param text the name.
         * @return the name, or null.
         * @since 1.0.0
         */
        public fun ofOrNull(text: String): MetricName? = NameRules.METRIC.validOrNull(text)?.let(::MetricName)
    }
}
