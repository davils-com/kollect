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

package com.davils.kollect.core.trace

/**
 * How the work of a span ended.
 *
 * @property label the value of the `outcome` label of a metered tracer.
 * @since 1.0.0
 */
public enum class SpanOutcome(public val label: String) {
    /**
     * The work succeeded.
     *
     * @since 1.0.0
     */
    Ok("ok"),

    /**
     * The work failed.
     *
     * @since 1.0.0
     */
    Error("error"),
}
