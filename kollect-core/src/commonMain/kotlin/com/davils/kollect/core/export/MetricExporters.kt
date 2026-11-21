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
import com.davils.kollect.core.otlp.Resource
import kotlinx.coroutines.CoroutineScope

/**
 * Creates and starts an exporter that sends the snapshots of [registry] through [transport].
 *
 * @param scope the scope the exporter runs on; cancelling it stops the exporter without sending.
 * @param registry the registry to export.
 * @param resource the resource of the process.
 * @param transport sends the requests.
 * @param configure sets the interval, temporality, limits, entropy and observer.
 * @return the running exporter.
 * @since 1.0.0
 */
public fun metricExporter(
    scope: CoroutineScope,
    registry: MeterRegistry,
    resource: Resource,
    transport: Transport,
    configure: ExporterBuilder.() -> Unit = {},
): MetricExporter {
    val builder = ExporterBuilder().apply(configure)
    return MetricExporter(
        scope = scope,
        registry = registry,
        resource = resource,
        transport = transport,
        builder = builder,
    )
}
