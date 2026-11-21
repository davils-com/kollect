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

/**
 * Builds validated registry limits that start from [base] and change what [configure] sets.
 *
 * @param base the limits every property starts from; [RegistryLimits.Default] by default.
 * @param configure sets the limits that differ from [base].
 * @return the limits.
 * @throws com.davils.kore.core.validation.ValidationException if a limit is below 1, not positive or above its hard
 *   ceiling.
 * @since 1.0.0
 */
public fun registryLimits(
    base: RegistryLimits = RegistryLimits.Default,
    configure: RegistryLimitsBuilder.() -> Unit,
): RegistryLimits {
    val builder = RegistryLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}

/**
 * Builds validated exposition limits that start from [base] and change what [configure] sets.
 *
 * @param base the limits every property starts from; [ExpositionLimits.Default] by default.
 * @param configure sets the limits that differ from [base].
 * @return the limits.
 * @throws com.davils.kore.core.validation.ValidationException if a limit is below 1 or above its hard ceiling.
 * @since 1.0.0
 */
public fun expositionLimits(
    base: ExpositionLimits = ExpositionLimits.Default,
    configure: ExpositionLimitsBuilder.() -> Unit,
): ExpositionLimits {
    val builder = ExpositionLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}

/**
 * Builds validated export limits that start from [base] and change what [configure] sets.
 *
 * @param base the limits every property starts from; [ExportLimits.Default] by default.
 * @param configure sets the limits that differ from [base].
 * @return the limits.
 * @throws com.davils.kore.core.validation.ValidationException if a limit is below 1, not positive or above its hard
 *   ceiling.
 * @since 1.0.0
 */
public fun exportLimits(
    base: ExportLimits = ExportLimits.Default,
    configure: ExportLimitsBuilder.() -> Unit,
): ExportLimits {
    val builder = ExportLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}

/**
 * Builds validated ingest limits that start from [base] and change what [configure] sets.
 *
 * @param base the limits every property starts from; [IngestLimits.Default] by default.
 * @param configure sets the limits that differ from [base].
 * @return the limits.
 * @throws com.davils.kore.core.validation.ValidationException if a limit is below 1, not positive or above its hard
 *   ceiling.
 * @since 1.0.0
 */
public fun ingestLimits(
    base: IngestLimits = IngestLimits.Default,
    configure: IngestLimitsBuilder.() -> Unit,
): IngestLimits {
    val builder = IngestLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}

/**
 * Builds validated limits that start from [base] and change what [configure] sets.
 *
 * @param base the limits every property starts from; [HealthLimits.Default] by default.
 * @param configure sets the limits that differ from [base].
 * @return the limits.
 * @throws com.davils.kore.core.validation.ValidationException if a limit is below 1, not positive or above its hard
 *   ceiling.
 * @since 1.0.0
 */
public fun healthLimits(
    base: HealthLimits = HealthLimits.Default,
    configure: HealthLimitsBuilder.() -> Unit,
): HealthLimits {
    val builder = HealthLimitsBuilder()
    builder.from(base)
    builder.configure()
    return builder.build()
}
