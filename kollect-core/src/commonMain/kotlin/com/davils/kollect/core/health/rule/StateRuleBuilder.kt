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

package com.davils.kollect.core.health.rule

import com.davils.kore.core.annotation.DavilsDsl

/**
 * Describes a state rule: the state set it reads and the states that make it Degraded and Down. A family with several
 * series is as unhealthy as its worst series.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class StateRuleBuilder internal constructor() {
    /**
     * The name of the state set, such as `breaker.state`; required.
     *
     * @since 1.0.0
     */
    public var meter: String = ""

    /**
     * What is decided when the rule cannot decide; [Undetermined.Down] by default.
     *
     * @since 1.0.0
     */
    public var undetermined: Undetermined = Undetermined.Down

    internal val downStates: MutableList<String> = mutableListOf()
    internal val degradedStates: MutableList<String> = mutableListOf()

    /**
     * Makes the rule Down while a series is in one of [states].
     *
     * @param states the names of the states.
     * @since 1.0.0
     */
    public fun down(vararg states: String) {
        downStates.addAll(states)
    }

    /**
     * Makes the rule Degraded while a series is in one of [states].
     *
     * @param states the names of the states.
     * @since 1.0.0
     */
    public fun degraded(vararg states: String) {
        degradedStates.addAll(states)
    }
}
