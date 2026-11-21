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

package com.davils.kollect.core.limit

import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val VALIDATED: String = "validate, compare and show"
private const val CHANGED: Int = 2

class LimitsTest : FunSpec({
    context("registry limits") {
        test("start from the documented defaults") {
            val limits = RegistryLimits.Default
            limits.maximumFamilies shouldBe RegistryLimits.DEFAULT_FAMILIES
            limits.maximumSeriesPerFamily shouldBe RegistryLimits.DEFAULT_SERIES_PER_FAMILY
            limits.maximumSnapshotStall shouldBe 100.milliseconds
        }

        test("accept every hard ceiling and refuse one more") {
            registryLimits { maximumSeries = RegistryLimits.HARD_SERIES }.maximumSeries shouldBe
                RegistryLimits.HARD_SERIES
            shouldThrow<ValidationException> { registryLimits { maximumSeries = RegistryLimits.HARD_SERIES + 1 } }
            shouldThrow<ValidationException> { registryLimits { maximumLabels = 0 } }
            shouldThrow<ValidationException> { registryLimits { maximumSnapshotStall = Duration.ZERO } }
            shouldThrow<ValidationException> {
                registryLimits {
                    maximumSnapshotStall =
                        RegistryLimits.HARD_SNAPSHOT_STALL + 1.seconds
                }
            }
        }

        test("compare, hash and show by value") {
            val changed = registryLimits { maximumFamilies = CHANGED }
            registryLimits { } shouldBe RegistryLimits.Default
            registryLimits { }.hashCode() shouldBe RegistryLimits.Default.hashCode()
            changed shouldNotBe RegistryLimits.Default
            RegistryLimits.Default.equals("limits") shouldBe false
            changed.toString() shouldContain "families=$CHANGED"
        }

        test("start from their base") {
            val base = registryLimits { maximumLabels = CHANGED }
            registryLimits(base) { maximumFamilies = CHANGED }.maximumLabels shouldBe CHANGED
        }
    }

    context("exposition limits") {
        test(VALIDATED) {
            val limits = expositionLimits { maximumBytes = CHANGED }
            limits.maximumBytes shouldBe CHANGED
            expositionLimits(limits) { maximumSeries = CHANGED }.maximumBytes shouldBe CHANGED
            limits shouldNotBe ExpositionLimits.Default
            ExpositionLimits.Default.equals(1) shouldBe false
            expositionLimits { } shouldBe ExpositionLimits.Default
            expositionLimits { }.hashCode() shouldBe ExpositionLimits.Default.hashCode()
            limits.toString() shouldContain "bytes=$CHANGED"
            shouldThrow<ValidationException> { expositionLimits { maximumBytes = ExpositionLimits.HARD_BYTES + 1 } }
        }
    }

    context("export limits") {
        test(VALIDATED) {
            val limits = exportLimits { maximumAttempts = CHANGED }
            exportLimits(limits) { maximumQueuedBatches = CHANGED }.maximumAttempts shouldBe CHANGED
            limits shouldNotBe ExportLimits.Default
            ExportLimits.Default.equals(1) shouldBe false
            exportLimits { } shouldBe ExportLimits.Default
            exportLimits { }.hashCode() shouldBe ExportLimits.Default.hashCode()
            limits.toString() shouldContain "attempts=$CHANGED"
            shouldThrow<ValidationException> { exportLimits { maximumBackoff = ExportLimits.HARD_BACKOFF + 1.seconds } }
        }
    }

    context("ingest limits") {
        test(VALIDATED) {
            val limits = ingestLimits { maximumSources = CHANGED }
            ingestLimits(limits) { maximumSeries = CHANGED }.maximumSources shouldBe CHANGED
            limits shouldNotBe IngestLimits.Default
            IngestLimits.Default.equals(1) shouldBe false
            ingestLimits { } shouldBe IngestLimits.Default
            ingestLimits { }.hashCode() shouldBe IngestLimits.Default.hashCode()
            limits.toString() shouldContain "sources=$CHANGED"
            shouldThrow<ValidationException> {
                ingestLimits { maximumPointAge = IngestLimits.HARD_POINT_AGE + 1.seconds }
            }
        }
    }

    context("health limits") {
        test(VALIDATED) {
            val limits = healthLimits { maximumDepth = CHANGED }
            healthLimits(limits) { maximumMembers = CHANGED }.maximumDepth shouldBe CHANGED
            limits shouldNotBe HealthLimits.Default
            HealthLimits.Default.equals(1) shouldBe false
            healthLimits { } shouldBe HealthLimits.Default
            healthLimits { }.hashCode() shouldBe HealthLimits.Default.hashCode()
            limits.toString() shouldContain "depth=$CHANGED"
            shouldThrow<ValidationException> {
                healthLimits {
                    maximumCheckTimeout =
                        HealthLimits.HARD_CHECK_TIMEOUT + 1.seconds
                }
            }
        }
    }
})
