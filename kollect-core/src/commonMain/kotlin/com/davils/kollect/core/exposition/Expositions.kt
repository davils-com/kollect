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

import com.davils.kollect.core.internal.exposition.AcceptParser

/**
 * Creates an exposition.
 *
 * @param configure sets its limits and redaction.
 * @return the exposition.
 * @since 1.0.0
 */
public fun exposition(configure: ExpositionBuilder.() -> Unit = {}): Exposition {
    val builder = ExpositionBuilder().apply(configure)
    return Exposition(builder.limits, builder.redaction)
}

/**
 * Chooses the format to answer a scrape with from its `Accept` header, following RFC 9110.
 *
 * OpenMetrics is chosen when the header accepts `application/openmetrics-text` in version 1.0.0 or without a version
 * with a quality above 0 and at least that of any other media range; every other header, a missing one and one that
 * cannot be read give [ExpositionFormat.PrometheusText]. Only the first 4,096 characters and 32 media ranges are read.
 * The function never throws.
 *
 * @param accept the value of the `Accept` header, or null if the request has none.
 * @return the format.
 * @since 1.0.0
 */
public fun negotiateExposition(accept: String?): ExpositionFormat = AcceptParser.formatOf(accept)
