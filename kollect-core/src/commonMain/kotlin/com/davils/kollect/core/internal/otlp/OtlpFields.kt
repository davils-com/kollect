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

package com.davils.kollect.core.internal.otlp

internal object OtlpFields {
    const val REQUEST_RESOURCE_METRICS: Int = 1
    const val RESOURCE_METRICS_RESOURCE: Int = 1
    const val RESOURCE_METRICS_SCOPE_METRICS: Int = 2
    const val RESOURCE_ATTRIBUTES: Int = 1
    const val SCOPE_METRICS_SCOPE: Int = 1
    const val SCOPE_METRICS_METRICS: Int = 2
    const val SCOPE_NAME: Int = 1
    const val KEY_VALUE_KEY: Int = 1
    const val KEY_VALUE_VALUE: Int = 2
    const val ANY_STRING: Int = 1
    const val ANY_BOOL: Int = 2
    const val ANY_INT: Int = 3
    const val ANY_DOUBLE: Int = 4
    const val METRIC_NAME: Int = 1
    const val METRIC_DESCRIPTION: Int = 2
    const val METRIC_UNIT: Int = 3
    const val METRIC_GAUGE: Int = 5
    const val METRIC_SUM: Int = 7
    const val METRIC_HISTOGRAM: Int = 9
    const val METRIC_METADATA: Int = 12
    const val DATA_POINTS: Int = 1
    const val TEMPORALITY: Int = 2
    const val MONOTONIC: Int = 3
    const val NUMBER_START: Int = 2
    const val NUMBER_TIME: Int = 3
    const val NUMBER_DOUBLE: Int = 4
    const val NUMBER_INT: Int = 6
    const val NUMBER_ATTRIBUTES: Int = 7
    const val NUMBER_FLAGS: Int = 8
    const val HISTOGRAM_START: Int = 2
    const val HISTOGRAM_TIME: Int = 3
    const val HISTOGRAM_COUNT: Int = 4
    const val HISTOGRAM_SUM: Int = 5
    const val HISTOGRAM_BUCKETS: Int = 6
    const val HISTOGRAM_BOUNDS: Int = 7
    const val HISTOGRAM_ATTRIBUTES: Int = 9
    const val HISTOGRAM_FLAGS: Int = 10
    const val HISTOGRAM_MIN: Int = 11
    const val HISTOGRAM_MAX: Int = 12
    const val RESPONSE_PARTIAL_SUCCESS: Int = 1
    const val PARTIAL_REJECTED: Int = 1
    const val PARTIAL_MESSAGE: Int = 2
    const val TEMPORALITY_DELTA: Long = 1L
    const val TEMPORALITY_CUMULATIVE: Long = 2L
    const val FLAG_NO_RECORDED_VALUE: Long = 1L
    const val CONTENT_TYPE: String = "application/x-protobuf"
}
