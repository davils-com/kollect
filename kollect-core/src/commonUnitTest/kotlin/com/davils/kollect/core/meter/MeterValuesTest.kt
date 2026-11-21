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

package com.davils.kollect.core.meter

import com.davils.kollect.core.name.LabelKey
import com.davils.kollect.core.name.MetricName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val REQUEST: String = "request"
private const val QUEUE: String = "The size of a queue."
private const val FINE: Double = 0.005
private const val BOUND: Double = 2.5

private fun descriptorWith(description: String): Descriptor = Descriptor(
    name = MetricName.of("queue.size"),
    kind = MeterKind.Gauge,
    unit = MeasureUnit.NONE,
    description = description,
    labelKeys = listOf(LabelKey.of("queue")),
)

class MeterValuesTest : FunSpec({
    context("units") {
        test("carry their code and suffix") {
            MeasureUnit.SECONDS.ucum shouldBe "s"
            MeasureUnit.SECONDS.prometheusSuffix shouldBe "seconds"
            MeasureUnit.NONE.toString() shouldBe "none"
            MeasureUnit.BYTES.toString() shouldBe "By"
            listOf(
                MeasureUnit.PERCENT,
                MeasureUnit.CELSIUS,
                MeasureUnit.VOLTS,
                MeasureUnit.AMPERES,
                MeasureUnit.WATTS,
                MeasureUnit.JOULES,
                MeasureUnit.HERTZ,
                MeasureUnit.METERS,
                MeasureUnit.RATIO,
            ).forEach { unit -> MeasureUnit.ofUcumOrNull(unit.ucum) shouldBe unit }
        }

        test("name what is counted") {
            val requests = MeasureUnit.annotation(REQUEST)
            requests.ucum shouldBe "{request}"
            requests.prometheusSuffix.shouldBeNull()
            MeasureUnit.ofUcumOrNull("{request}") shouldBe requests
            MeasureUnit.ofUcumOrNull("{Request}").shouldBeNull()
            MeasureUnit.ofUcumOrNull("{}").shouldBeNull()
            MeasureUnit.ofUcumOrNull("parsec").shouldBeNull()
            shouldThrow<IllegalArgumentException> { MeasureUnit.annotation("Request") }
        }

        test("compare by value") {
            MeasureUnit.annotation(REQUEST) shouldBe MeasureUnit.annotation(REQUEST)
            MeasureUnit.annotation(REQUEST).hashCode() shouldBe MeasureUnit.annotation(REQUEST).hashCode()
            MeasureUnit.SECONDS shouldNotBe MeasureUnit.BYTES
            MeasureUnit.SECONDS.equals("s") shouldBe false
        }
    }

    context("explicit bounds") {
        test("are finite and strictly increasing") {
            ExplicitBounds.of(1.0, BOUND).values shouldContainExactly listOf(1.0, BOUND)
            ExplicitBounds.of(1.0).bucketCount shouldBe 2
            ExplicitBounds.ofOrNull(listOf(BOUND, 1.0)).shouldBeNull()
            ExplicitBounds.ofOrNull(listOf(Double.NaN)).shouldBeNull()
            ExplicitBounds.ofOrNull(listOf(1.0))?.values shouldBe listOf(1.0)
            shouldThrow<IllegalArgumentException> { ExplicitBounds.of(1.0, 1.0) }
            shouldThrow<IllegalArgumentException> { ExplicitBounds.of(Double.POSITIVE_INFINITY) }
        }

        test("compare and show by value") {
            ExplicitBounds.of(1.0) shouldBe ExplicitBounds.of(1.0)
            ExplicitBounds.of(1.0).hashCode() shouldBe ExplicitBounds.of(1.0).hashCode()
            ExplicitBounds.of(1.0) shouldNotBe ExplicitBounds.SECONDS
            ExplicitBounds.DEFAULT.equals("bounds") shouldBe false
            ExplicitBounds.of(FINE, BOUND).toString() shouldBe "ExplicitBounds[0.005, 2.5]"
        }
    }

    context("descriptors") {
        test("compare by every part and show the description escaped") {
            val descriptor = descriptorWith(QUEUE)
            descriptor shouldBe descriptorWith(QUEUE)
            descriptor.hashCode() shouldBe descriptorWith(QUEUE).hashCode()
            descriptor shouldNotBe descriptorWith("Another.")
            descriptor.equals("descriptor") shouldBe false
            descriptorWith("line\nbreak").toString() shouldContain "\\n"
        }
    }

    context("rejections") {
        test("label their reason") {
            Rejection.entries.map(Rejection::label) shouldContainExactly
                listOf("negative", "not_finite", "too_large", "invalid_labels", "untracked", "duplicate")
        }
    }
})
