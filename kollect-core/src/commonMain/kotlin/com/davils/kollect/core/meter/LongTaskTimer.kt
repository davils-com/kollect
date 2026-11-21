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
 * A meter of tasks that run for a long time, such as a running migration: it reports how many run, how long they ran
 * together and the longest one, while they still run.
 *
 * @since 1.0.0
 */
public interface LongTaskTimer : Meter {
    /**
     * Starts tracking a task until the returned handle is closed.
     *
     * A task beyond [com.davils.kollect.core.limit.RegistryLimits.maximumActiveTasks] is not tracked; its refusal is
     * counted and its handle does nothing.
     *
     * @return the handle of the task.
     * @since 1.0.0
     */
    public fun start(): LongTask
}
