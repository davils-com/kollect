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

package com.davils.kollect.core.export

/**
 * Receives what an exporter does, for an application to map to its own logs or alerts; kollect logs nothing itself.
 *
 * The observer is called on the coroutine of the exporter and must return quickly. An observer that throws is
 * ignored.
 *
 * @since 1.0.0
 */
public fun interface ExportObserver {
    /**
     * Receives [event].
     *
     * @param event what happened.
     * @since 1.0.0
     */
    public fun observe(event: ExportEvent)

    /**
     * The observer that receives nothing.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Receives nothing.
         *
         * @since 1.0.0
         */
        public val None: ExportObserver = ExportObserver { }
    }
}
