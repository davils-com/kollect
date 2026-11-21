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

package com.davils.kollect.core.name

import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kore.testing.HostileFragments
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain

private const val METHOD: String = "method"
private const val VALUE: String = "value"
private const val REQUESTS: String = "http.server.requests"

private fun descriptorOf(name: String, kind: MeterKind, unit: MeasureUnit = MeasureUnit.NONE): Descriptor = Descriptor(
    name = MetricName.of(name),
    kind = kind,
    unit = unit,
    description = "",
    labelKeys = emptyList(),
    bounds = ExplicitBounds.DEFAULT.takeIf { kind == MeterKind.Histogram },
)

class NamesTest : FunSpec({
    context("metric names") {
        test("follow their grammar") {
            MetricName.of(REQUESTS).value shouldBe REQUESTS
            MetricName.ofOrNull("Http.Requests").shouldBeNull()
            MetricName.ofOrNull("http..requests").shouldBeNull()
            MetricName.ofOrNull("http__requests").shouldBeNull()
            MetricName.ofOrNull("").shouldBeNull()
            MetricName.ofOrNull("a".repeat(MetricName.MAXIMUM_LENGTH + 1)).shouldBeNull()
            shouldThrow<IllegalArgumentException> { MetricName.of("{bad}") }
        }

        test("compare and show as text") {
            MetricName.of("a.b") shouldBeLessThan MetricName.of("a.c")
            MetricName.of(REQUESTS).toString() shouldBe REQUESTS
        }
    }

    context("label keys") {
        test("follow their grammar and know the reserved keys") {
            LabelKey.of(METHOD).isReserved shouldBe false
            LabelKey.of("le").isReserved shouldBe true
            LabelKey.of("quantile").isReserved shouldBe true
            LabelKey.OVERFLOW.isReserved shouldBe true
            LabelKey.ofOrNull("Method").shouldBeNull()
            shouldThrow<IllegalArgumentException> { LabelKey.of("") }
            LabelKey.of("a") shouldBeLessThan LabelKey.of("b")
            LabelKey.of("a").toString() shouldBe "a"
        }
    }

    context("labels") {
        test("give the value of a key") {
            val labels = Labels(listOf(LabelKey.of(METHOD)), listOf("GET"))
            labels[LabelKey.of(METHOD)] shouldBe "GET"
            labels[LabelKey.of("path")].shouldBeNull()
            labels.size shouldBe 1
            Labels.EMPTY.size shouldBe 0
        }

        test("order by values and then by keys") {
            val first = Labels(listOf(LabelKey.of("a")), listOf("x"))
            val second = Labels(listOf(LabelKey.of("a")), listOf("y"))
            val other = Labels(listOf(LabelKey.of("b")), listOf("x"))
            first shouldBeLessThan second
            first shouldBeLessThan other
            Labels.EMPTY shouldBeLessThan first
            first.compareTo(first) shouldBe 0
        }

        test("compare by value and show values escaped") {
            val hostile = HostileFragments.all.joinToString(separator = "")
            val labels = Labels(listOf(LabelKey.of(VALUE)), listOf(hostile))
            labels shouldBe Labels(listOf(LabelKey.of(VALUE)), listOf(hostile))
            labels shouldNotBe Labels.EMPTY
            labels.equals("labels") shouldBe false
            labels.hashCode() shouldBe Labels(listOf(LabelKey.of(VALUE)), listOf(hostile)).hashCode()
            labels.toString() shouldNotContain "\n"
        }

        test("refuse a value count that differs from the key count") {
            shouldThrow<IllegalArgumentException> { Labels(listOf(LabelKey.of("a")), emptyList()) }
        }
    }

    context("the Prometheus naming") {
        test("adds the suffix of the unit once") {
            PrometheusNaming.familyName(descriptorOf("http.duration", MeterKind.Gauge, MeasureUnit.SECONDS)) shouldBe
                "http_duration_seconds"
            PrometheusNaming.familyName(
                descriptorOf("http.duration_seconds", MeterKind.Gauge, MeasureUnit.SECONDS),
            ) shouldBe
                "http_duration_seconds"
            PrometheusNaming.familyName(descriptorOf("queue.size", MeterKind.Gauge)) shouldBe "queue_size"
            PrometheusNaming.labelName(LabelKey.of("http.request.method")) shouldBe "http_request_method"
        }

        test("names every sample of a kind") {
            PrometheusNaming.sampleNames(descriptorOf("jobs", MeterKind.Counter)) shouldContainExactly
                listOf("jobs_total", "jobs_created")
            PrometheusNaming.sampleNames(descriptorOf("jobs", MeterKind.Histogram)) shouldContainExactly
                listOf("jobs_bucket", "jobs_count", "jobs_sum", "jobs_created")
            PrometheusNaming.sampleNames(
                descriptorOf("build", MeterKind.Info),
            ) shouldContainExactly listOf("build_info")
            PrometheusNaming.sampleNames(descriptorOf("state", MeterKind.StateSet)) shouldContainExactly listOf("state")
        }

        test("reserves the suffixes of samples") {
            PrometheusNaming.isReserved(descriptorOf("jobs_total", MeterKind.Gauge)) shouldBe true
            PrometheusNaming.isReserved(descriptorOf("jobs", MeterKind.Gauge)) shouldBe false
        }
    }
})
