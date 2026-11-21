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

import com.davils.kollect.core.name.LabelKey

private val SENSITIVE: List<String> = listOf(
    "password",
    "secret",
    "token",
    "authorization",
    "cookie",
    "session",
    "api_key",
)

/**
 * Decides which label values an exposition replaces with the redaction marker of Kore, `****`, so a secret that
 * reached a label does not leave the process.
 *
 * @since 1.0.0
 */
public fun interface LabelRedaction {
    /**
     * Tells whether the values of the label [key] are redacted.
     *
     * @param key the key.
     * @return true to write the redaction marker instead of the value.
     * @since 1.0.0
     */
    public fun redacts(key: LabelKey): Boolean

    /**
     * The redactions kollect offers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Redacts every label whose key contains `password`, `secret`, `token`, `authorization`, `cookie`, `session`
         * or `api_key`.
         *
         * @since 1.0.0
         */
        public val Sensitive: LabelRedaction = LabelRedaction { key -> SENSITIVE.any(key.value::contains) }

        /**
         * Redacts nothing.
         *
         * @since 1.0.0
         */
        public val None: LabelRedaction = LabelRedaction { false }
    }
}
