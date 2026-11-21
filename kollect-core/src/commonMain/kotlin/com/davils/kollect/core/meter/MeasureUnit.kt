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

import com.davils.kollect.core.internal.text.NameRules

private const val ANNOTATION_START: Char = '{'
private const val ANNOTATION_END: Char = '}'
private const val HASH_FACTOR: Int = 31

/**
 * The unit of a family, as a code of the Unified Code for Units of Measure (UCUM) that OTLP carries and the suffix a
 * Prometheus name gets.
 *
 * Units are base units only, so a duration is measured in [SECONDS] and a size in [BYTES], never in milliseconds or
 * kilobytes. [annotation] builds a unit that names what is counted, such as `{request}`, which has no Prometheus
 * suffix. The set of units is closed: two units are equal when their code and their suffix are equal.
 *
 * @property ucum the UCUM code, such as `s`; empty for [NONE].
 * @property prometheusSuffix the suffix of a Prometheus name, such as `seconds`, or null if the unit adds none.
 * @since 1.0.0
 */
public class MeasureUnit private constructor(public val ucum: String, public val prometheusSuffix: String?) {
    /**
     * Tells whether [other] is the same unit.
     *
     * @param other the object to compare with.
     * @return true if [other] has the same code and suffix.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is MeasureUnit) return false
        return ucum == other.ucum && prometheusSuffix == other.prometheusSuffix
    }

    /**
     * Returns a hash of the code and the suffix.
     *
     * @return the hash.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * ucum.hashCode() + prometheusSuffix.hashCode()

    /**
     * Returns the UCUM code, or `none` for [NONE].
     *
     * @return the unit as text.
     * @since 1.0.0
     */
    override fun toString(): String = ucum.ifEmpty { "none" }

    /**
     * The units kollect knows.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * No unit.
         *
         * @since 1.0.0
         */
        public val NONE: MeasureUnit = MeasureUnit(ucum = "", prometheusSuffix = null)

        /**
         * A ratio between 0 and 1, `1` in UCUM and `ratio` in Prometheus.
         *
         * @since 1.0.0
         */
        public val RATIO: MeasureUnit = MeasureUnit(ucum = "1", prometheusSuffix = "ratio")

        /**
         * Percent between 0 and 100, `%` in UCUM and `percent` in Prometheus.
         *
         * @since 1.0.0
         */
        public val PERCENT: MeasureUnit = MeasureUnit(ucum = "%", prometheusSuffix = "percent")

        /**
         * Seconds, `s`.
         *
         * @since 1.0.0
         */
        public val SECONDS: MeasureUnit = MeasureUnit(ucum = "s", prometheusSuffix = "seconds")

        /**
         * Bytes, `By`.
         *
         * @since 1.0.0
         */
        public val BYTES: MeasureUnit = MeasureUnit(ucum = "By", prometheusSuffix = "bytes")

        /**
         * Degrees Celsius, `Cel`.
         *
         * @since 1.0.0
         */
        public val CELSIUS: MeasureUnit = MeasureUnit(ucum = "Cel", prometheusSuffix = "celsius")

        /**
         * Volts, `V`.
         *
         * @since 1.0.0
         */
        public val VOLTS: MeasureUnit = MeasureUnit(ucum = "V", prometheusSuffix = "volts")

        /**
         * Amperes, `A`.
         *
         * @since 1.0.0
         */
        public val AMPERES: MeasureUnit = MeasureUnit(ucum = "A", prometheusSuffix = "amperes")

        /**
         * Watts, `W`.
         *
         * @since 1.0.0
         */
        public val WATTS: MeasureUnit = MeasureUnit(ucum = "W", prometheusSuffix = "watts")

        /**
         * Joules, `J`.
         *
         * @since 1.0.0
         */
        public val JOULES: MeasureUnit = MeasureUnit(ucum = "J", prometheusSuffix = "joules")

        /**
         * Hertz, `Hz`.
         *
         * @since 1.0.0
         */
        public val HERTZ: MeasureUnit = MeasureUnit(ucum = "Hz", prometheusSuffix = "hertz")

        /**
         * Meters, `m`.
         *
         * @since 1.0.0
         */
        public val METERS: MeasureUnit = MeasureUnit(ucum = "m", prometheusSuffix = "meters")

        private val KNOWN: List<MeasureUnit> =
            listOf(NONE, RATIO, PERCENT, SECONDS, BYTES, CELSIUS, VOLTS, AMPERES, WATTS, JOULES, HERTZ, METERS)

        /**
         * Returns a unit that names what is counted, such as `{request}` for `annotation("request")`.
         *
         * The annotation follows the grammar of a [com.davils.kollect.core.name.MetricName] and adds no suffix to a
         * Prometheus name.
         *
         * @param name what is counted, such as `request`.
         * @return the unit.
         * @throws IllegalArgumentException if [name] does not follow the grammar.
         * @since 1.0.0
         */
        public fun annotation(name: String): MeasureUnit {
            val valid = NameRules.METRIC.requireValid(text = name, kind = "unit annotation")
            return MeasureUnit(ucum = "$ANNOTATION_START$valid$ANNOTATION_END", prometheusSuffix = null)
        }

        /**
         * Returns the unit with the UCUM code [ucum], one of the units of this companion or an annotation, or null if
         * kollect does not know the code.
         *
         * @param ucum the code, such as `s` or `{request}`.
         * @return the unit, or null.
         * @since 1.0.0
         */
        public fun ofUcumOrNull(ucum: String): MeasureUnit? {
            val known = KNOWN.firstOrNull { unit -> unit.ucum == ucum }
            if (known != null) return known
            return annotationOrNull(ucum)
        }

        private fun annotationOrNull(ucum: String): MeasureUnit? {
            val inner = ucum.removeSurrounding(prefix = ANNOTATION_START.toString(), suffix = ANNOTATION_END.toString())
            val isAnnotation = inner.length + 2 == ucum.length
            val name = inner.takeIf { isAnnotation }?.let(NameRules.METRIC::validOrNull) ?: return null
            return MeasureUnit(ucum = "$ANNOTATION_START$name$ANNOTATION_END", prometheusSuffix = null)
        }
    }
}
