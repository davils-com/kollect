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

/**
 * A handle that records measurements into one series of a family.
 *
 * A meter is obtained from a [com.davils.kollect.core.Family] and is safe to use from many threads at the same time.
 * Recording never throws and never blocks: a measurement a meter cannot take, such as a negative amount of a counter,
 * is rejected and counted by the registry. A meter of a refused declaration or of an invalid series records nothing.
 * Applications should keep a meter instead of looking it up for every measurement.
 *
 * @since 1.0.0
 */
public interface Meter
