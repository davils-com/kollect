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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration

/**
 * Builds [RegistryLimits]; every property starts from the limits [registryLimits] starts from.
 *
 * Each property documents its limit on [RegistryLimits]. A property can no longer be set once the builder was built.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class RegistryLimitsBuilder internal constructor() : ValidatingBuilder<RegistryLimits>() {
    override val subject: String get() = "registry limits"

    /**
     * See [RegistryLimits.maximumFamilies].
     *
     * @since 1.0.0
     */
    public var maximumFamilies: Int = RegistryLimits.DEFAULT_FAMILIES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumSeriesPerFamily].
     *
     * @since 1.0.0
     */
    public var maximumSeriesPerFamily: Int = RegistryLimits.DEFAULT_SERIES_PER_FAMILY
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumSeries].
     *
     * @since 1.0.0
     */
    public var maximumSeries: Int = RegistryLimits.DEFAULT_SERIES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumLabels].
     *
     * @since 1.0.0
     */
    public var maximumLabels: Int = RegistryLimits.DEFAULT_LABELS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumNameLength].
     *
     * @since 1.0.0
     */
    public var maximumNameLength: Int = RegistryLimits.DEFAULT_NAME_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumLabelValueLength].
     *
     * @since 1.0.0
     */
    public var maximumLabelValueLength: Int = RegistryLimits.DEFAULT_LABEL_VALUE_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumDescriptionLength].
     *
     * @since 1.0.0
     */
    public var maximumDescriptionLength: Int = RegistryLimits.DEFAULT_DESCRIPTION_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumBuckets].
     *
     * @since 1.0.0
     */
    public var maximumBuckets: Int = RegistryLimits.DEFAULT_BUCKETS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumStates].
     *
     * @since 1.0.0
     */
    public var maximumStates: Int = RegistryLimits.DEFAULT_STATES
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumActiveTasks].
     *
     * @since 1.0.0
     */
    public var maximumActiveTasks: Int = RegistryLimits.DEFAULT_ACTIVE_TASKS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumFilters].
     *
     * @since 1.0.0
     */
    public var maximumFilters: Int = RegistryLimits.DEFAULT_FILTERS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * See [RegistryLimits.maximumSnapshotStall].
     *
     * @since 1.0.0
     */
    public var maximumSnapshotStall: Duration = RegistryLimits.DEFAULT_SNAPSHOT_STALL
        set(value) {
            checkOpen()
            field = value
        }

    internal fun from(base: RegistryLimits) {
        maximumFamilies = base.maximumFamilies
        maximumSeriesPerFamily = base.maximumSeriesPerFamily
        maximumSeries = base.maximumSeries
        maximumLabels = base.maximumLabels
        maximumNameLength = base.maximumNameLength
        maximumLabelValueLength = base.maximumLabelValueLength
        maximumDescriptionLength = base.maximumDescriptionLength
        maximumBuckets = base.maximumBuckets
        maximumStates = base.maximumStates
        maximumActiveTasks = base.maximumActiveTasks
        maximumFilters = base.maximumFilters
        maximumSnapshotStall = base.maximumSnapshotStall
    }

    override fun assemble(): RegistryLimits = RegistryLimits(this)
}
