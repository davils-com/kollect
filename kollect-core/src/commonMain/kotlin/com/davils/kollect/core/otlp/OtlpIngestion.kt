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
import com.davils.kollect.core.internal.otlp.FamilyRule
import com.davils.kollect.core.internal.otlp.IngestCounts
import com.davils.kollect.core.internal.otlp.IngestStore
import com.davils.kollect.core.internal.otlp.OtlpFields
import com.davils.kollect.core.internal.otlp.OtlpReader
import com.davils.kollect.core.internal.otlp.Oversized
import com.davils.kollect.core.internal.wire.ProtoWriter
import com.davils.kollect.core.limit.IngestLimits
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kore.core.failure.contained
import kotlin.time.Clock

private const val PROTOBUF: String = "application/x-protobuf"
private const val PARTIAL_MESSAGE: String = "some data points were rejected"

/**
 * Takes OTLP metric requests from clients, such as browser and desktop applications, so a server re-exposes their
 * metrics with its own, for Prometheus to scrape.
 *
 * kollect serves no route: the application authenticates the caller, maps it to a [Source], decompresses the body
 * within [IngestLimits.maximumPayloadBytes] and passes it to [ingest]. The ingestion reads every request as hostile:
 * it checks every length before it reads, bounds the work of every request, accepts only the families it was told to,
 * keeps every source in its own series labelled `kollect.source`, rejects reserved attributes, points out of time and
 * replayed deltas, limits the series and the rate of every source, and answers without echoing the request. The
 * series of the instances of a source are summed; when an instance falls silent its sums stay in the totals, so
 * counters stay monotonic.
 *
 * An ingestion is safe to use from many threads; it starts no timer and expires series when it is used.
 *
 * @property limits the limits of the ingestion.
 * @since 1.0.0
 */
public class OtlpIngestion internal constructor(
    private val clock: Clock,
    public val limits: IngestLimits,
    rules: List<FamilyRule>,
    anyPrefix: String?,
) {
    private val store: IngestStore = IngestStore(limits, rules, anyPrefix)

    /**
     * The number of sources the ingestion holds series of.
     *
     * @since 1.0.0
     */
    public val sourceCount: Int get() = store.sourceCount

    /**
     * Ingests one request of [source].
     *
     * @param source who sent the request, as the application authenticated it.
     * @param contentType the content type of the request; only `application/x-protobuf` is taken.
     * @param body the body of the request, already decompressed.
     * @return the verdict, the body to answer with and the account of the points.
     * @since 1.0.0
     */
    public fun ingest(source: Source, contentType: String, body: ByteArray): Ingestion {
        val counts = IngestCounts()
        val verdict = refusalOf(contentType, body) ?: decodedAndMerged(source, body, counts)
        return Ingestion(
            verdict = verdict,
            response = responseOf(counts),
            accepted = counts.accepted,
            rejected = counts.rejections(),
        )
    }

    /**
     * Takes a snapshot of every series the ingestion holds, for an [com.davils.kollect.core.exposition.Exposition]
     * to write after the snapshot of the registry.
     *
     * @return the snapshot.
     * @since 1.0.0
     */
    public fun snapshot(): MetricsSnapshot = store.snapshot(clock.now())

    /**
     * Drops every series of [source], such as after its access was revoked.
     *
     * @param source the source.
     * @return true if the ingestion held series of it.
     * @since 1.0.0
     */
    public fun forget(source: Source): Boolean = store.forget(source.id)

    /**
     * Shows the ingestion, such as `OtlpIngestion(12 sources)`.
     *
     * @return the ingestion as text.
     * @since 1.0.0
     */
    override fun toString(): String = "OtlpIngestion($sourceCount sources)"

    private fun refusalOf(contentType: String, body: ByteArray): IngestVerdict? {
        val mediaType = contentType.substringBefore(';').trim()
        if (!mediaType.equals(PROTOBUF, ignoreCase = true)) return IngestVerdict.UnsupportedContent
        return IngestVerdict.TooLarge.takeIf { body.size > limits.maximumPayloadBytes }
    }

    private fun decodedAndMerged(source: Source, body: ByteArray, counts: IngestCounts): IngestVerdict {
        val decoded = contained { OtlpReader(limits).read(body) }
        val resources = decoded.getOrNull() ?: return verdictOf(decoded.exceptionOrNull())
        val refusal = store.merge(source = source.id, resources = resources, now = clock.now(), counts = counts)
        return refusal ?: IngestVerdict.Accepted.takeIf { counts.rejectedTotal == 0L }
            ?: IngestVerdict.PartiallyAccepted
    }

    private fun verdictOf(failure: Throwable?): IngestVerdict =
        IngestVerdict.TooLarge.takeIf { failure is Oversized } ?: IngestVerdict.Malformed

    private fun responseOf(counts: IngestCounts): Payload {
        val writer = ProtoWriter()
        val rejected = counts.rejectedTotal
        if (rejected > 0L) {
            writer.message(OtlpFields.RESPONSE_PARTIAL_SUCCESS) {
                uint64(OtlpFields.PARTIAL_REJECTED, rejected)
                string(OtlpFields.PARTIAL_MESSAGE, PARTIAL_MESSAGE)
            }
        }
        return Payload(writer.toByteArray(), OtlpFields.CONTENT_TYPE)
    }
}
