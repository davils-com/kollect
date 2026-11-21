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

import com.davils.kollect.core.internal.otlp.OtlpWriter
import com.davils.kollect.core.limit.ExportLimits
import com.davils.kollect.core.snapshot.MetricsSnapshot

/**
 * Encodes [snapshot] as OTLP requests in Protocol Buffers, `application/x-protobuf`, for an application to send to
 * an OTLP receiver such as the OpenTelemetry Collector, Grafana Alloy or Mimir with its own transport.
 *
 * Counters become monotonic sums and up-down counters non-monotonic sums with the temporality of their family,
 * gauges become gauges and histograms explicit bucket histograms; state sets and info families follow the Prometheus
 * compatibility rules of OpenTelemetry and carry `prometheus.type` in their metadata. The requests stay within
 * [ExportLimits.maximumPayloadBytes] and [ExportLimits.maximumPointsPerPayload]; a family with more points is split
 * over several requests. The encoding is deterministic.
 *
 * @param snapshot the snapshot.
 * @param resource the resource of the process.
 * @param limits the limits of the requests.
 * @return the requests.
 * @since 1.0.0
 */
public fun encodeOtlp(
    snapshot: MetricsSnapshot,
    resource: Resource,
    limits: ExportLimits = ExportLimits.Default,
): OtlpBatch = OtlpWriter(resource, limits).write(snapshot)
