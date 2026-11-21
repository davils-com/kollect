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

package com.davils.kollect.core.event

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.Unreadability
import com.davils.konfetti.core.Event
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What a meter registry publishes on [MetricTopics.REGISTRY] when its families change or it rejects something.
 *
 * Measurement rejections are counted between snapshots and published once per snapshot, so a flood of bad values does
 * not flood the bus.
 *
 * @since 1.0.0
 */
public sealed interface RegistryEvent : Event {
    /**
     * A family was declared.
     *
     * @property descriptor the family.
     * @since 1.0.0
     */
    public class FamilyDeclared internal constructor(public val descriptor: Descriptor) : RegistryEvent {
        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "FamilyDeclared(${descriptor.name})"
    }

    /**
     * A family was removed or its callback closed.
     *
     * @property name the name of the family.
     * @since 1.0.0
     */
    public class FamilyRemoved internal constructor(public val name: MetricName) : RegistryEvent {
        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "FamilyRemoved($name)"
    }

    /**
     * A declaration was refused.
     *
     * @property name the name the declaration asked for, which may be invalid; show it escaped.
     * @property refusal why it was refused.
     * @since 1.0.0
     */
    public class DeclarationRefused internal constructor(public val name: String, public val refusal: Refusal) :
        RegistryEvent {
        /**
         * Shows the event, the name escaped.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "DeclarationRefused(${SafeText.quoted(name)}, $refusal)"
    }

    /**
     * A family started to record new series into its overflow series; published once until a new series fits again.
     *
     * @property name the name of the family.
     * @since 1.0.0
     */
    public class FamilyOverflowed internal constructor(public val name: MetricName) : RegistryEvent {
        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "FamilyOverflowed($name)"
    }

    /**
     * A snapshot could not read a family.
     *
     * @property name the name of the family.
     * @property reason why.
     * @since 1.0.0
     */
    public class FamilyUnreadable internal constructor(public val name: MetricName, public val reason: Unreadability) :
        RegistryEvent {
        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "FamilyUnreadable($name, $reason)"
    }

    /**
     * Measurements were rejected since the last snapshot.
     *
     * @property rejection why.
     * @property count how many.
     * @since 1.0.0
     */
    public class MeasurementsRejected internal constructor(public val rejection: Rejection, public val count: Long) :
        RegistryEvent {
        /**
         * Shows the event.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "MeasurementsRejected($rejection, $count)"
    }
}
