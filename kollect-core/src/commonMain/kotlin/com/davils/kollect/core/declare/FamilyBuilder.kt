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

package com.davils.kollect.core.declare

import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Describes a family while it is declared: its description, unit and label keys.
 *
 * The registry validates what the builder holds when the declaration runs and refuses an invalid declaration with
 * [com.davils.kollect.core.Refusal.Invalid].
 *
 * @since 1.0.0
 */
@DavilsDsl
public open class FamilyBuilder internal constructor(unit: MeasureUnit) {
    /**
     * What the family measures, in words; empty by default. It may not hold control characters and is bounded by
     * [com.davils.kollect.core.limit.RegistryLimits.maximumDescriptionLength].
     *
     * @since 1.0.0
     */
    public var description: String = ""

    /**
     * The unit of the values.
     *
     * @since 1.0.0
     */
    public var unit: MeasureUnit = unit

    internal var keys: List<String> = emptyList()
        private set

    /**
     * Declares the label keys of the family, in the order their values are given to
     * [com.davils.kollect.core.Family.with].
     *
     * @param keys the keys, each following the grammar of a [com.davils.kollect.core.name.LabelKey].
     * @since 1.0.0
     */
    public fun labelKeys(vararg keys: String) {
        this.keys = keys.asList()
    }

    internal fun labelKeyList(keys: List<String>) {
        this.keys = keys
    }
}
