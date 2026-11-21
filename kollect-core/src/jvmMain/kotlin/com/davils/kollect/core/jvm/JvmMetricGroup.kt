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

package com.davils.kollect.core.jvm

/**
 * A group of the JVM metrics [bindJvmMetrics] reports, named after the semantic conventions of OpenTelemetry.
 *
 * @since 1.0.0
 */
public enum class JvmMetricGroup {
    /**
     * `jvm.memory.used`, `jvm.memory.committed`, `jvm.memory.limit` and `jvm.memory.used_after_last_gc`, by memory
     * pool.
     *
     * @since 1.0.0
     */
    Memory,

    /**
     * `jvm.gc.duration`, the pauses of every collector, recorded from the notifications of the JVM.
     *
     * @since 1.0.0
     */
    GarbageCollection,

    /**
     * `jvm.thread.count`, by whether the threads are daemons; read cheaply, without thread dumps.
     *
     * @since 1.0.0
     */
    Threads,

    /**
     * `jvm.class.loaded`, `jvm.class.unloaded` and `jvm.class.count`.
     *
     * @since 1.0.0
     */
    Classes,

    /**
     * `jvm.cpu.time`, `jvm.cpu.count` and `jvm.cpu.recent_utilization`.
     *
     * @since 1.0.0
     */
    Cpu,

    /**
     * `jvm.file_descriptor.count` and `jvm.file_descriptor.limit`, on operating systems that report them.
     *
     * @since 1.0.0
     */
    FileDescriptors,
}
