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

internal data class DecodedPoint(
    val attributes: List<Pair<String, String>>?,
    val start: Long = 0L,
    val time: Long = 0L,
    val value: Double = 0.0,
    val count: Long = 0L,
    val sum: Double = 0.0,
    val buckets: List<Long> = emptyList(),
    val bounds: List<Double> = emptyList(),
    val flags: Long = 0L,
)
