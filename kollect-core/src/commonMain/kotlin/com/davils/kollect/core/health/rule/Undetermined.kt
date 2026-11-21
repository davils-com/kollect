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

/**
 * What a rule decides when it cannot decide: no data, a stale gauge, NaN, too few readings or an unreadable family.
 *
 * A rule never decides such a case as Up.
 *
 * @since 1.0.0
 */
public enum class Undetermined {
    /**
     * Down; the default, for members that must prove they work.
     *
     * @since 1.0.0
     */
    Down,

    /**
     * Degraded, for members whose silence is expected, such as a counter of rare events.
     *
     * @since 1.0.0
     */
    Degraded,
}
