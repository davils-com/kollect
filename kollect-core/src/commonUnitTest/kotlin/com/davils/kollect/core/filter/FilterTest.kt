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

package com.davils.kollect.core.filter

import com.davils.kollect.core.Refusal
import com.davils.kollect.core.Registration
import com.davils.kollect.core.fixture.counterOf
import com.davils.kollect.core.fixture.points
import com.davils.kollect.core.fixture.sums
import com.davils.kollect.core.fixture.testRegistry
import com.davils.kollect.core.limit.registryLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val METHOD: String = "method"
private const val REQUESTS: String = "http.requests"

class FilterTest : FunSpec({
    test("a denying filter refuses the family") {
        val registry = testRegistry { filter(MeterFilter.denyPrefix("jvm.")) }
        registry.counter("jvm.threads").shouldBeInstanceOf<Registration.Refused<*>>().refusal shouldBe Refusal.Denied
        registry.counter(REQUESTS).shouldBeInstanceOf<Registration.Declared<*>>()
        Refusal.Denied.toString() shouldBe "Denied"
    }

    test("a reshaping filter merges the series that differ only in the dropped labels") {
        val registry = testRegistry { filter(MeterFilter.dropLabels("path", "absent")) }
        val family = registry.counterOf(REQUESTS, METHOD, "path")
        family.with("GET", "/a").increment()
        family.with("GET", "/b").increment()
        val points = registry.points(REQUESTS)
        val keys = points.single().labels.keys
        keys.map { key -> key.value } shouldContainExactly listOf(METHOD)
        registry.sums(REQUESTS) shouldContainExactly listOf(2.0)
        val other = registry.counterOf("other", METHOD).descriptor.shouldNotBeNull()
        other.labelKeys.size shouldBe 1
    }

    test("an accepting filter keeps the family and stops the chain") {
        val registry = testRegistry {
            filter { FilterDecision.Accept }
            filter { FilterDecision.Deny }
        }
        registry.counter(REQUESTS).shouldBeInstanceOf<Registration.Declared<*>>()
    }

    test("a throwing filter refuses the family") {
        val registry = testRegistry { filter { error("broken") } }
        registry.counter(REQUESTS).shouldBeInstanceOf<Registration.Refused<*>>().refusal shouldBe Refusal.FilterFailed
        Refusal.FilterFailed.toString() shouldBe "FilterFailed"
    }

    test("a registry takes at most its limit of filters") {
        shouldThrow<IllegalArgumentException> {
            testRegistry {
                limits = registryLimits { maximumFilters = 1 }
                filter { FilterDecision.Neutral }
                filter { FilterDecision.Neutral }
            }
        }
    }

    test("decisions show themselves") {
        FilterDecision.Accept.toString() shouldBe "Accept"
        FilterDecision.Deny.toString() shouldBe "Deny"
        FilterDecision.Neutral.toString() shouldBe "Neutral"
        FilterDecision.Reshape(emptySet()).toString() shouldBe "Reshape([])"
    }
})
