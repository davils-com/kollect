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

package com.davils.kollect.core

import com.davils.kollect.core.meter.Descriptor
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Why a registry refused to declare a family.
 *
 * A refused declaration still returns a family, whose meters record nothing, so code that declares its meters never
 * has to handle a missing one; [Registration.orThrow] turns a refusal into an exception for code that must not start
 * without its meters. Every refusal is counted in the family `kollect.declarations.refused`, labelled with [label].
 *
 * @since 1.0.0
 */
public sealed interface Refusal {
    /**
     * The value of the `reason` label of the refusal, such as `conflict`.
     *
     * @since 1.0.0
     */
    public val label: String

    /**
     * The declaration was invalid, such as a name that does not follow the grammar or a description that is too long.
     *
     * @property problem what was invalid, with every text from the caller escaped.
     * @since 1.0.0
     */
    public class Invalid internal constructor(public val problem: String) : Refusal {
        override val label: String get() = "invalid"

        /**
         * Shows the refusal, such as `Invalid(the name … is not a metric name)`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Invalid($problem)"
    }

    /**
     * A family with the same name but another descriptor exists, or the Prometheus names of the family collide with
     * those of [existing].
     *
     * @property existing the family the declaration conflicts with.
     * @since 1.0.0
     */
    public class Conflict internal constructor(public val existing: Descriptor) : Refusal {
        override val label: String get() = "conflict"

        /**
         * Shows the refusal, such as `Conflict(Descriptor(…))`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Conflict($existing)"
    }

    /**
     * A limit of [com.davils.kollect.core.limit.RegistryLimits] would be exceeded.
     *
     * @property limit the name of the limit, such as `maximumFamilies`.
     * @since 1.0.0
     */
    public class LimitReached internal constructor(public val limit: String) : Refusal {
        override val label: String get() = "limit_reached"

        /**
         * Shows the refusal, such as `LimitReached(maximumFamilies)`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "LimitReached(${SafeText.text(limit)})"
    }

    /**
     * A meter filter denied the family.
     *
     * @since 1.0.0
     */
    public object Denied : Refusal {
        override val label: String get() = "denied"

        /**
         * Returns `Denied`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Denied"
    }

    /**
     * A meter filter threw; the declaration fails closed.
     *
     * @since 1.0.0
     */
    public object FilterFailed : Refusal {
        override val label: String get() = "filter_failed"

        /**
         * Returns `FilterFailed`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        override fun toString(): String = "FilterFailed"
    }

    /**
     * The registry is closed.
     *
     * @since 1.0.0
     */
    public object Closed : Refusal {
        override val label: String get() = "closed"

        /**
         * Returns `Closed`.
         *
         * @return the refusal as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Closed"
    }
}
