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

package com.davils.kollect.core.health

/**
 * Why something is not [HealthStatus.Up].
 *
 * Every code but [ThresholdCrossed], [StateMatched] and [Reported] means the health could not be decided; a rule
 * decides such a case as Down unless it was told to decide it as Degraded, never as Up.
 *
 * @since 1.0.0
 */
public enum class ReasonCode {
    /**
     * A value crossed the threshold of a rule.
     *
     * @since 1.0.0
     */
    ThresholdCrossed,

    /**
     * A state set is in a state a rule names.
     *
     * @since 1.0.0
     */
    StateMatched,

    /**
     * An indicator reported the status itself.
     *
     * @since 1.0.0
     */
    Reported,

    /**
     * The family a rule reads does not exist or has no series.
     *
     * @since 1.0.0
     */
    NoData,

    /**
     * The snapshot could not read the family a rule reads.
     *
     * @since 1.0.0
     */
    Unreadable,

    /**
     * A gauge was not observed within the age a rule allows.
     *
     * @since 1.0.0
     */
    Stale,

    /**
     * A statistic is not a number.
     *
     * @since 1.0.0
     */
    NotANumber,

    /**
     * A rule has not seen enough readings yet, such as for a rate over a window.
     *
     * @since 1.0.0
     */
    InsufficientSamples,

    /**
     * An indicator did not answer within its timeout.
     *
     * @since 1.0.0
     */
    TimedOut,

    /**
     * An indicator threw.
     *
     * @since 1.0.0
     */
    CheckFailed,

    /**
     * A group has no members.
     *
     * @since 1.0.0
     */
    EmptyGroup,
}
