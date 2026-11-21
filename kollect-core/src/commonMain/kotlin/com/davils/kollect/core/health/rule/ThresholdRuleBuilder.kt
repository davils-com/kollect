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
import kotlin.time.Duration

/**
 * Describes a threshold rule: the family it reads, the statistic, the direction and the thresholds that make it
 * Degraded and Down. A family with several series is as unhealthy as its worst series.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ThresholdRuleBuilder internal constructor() {
    /**
     * The name of the family, such as `http.server.errors`; required.
     *
     * @since 1.0.0
     */
    public var meter: String = ""

    /**
     * What is read from each series; [Statistic.Value] by default.
     *
     * @since 1.0.0
     */
    public var statistic: Statistic = Statistic.Value

    /**
     * What is decided when the rule cannot decide; [Undetermined.Down] by default.
     *
     * @since 1.0.0
     */
    public var undetermined: Undetermined = Undetermined.Down

    /**
     * The longest a gauge may go unobserved before it is stale, or null to accept any age; null by default.
     *
     * @since 1.0.0
     */
    public var maximumAge: Duration? = null

    internal var direction: Direction = Direction.Above
        private set
    internal var degraded: Double? = null
        private set
    internal var down: Double? = null
        private set

    /**
     * Crosses at or above [degraded] and [down].
     *
     * @param degraded the threshold that makes the rule Degraded, or null for none.
     * @param down the threshold that makes the rule Down, or null for none.
     * @since 1.0.0
     */
    public fun above(degraded: Double? = null, down: Double? = null) {
        thresholds(Direction.Above, degraded, down)
    }

    /**
     * Crosses at or below [degraded] and [down].
     *
     * @param degraded the threshold that makes the rule Degraded, or null for none.
     * @param down the threshold that makes the rule Down, or null for none.
     * @since 1.0.0
     */
    public fun below(degraded: Double? = null, down: Double? = null) {
        thresholds(Direction.Below, degraded, down)
    }

    private fun thresholds(direction: Direction, degraded: Double?, down: Double?) {
        this.direction = direction
        this.degraded = degraded
        this.down = down
    }
}
