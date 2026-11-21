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

/**
 * Whether the sums and histograms of a family accumulate since the start of a series or only since the last export.
 *
 * @since 1.0.0
 */
public enum class Temporality {
    /**
     * Since the start of each series; what a registry and Prometheus hold.
     *
     * @since 1.0.0
     */
    Cumulative,

    /**
     * Since the last export; what a [DeltaConverter] produces.
     *
     * @since 1.0.0
     */
    Delta,
}
