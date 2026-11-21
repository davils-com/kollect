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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableMap

private const val HASH_FACTOR: Int = 31

/**
 * The account of every point an exporter produced: `produced = delivered + rejected + dropped + pending` holds at
 * every moment.
 *
 * @property produced the points the exporter encoded.
 * @property delivered the points receivers took.
 * @property rejected the points receivers took but reported as rejected.
 * @property pending the points that wait in the queue.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class ExportAccounting internal constructor(
    public val produced: Long,
    public val delivered: Long,
    public val rejected: Long,
    dropped: Map<DropReason, Long>,
    public val pending: Long,
) {
    /**
     * The points dropped, by reason.
     *
     * @since 1.0.0
     */
    public val dropped: Map<DropReason, Long> = dropped.immutableMap()

    /**
     * Tells whether [other] is an equal account.
     *
     * @param other the object to compare with.
     * @return true if every count is equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ExportAccounting) return false
        return produced == other.produced &&
            delivered == other.delivered &&
            rejected == other.rejected &&
            dropped == other.dropped &&
            pending == other.pending
    }

    /**
     * Returns a hash consistent with [equals].
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(produced, delivered, rejected, dropped, pending)
        .fold(0) { hash, part -> HASH_FACTOR * hash + part.hashCode() }

    /**
     * Shows the account.
     *
     * @return the account as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "ExportAccounting(produced=$produced, delivered=$delivered, rejected=$rejected, dropped=$dropped, pending=$pending)"
}
