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

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.fixture.Mode
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.gaugeOf
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.fixture.text
import com.davils.kollect.core.fixture.withoutOwnFamilies
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.limit.expositionLimits
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.orThrow
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kore.testing.HostileFragments
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SNAPSHOT_SECONDS: Long = 1_700_000_000L
private const val GAUGE: Double = 1.5
private const val SMALL: Int = 10
private const val MAXIMUM_BYTES: String = "maximumBytes"
private const val VALUE: String = "value"

private const val DESCRIPTION: String = """The temperature of the "room"\now"""

private fun MeterRegistry.sample() {
    val requests = counterOf("http.requests", "method")
    requests.with("GET").increment()
    val items = upDownCounter("queue.items").orThrow()
    items.meter.add(2L)
    val temperature = gauge("room.temperature") {
        description = DESCRIPTION
        unit = MeasureUnit.CELSIUS
    }
    temperature.orThrow().meter.set(GAUGE)
    val timer = timer("job.duration") { bounds = ExplicitBounds.of(1.0) }
    timer.orThrow().meter.record(GAUGE.seconds)
    stateSet("worker.mode", Mode.Busy).orThrow()
    val build = info("build") { labelKeys("version") }
    build.orThrow().with("1.0")
}

private fun sampleSnapshot(): MetricsSnapshot {
    val registry = testRegistry()
    registry.sample()
    return registry.snapshot().withoutOwnFamilies()
}

private fun gaugeSnapshot(name: String, labels: Labels, description: String = ""): MetricsSnapshot {
    val at = Instant.fromEpochSeconds(SNAPSHOT_SECONDS)
    val descriptor = Descriptor(
        name = MetricName.of(name),
        kind = MeterKind.Gauge,
        unit = MeasureUnit.NONE,
        description = description,
        labelKeys = labels.keys,
    )
    val point = GaugePoint(labels = labels, start = at, value = 1.0, observedAt = at)
    return MetricsSnapshot(at, listOf(FamilySnapshot(descriptor, Temporality.Cumulative, listOf(point))))
}

private fun labelsOf(value: String): Labels = Labels(listOf(LabelKey.of(VALUE)), listOf(value))

private fun ExpositionResult.written(): ExpositionResult.Written = shouldBeInstanceOf<ExpositionResult.Written>()

private fun Exposition.written(format: ExpositionFormat, snapshot: MetricsSnapshot): ExpositionResult.Written =
    write(format, snapshot).written()

private fun Exposition.textOf(format: ExpositionFormat, snapshot: MetricsSnapshot): String {
    val bytes = written(format, snapshot).payload.copyBytes()
    return bytes.decodeToString()
}

private fun Exposition.problemOf(format: ExpositionFormat, snapshot: MetricsSnapshot): FamilyProblem {
    val report = written(format, snapshot).report
    return report.refused.single().problem
}

private fun Exposition.refusalOf(format: ExpositionFormat, snapshot: MetricsSnapshot): ExpositionResult.Refused {
    val result = write(format, snapshot)
    return result.shouldBeInstanceOf<ExpositionResult.Refused>()
}

class ExpositionTest : FunSpec({
    context("the Prometheus text format") {
        test("writes every kind of family") {
            val text = sampleSnapshot().text(ExpositionFormat.PrometheusText)
            text.lines() shouldContainExactly listOf(
                "# TYPE build_info gauge",
                """build_info{version="1.0"} 1""",
                "# TYPE http_requests_total counter",
                """http_requests_total{method="GET"} 1""",
                "# TYPE job_duration_seconds histogram",
                """job_duration_seconds_bucket{le="1.0"} 0""",
                """job_duration_seconds_bucket{le="+Inf"} 1""",
                "job_duration_seconds_count 1",
                "job_duration_seconds_sum 1.5",
                "# TYPE queue_items gauge",
                "queue_items 2",
                """# HELP room_temperature_celsius The temperature of the "room"\\now""",
                "# TYPE room_temperature_celsius gauge",
                "room_temperature_celsius 1.5",
                "# TYPE worker_mode gauge",
                """worker_mode{worker_mode="Idle"} 0""",
                """worker_mode{worker_mode="Busy"} 1""",
                "",
            )
        }
    }

    context("OpenMetrics") {
        test("writes types, units, created samples and the end") {
            val text = sampleSnapshot().text(ExpositionFormat.OpenMetrics)
            text shouldContain "# TYPE http_requests counter"
            text shouldContain """http_requests_created{method="GET"} 1700000000"""
            text shouldContain "# TYPE room_temperature_celsius gauge"
            text shouldContain "# UNIT room_temperature_celsius celsius"
            text shouldContain """# HELP room_temperature_celsius The temperature of the \"room\"\\now"""
            text shouldContain "# TYPE worker_mode stateset"
            text shouldContain "# TYPE build info"
            text shouldContain "job_duration_seconds_created 1700000000"
            text.endsWith("# EOF\n") shouldBe true
        }
    }

    context("an exposition") {
        test("escapes label values exactly once") {
            val hostile = HostileFragments.all.filter { fragment -> TextRules.firstInvalidIndex(fragment) < 0 }
            val value = hostile.joinToString(separator = "") + """\""""
            val snapshot = gaugeSnapshot("hostile.value", labelsOf(value))
            val text = exposition().textOf(ExpositionFormat.PrometheusText, snapshot)
            text.lines().count { line -> line.startsWith("hostile_value{") } shouldBe 1
        }

        test("refuses a family whose text no format can escape") {
            val written = exposition().written(
                ExpositionFormat.OpenMetrics,
                gaugeSnapshot("broken.value", labelsOf("a\rb")),
            )
            val refused = written.report.refused.single()
            refused.problem shouldBe FamilyProblem.InvalidText
            refused.toString() shouldBe "RefusedFamily(broken.value, InvalidText)"
            written.report.families shouldBe 0
            val broken = gaugeSnapshot("broken.help", Labels.EMPTY, "a\u0000")
            exposition().problemOf(ExpositionFormat.OpenMetrics, broken) shouldBe FamilyProblem.InvalidText
        }

        test("refuses a family whose names collide with a family written before") {
            val first = gaugeSnapshot("queue.size", Labels.EMPTY)
            val second = gaugeSnapshot("queue_size", Labels.EMPTY)
            val result = exposition().write(ExpositionFormat.PrometheusText, first, second)
            val report = result.written().report
            report.refused.single().problem shouldBe FamilyProblem.NameCollision
            report.families shouldBe 1
            report.series shouldBe 1
            report.toString() shouldBe "ExpositionReport(1 families, 1 series, 1 refused)"
        }

        test("redacts sensitive labels and refuses series that collide after redaction") {
            val registry = testRegistry()
            val family = registry.gaugeOf("session.age", "session.token")
            family.with("secret-one").set(1.0)
            val text = exposition().textOf(ExpositionFormat.PrometheusText, registry.snapshot().withoutOwnFamilies())
            text shouldContain """session_token="****""""
            text shouldNotContain "secret-one"
            family.with("secret-two").set(2.0)
            val collided = registry.snapshot().withoutOwnFamilies()
            exposition().problemOf(ExpositionFormat.PrometheusText, collided) shouldBe FamilyProblem.DuplicateSeries
            val open = exposition { redaction = LabelRedaction.None }
            val report = open.written(ExpositionFormat.PrometheusText, collided).report
            report.refused.size shouldBe 0
        }

        test("refuses the whole exposition beyond its limits") {
            val snapshot = sampleSnapshot()
            val small = exposition { limits = expositionLimits { maximumBytes = SMALL } }
            small.refusalOf(ExpositionFormat.PrometheusText, snapshot).limit shouldBe MAXIMUM_BYTES
            val few = exposition { limits = expositionLimits { maximumSeries = 1 } }
            few.refusalOf(ExpositionFormat.OpenMetrics, snapshot).toString() shouldBe "Refused(maximumSeries)"
            val single = gaugeSnapshot("a", Labels.EMPTY)
            val exact = exposition().written(ExpositionFormat.OpenMetrics, single).payload.size
            val tight = exposition { limits = expositionLimits { maximumBytes = exact - 1 } }
            tight.refusalOf(ExpositionFormat.OpenMetrics, single).limit shouldBe MAXIMUM_BYTES
        }

        test("shows itself and its results") {
            exposition().toString() shouldContain "ExpositionLimits"
            val result = exposition().write(ExpositionFormat.PrometheusText)
            val written = result.written()
            written.toString() shouldContain "Written("
            LabelRedaction.Sensitive.redacts(LabelKey.of("api_key")) shouldBe true
        }
    }
})
