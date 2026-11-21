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

package com.davils.kollect.core.internal.jvm

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.annotation.DavilsPlatformBinding
import com.sun.management.GarbageCollectionNotificationInfo
import com.sun.management.OperatingSystemMXBean
import com.sun.management.UnixOperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.lang.management.MemoryPoolMXBean
import java.lang.management.MemoryType
import javax.management.Notification
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import javax.management.openmbean.CompositeData

private const val NANOSECONDS_PER_SECOND: Double = 1_000_000_000.0

@OptIn(DavilsInternal::class)
@DavilsPlatformBinding
internal object PlatformReadings : ManagementReadings {
    override fun pools(): List<PoolReading> = ManagementFactory.getMemoryPoolMXBeans().map(::readingOf)

    override fun threads(): Pair<Long, Long> {
        val threads = ManagementFactory.getThreadMXBean()
        return threads.threadCount.toLong() to threads.daemonThreadCount.toLong()
    }

    override fun classes(): Triple<Long, Long, Long> {
        val classes = ManagementFactory.getClassLoadingMXBean()
        return Triple(
            first = classes.totalLoadedClassCount,
            second = classes.unloadedClassCount,
            third = classes.loadedClassCount.toLong(),
        )
    }

    override fun cpuSeconds(): Double? {
        val time = (ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean)?.processCpuTime
        return time?.takeIf { nanoseconds -> nanoseconds >= 0L }?.let { nanoseconds ->
            nanoseconds /
                NANOSECONDS_PER_SECOND
        }
    }

    override fun processors(): Int = Runtime.getRuntime().availableProcessors()

    override fun cpuUtilization(): Double? {
        val load = (ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean)?.processCpuLoad
        return load?.takeIf { value -> value >= 0.0 }
    }

    override fun fileDescriptors(): Pair<Long, Long>? {
        val system = ManagementFactory.getOperatingSystemMXBean() as? UnixOperatingSystemMXBean ?: return null
        return system.openFileDescriptorCount to system.maxFileDescriptorCount
    }

    override fun subscribeToCollections(listener: (String, String, Long) -> Unit): () -> Unit {
        val emitters = ManagementFactory.getGarbageCollectorMXBeans().filterIsInstance<NotificationEmitter>()
        val notified = NotificationListener { notification, _ -> collected(notification, listener) }
        emitters.forEach { emitter -> emitter.addNotificationListener(notified, null, null) }
        return { emitters.forEach { emitter -> emitter.removeNotificationListener(notified) } }
    }

    private fun collected(notification: Notification, listener: (String, String, Long) -> Unit) {
        val isCollection = notification.type == GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
        val data = notification.userData as? CompositeData
        if (!isCollection || data == null) return
        val info = GarbageCollectionNotificationInfo.from(data)
        listener(info.gcName, info.gcAction, info.gcInfo.duration)
    }

    private fun readingOf(pool: MemoryPoolMXBean): PoolReading {
        val usage = pool.usage
        val type = "heap".takeIf { pool.type == MemoryType.HEAP } ?: "non_heap"
        return PoolReading(
            name = pool.name,
            type = type,
            used = usage.used,
            committed = usage.committed,
            limit = usage.max,
            usedAfterGc = pool.collectionUsage?.used ?: -1L,
        )
    }
}
