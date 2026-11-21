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

package com.davils.kollect.core.otlp

import com.davils.kollect.core.Payload
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The OTLP requests one snapshot was encoded into.
 *
 * @param payloads the requests.
 * @property points the points the requests carry; a state set counts one point per state.
 * @property droppedPoints the points left out because a single metric would not fit into one request.
 * @param pointsPerPayload the points of each request.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class OtlpBatch internal constructor(
    payloads: List<Payload>,
    public val points: Int,
    public val droppedPoints: Int,
    internal val pointsPerPayload: List<Int> = emptyList(),
) {
    /**
     * The requests, each an `ExportMetricsServiceRequest` in Protocol Buffers within the limits it was encoded with.
     *
     * @since 1.0.0
     */
    public val payloads: List<Payload> = payloads.immutableList()

    /**
     * Shows the batch, such as `OtlpBatch(2 requests, 6120 points, 0 dropped)`.
     *
     * @return the batch as text.
     * @since 1.0.0
     */
    override fun toString(): String = "OtlpBatch(${payloads.size} requests, $points points, $droppedPoints dropped)"
}
