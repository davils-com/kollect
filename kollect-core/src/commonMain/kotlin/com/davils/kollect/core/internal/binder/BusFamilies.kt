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

package com.davils.kollect.core.internal.binder

import com.davils.kollect.core.meter.MeterKind

internal object BusFamilies {
    val ALL: List<BusFamily> = listOf(
        BusFamily.Topic(
            "konfetti.topic.published",
            MeterKind.Counter,
            "The envelopes published on a topic.",
        ) { it.published },
        BusFamily.Topic(
            "konfetti.topic.rejected",
            MeterKind.Counter,
            "The publications a topic rejected.",
        ) { it.rejected },
        BusFamily.Topic(
            "konfetti.topic.dispatched",
            MeterKind.Counter,
            "The envelopes a topic dispatched.",
        ) { it.dispatched },
        BusFamily.Topic("konfetti.topic.dropped", MeterKind.Counter, "The envelopes a topic dropped.") { it.dropped },
        BusFamily.Topic("konfetti.topic.queued", MeterKind.Gauge, "The envelopes queued in a topic.") { it.queued },
        BusFamily.Topic(
            "konfetti.topic.consumers",
            MeterKind.Gauge,
            "The consumers of a topic.",
        ) { it.consumers.toLong() },
        BusFamily.Consumer(
            "konfetti.consumer.received",
            MeterKind.Counter,
            "The envelopes a consumer received.",
        ) { it.received },
        BusFamily.Consumer(
            "konfetti.consumer.delivered",
            MeterKind.Counter,
            "The envelopes a consumer handled.",
        ) { it.delivered },
        BusFamily.Consumer(
            "konfetti.consumer.failed",
            MeterKind.Counter,
            "The attempts of a consumer that failed.",
        ) { it.failed },
        BusFamily.Consumer("konfetti.consumer.retried", MeterKind.Counter, "The retries of a consumer.") { it.retried },
        BusFamily.Consumer(
            "konfetti.consumer.dead_lettered",
            MeterKind.Counter,
            "The envelopes a consumer dead-lettered.",
        ) {
            it.deadLettered
        },
        BusFamily.Consumer(
            "konfetti.consumer.dropped",
            MeterKind.Counter,
            "The envelopes dropped for a consumer.",
        ) { it.dropped },
        BusFamily.Consumer(
            "konfetti.consumer.in_flight",
            MeterKind.Gauge,
            "The envelopes a consumer handles now.",
        ) { it.inFlight },
        BusFamily.Consumer(
            "konfetti.consumer.queued",
            MeterKind.Gauge,
            "The envelopes queued for a consumer.",
        ) { it.queued },
        BusFamily.Bus("konfetti.bus.observer_failures", MeterKind.Counter, "The observers of the bus that threw.") {
            it.observerFailures
        },
        BusFamily.Bus(
            "konfetti.bus.interceptor_failures",
            MeterKind.Counter,
            "The interceptors of the bus that threw.",
        ) {
            it.interceptorFailures
        },
    )
}
