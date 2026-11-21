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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * What an exposition wrote and what it left out.
 *
 * @property families the number of families written.
 * @property series the number of series written.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class ExpositionReport internal constructor(
    public val families: Int,
    public val series: Int,
    refused: List<RefusedFamily>,
) {
    /**
     * The families left out, in the order they were met.
     *
     * @since 1.0.0
     */
    public val refused: List<RefusedFamily> = refused.immutableList()

    /**
     * Shows the report, such as `ExpositionReport(12 families, 340 series, 0 refused)`.
     *
     * @return the report as text.
     * @since 1.0.0
     */
    override fun toString(): String = "ExpositionReport($families families, $series series, ${refused.size} refused)"
}
