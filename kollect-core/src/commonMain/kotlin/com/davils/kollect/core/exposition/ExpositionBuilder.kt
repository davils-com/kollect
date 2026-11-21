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

import com.davils.kollect.core.limit.ExpositionLimits
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Configures an [Exposition] while [exposition] builds it.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ExpositionBuilder internal constructor() {
    /**
     * The limits of every exposition; [ExpositionLimits.Default] by default.
     *
     * @since 1.0.0
     */
    public var limits: ExpositionLimits = ExpositionLimits.Default

    /**
     * Which label values are redacted; [LabelRedaction.Sensitive] by default.
     *
     * @since 1.0.0
     */
    public var redaction: LabelRedaction = LabelRedaction.Sensitive
}
