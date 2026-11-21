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

package com.davils.kollect.core

/**
 * The result of declaring a family: declared, already existing, or refused.
 *
 * Every result carries a [family]; the family of a refusal records nothing.
 *
 * @param F the type of the family.
 * @since 1.0.0
 */
public sealed interface Registration<out F> {
    /**
     * The family; it records nothing if the declaration was refused.
     *
     * @since 1.0.0
     */
    public val family: F

    /**
     * The family was declared by this call.
     *
     * @param F the type of the family.
     * @property family the family.
     * @since 1.0.0
     */
    public class Declared<out F> internal constructor(override val family: F) : Registration<F> {
        /**
         * Returns `Declared`.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Declared"
    }

    /**
     * A family with an equal descriptor existed already.
     *
     * @param F the type of the family.
     * @property family the existing family.
     * @since 1.0.0
     */
    public class Existing<out F> internal constructor(override val family: F) : Registration<F> {
        /**
         * Returns `Existing`.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Existing"
    }

    /**
     * The declaration was refused.
     *
     * @param F the type of the family.
     * @property family the family, which records nothing.
     * @property refusal why it was refused.
     * @since 1.0.0
     */
    public class Refused<out F> internal constructor(override val family: F, public val refusal: Refusal) :
        Registration<F> {
        /**
         * Shows the result, such as `Refused(Denied)`.
         *
         * @return the result as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($refusal)"
    }
}

/**
 * Returns the family, or throws if the declaration was refused; for code that must not run without its meters.
 *
 * @receiver the result of a declaration.
 * @return the family.
 * @throws IllegalStateException if the declaration was refused; the message names the refusal.
 * @since 1.0.0
 */
public fun <F> Registration<F>.orThrow(): F {
    check(this !is Registration.Refused) { "The declaration of a family was refused: $this" }
    return family
}
