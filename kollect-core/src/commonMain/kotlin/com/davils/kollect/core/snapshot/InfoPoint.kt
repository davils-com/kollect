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

package com.davils.kollect.core.snapshot

import com.davils.kollect.core.name.Labels
import kotlin.time.Instant

private const val HASH_FACTOR: Int = 31

/**
 * An info series, whose facts are its labels.
 *
 * @property labels the labels of the series, the facts it carries.
 * @property start when the series was created.
 * @since 1.0.0
 */
public class InfoPoint internal constructor(override val labels: Labels, override val start: Instant) : Point {
    /**
     * Tells whether [other] is an equal point.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same labels and start.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is InfoPoint) return false
        return labels == other.labels && start == other.start
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * labels.hashCode() + start.hashCode()

    /**
     * Shows the point, such as `InfoPoint({version="1.0.0"})`.
     *
     * @return the point as text.
     * @since 1.0.0
     */
    override fun toString(): String = "InfoPoint($labels, start=$start)"
}
