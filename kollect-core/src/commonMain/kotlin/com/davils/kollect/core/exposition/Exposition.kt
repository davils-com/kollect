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

import com.davils.kollect.core.internal.exposition.ExpositionWriter
import com.davils.kollect.core.limit.ExpositionLimits
import com.davils.kollect.core.snapshot.MetricsSnapshot

/**
 * Writes snapshots in a format a scraper reads, for an application to serve at its metrics endpoint.
 *
 * An exposition writes the families of every snapshot in turn; a family whose Prometheus names collide with a family
 * written before it, whose text cannot be escaped or whose series collide after redaction is left out and reported,
 * never changed. The output is deterministic: the same snapshots give the same bytes on every platform. An exposition
 * beyond its [ExpositionLimits] is refused as a whole.
 *
 * kollect serves nothing itself: the application answers the scrape with the payload, its content type and its own
 * authentication and transport security.
 *
 * An exposition is immutable and safe to use from many threads.
 *
 * @property limits the limits of every exposition.
 * @property redaction which label values are redacted.
 * @since 1.0.0
 */
public class Exposition internal constructor(
    public val limits: ExpositionLimits,
    public val redaction: LabelRedaction,
) {
    /**
     * Writes [snapshots] in [format].
     *
     * @param format the format.
     * @param snapshots the snapshots, such as that of the registry and that of an OTLP ingestion; families of an
     *   earlier snapshot win a name collision.
     * @return the payload and its report, or a refusal if a limit would be exceeded.
     * @since 1.0.0
     */
    public fun write(format: ExpositionFormat, vararg snapshots: MetricsSnapshot): ExpositionResult =
        ExpositionWriter(format, limits, redaction).write(snapshots.asList())

    /**
     * Shows the exposition, such as `Exposition(ExpositionLimits(…))`.
     *
     * @return the exposition as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Exposition($limits)"
}
