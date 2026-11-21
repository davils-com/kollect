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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val PROMETHEUS_2: String =
    "application/openmetrics-text;version=1.0.0,application/openmetrics-text;version=0.0.1;q=0.75," +
        "text/plain;version=0.0.4;q=0.5,*/*;q=0.1"
private const val PROMETHEUS_3: String =
    "application/vnd.google.protobuf;proto=io.prometheus.client.MetricFamily;encoding=delimited;q=0.6," +
        "application/openmetrics-text;version=1.0.0;escaping=allow-utf-8;q=0.5," +
        "text/plain;version=1.0.0;escaping=allow-utf-8;q=0.3,text/plain;version=0.0.4;q=0.2,*/*;q=0.1"

class NegotiationTest : FunSpec({
    test("chooses OpenMetrics when a scraper prefers it") {
        negotiateExposition(PROMETHEUS_2) shouldBe ExpositionFormat.OpenMetrics
        negotiateExposition(PROMETHEUS_3) shouldBe ExpositionFormat.OpenMetrics
        negotiateExposition("application/openmetrics-text") shouldBe ExpositionFormat.OpenMetrics
        negotiateExposition("APPLICATION/OPENMETRICS-TEXT; version=\"1.0.0\"") shouldBe ExpositionFormat.OpenMetrics
    }

    test("chooses the Prometheus text format otherwise") {
        negotiateExposition(null) shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("text/plain") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("application/openmetrics-text;version=2.0.0") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("application/openmetrics-text;q=0") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition(
            "application/openmetrics-text;q=0.2,text/plain;q=0.9",
        ) shouldBe ExpositionFormat.PrometheusText
    }

    test("ignores what it cannot read") {
        negotiateExposition("application/openmetrics-text;q=oops") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("application/openmetrics-text;q=2") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("application/openmetrics-text;q=-1") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition(";;;,,,=;=") shouldBe ExpositionFormat.PrometheusText
        negotiateExposition("x".repeat(LONG) + ",application/openmetrics-text") shouldBe ExpositionFormat.PrometheusText
    }

    test("names the content types of its formats") {
        ExpositionFormat.PrometheusText.contentType shouldBe "text/plain; version=0.0.4; charset=utf-8"
        ExpositionFormat.OpenMetrics.contentType shouldBe "application/openmetrics-text; version=1.0.0; charset=utf-8"
    }
})

private const val LONG: Int = 5_000
