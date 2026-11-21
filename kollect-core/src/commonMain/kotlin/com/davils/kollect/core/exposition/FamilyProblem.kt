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

package com.davils.kollect.core.exposition

/**
 * Why an exposition left a family out.
 *
 * @since 1.0.0
 */
public enum class FamilyProblem {
    /**
     * Its Prometheus names collide with those of a family written before it.
     *
     * @since 1.0.0
     */
    NameCollision,

    /**
     * A label value or its description holds a control character or a lone surrogate, which no exposition format can
     * escape.
     *
     * @since 1.0.0
     */
    InvalidText,

    /**
     * Two of its series have the same labels after redaction.
     *
     * @since 1.0.0
     */
    DuplicateSeries,
}
