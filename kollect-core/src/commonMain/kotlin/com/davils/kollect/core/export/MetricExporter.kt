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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.internal.export.Backoff
import com.davils.kollect.core.internal.export.DeltaConverter
import com.davils.kollect.core.internal.export.ExportLedger
import com.davils.kollect.core.internal.export.QueuedRequest
import com.davils.kollect.core.internal.otlp.OtlpWriter
import com.davils.kollect.core.limit.ExportLimits
import com.davils.kollect.core.otlp.Resource
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Sends the snapshots of a registry to an OTLP receiver at a fixed interval, through a [Transport] the application
 * chose; for processes that cannot be scraped, such as browser and desktop applications.
 *
 * The exporter runs one coroutine on the scope it was created with, chooses no dispatcher and starts no thread, so it
 * runs in a browser as well as on a server. It sends one request at a time. A request that fails in a way that may
 * pass is retried with exponential backoff and full jitter within [ExportLimits.maximumAttempts]; a throttled one
 * waits as long as the receiver asks, within [ExportLimits.maximumRetryAfter]. With cumulative temporality a newer
 * snapshot replaces requests that were not sent yet; with deltas the queue drops its oldest request when it is full.
 * Every point is accounted for in [accounting].
 *
 * @since 1.0.0
 */
public class MetricExporter internal constructor(
    scope: CoroutineScope,
    private val registry: MeterRegistry,
    resource: Resource,
    private val transport: Transport,
    builder: ExporterBuilder,
) {
    private val limits: ExportLimits = builder.limits
    private val observer: ExportObserver = builder.observer
    private val writer: OtlpWriter = OtlpWriter(resource, limits)
    private val isDelta: Boolean = builder.temporality == Temporality.Delta
    private val convert: (MetricsSnapshot) -> MetricsSnapshot = DeltaConverter()::convert.takeIf { isDelta } ?: { it }
    private val backoff: Backoff = Backoff(limits.maximumBackoff, builder.entropy)
    private val interval: Duration = maxOf(a = builder.interval, b = 1.seconds)
    private val ledger: ExportLedger = ExportLedger()
    private val mutex: Mutex = Mutex()
    private val queue: ArrayDeque<QueuedRequest> = ArrayDeque()
    private val worker: Job = scope.launch { exportPeriodically(this) }

    /**
     * Returns the account of every point produced so far.
     *
     * @return the account.
     * @since 1.0.0
     */
    public fun accounting(): ExportAccounting = ledger.accounting()

    /**
     * Takes a snapshot now and sends it with every waiting request, for at most [timeout].
     *
     * @param timeout how long the flush may take.
     * @return the account after the flush.
     * @since 1.0.0
     */
    public suspend fun flush(timeout: Duration): ExportAccounting {
        withTimeoutOrNull(timeout) { exportOnce() }
        return accounting()
    }

    /**
     * Stops the exporter: it takes a last snapshot and sends what it can within [timeout], then drops the rest with
     * [DropReason.ShutdownDeadline].
     *
     * @param timeout how long the shutdown may send.
     * @return the final account.
     * @since 1.0.0
     */
    public suspend fun shutdown(timeout: Duration): ExportAccounting {
        worker.cancelAndJoin()
        withTimeoutOrNull(timeout) { exportOnce() }
        mutex.withLock { dropAll(DropReason.ShutdownDeadline) }
        return accounting()
    }

    /**
     * Shows the exporter, such as `MetricExporter(ExportAccounting(…))`.
     *
     * @return the exporter as text.
     * @since 1.0.0
     */
    override fun toString(): String = "MetricExporter(${accounting()})"

    private suspend fun exportPeriodically(scope: CoroutineScope) {
        while (scope.isActive) {
            delay(interval)
            exportOnce()
        }
    }

    private suspend fun exportOnce() {
        mutex.withLock {
            enqueue(collected())
            drain()
        }
    }

    private fun collected(): List<QueuedRequest> {
        val snapshot = registry.snapshot()
        val converted = convert(snapshot)
        val batch = writer.write(converted)
        ledger.produced((batch.points + batch.droppedPoints).toLong())
        if (batch.droppedPoints > 0) drop(batch.droppedPoints.toLong(), DropReason.TooLarge)
        return batch.payloads.zip(batch.pointsPerPayload) { payload, points -> QueuedRequest(payload, points.toLong()) }
    }

    private fun enqueue(requests: List<QueuedRequest>) {
        if (!isDelta) dropAll(DropReason.Superseded)
        queue.addAll(requests)
        while (queue.size > limits.maximumQueuedBatches) drop(queue.removeFirst().points, DropReason.QueueFull)
    }

    private suspend fun drain() {
        while (queue.isNotEmpty()) {
            sent(queue.first())
            queue.removeFirst()
        }
    }

    private suspend fun sent(request: QueuedRequest) {
        var attempt = 0
        var isSettled = false
        while (!isSettled) {
            attempt += 1
            val delivery = attempted { transport.send(request.payload) }.getOrNull() ?: Delivery.Retryable
            isSettled = settled(request, delivery, attempt)
        }
    }

    private suspend fun settled(request: QueuedRequest, delivery: Delivery, attempt: Int): Boolean {
        val wait = when (delivery) {
            is Delivery.Accepted -> null.also { deliver(request, delivery.rejectedPoints) }

            is Delivery.Rejected -> null.also { drop(request.points, DropReason.Rejected) }

            is Delivery.Retryable -> backoff.delayOf(attempt)

            is Delivery.Throttled -> minOf(
                a = delivery.retryAfter ?: backoff.delayOf(attempt),
                b = limits.maximumRetryAfter,
            )
        }
        if (wait == null) return true
        return retried(request, attempt, wait)
    }

    private suspend fun retried(request: QueuedRequest, attempt: Int, wait: Duration): Boolean {
        val isExhausted = attempt >= limits.maximumAttempts
        if (isExhausted) drop(request.points, DropReason.RetriesExhausted)
        if (isExhausted) return true
        notify(ExportEvent.Retrying(attempt, wait))
        delay(wait)
        return false
    }

    private fun deliver(request: QueuedRequest, rejectedPoints: Long) {
        ledger.delivered(request.points, rejectedPoints)
        val refused = rejectedPoints.coerceIn(minimumValue = 0L, maximumValue = request.points)
        notify(ExportEvent.Delivered(points = request.points, rejectedPoints = refused))
    }

    private fun dropAll(reason: DropReason) {
        queue.forEach { request -> drop(request.points, reason) }
        queue.clear()
    }

    private fun drop(points: Long, reason: DropReason) {
        ledger.dropped(points, reason)
        notify(ExportEvent.Dropped(points, reason))
    }

    private fun notify(event: ExportEvent) {
        contained { observer.observe(event) }
    }
}
