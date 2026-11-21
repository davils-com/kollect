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

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.internal.cell.LongTaskCell
import com.davils.kollect.core.internal.cell.ReadContext
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.snapshot.FamilySnapshot
import com.davils.kollect.core.snapshot.GaugePoint
import com.davils.kollect.core.snapshot.Temporality
import com.davils.kollect.core.snapshot.Unreadability
import com.davils.kollect.core.snapshot.Unreadable
import kotlin.time.Instant

internal class RecordedMember(
    val core: FamilyCore<*>,
    override val variant: Variant,
    override val claims: List<String>,
    override val family: Any,
    private val active: Descriptor?,
    private val context: RegistryContext,
) : Member {
    override val descriptor: Descriptor get() = core.descriptor

    override fun collect(now: Instant): Collected {
        val read = ReadContext(now, context.timeSource, context.limits.maximumSnapshotStall)
        val points = core.read(read)
        if (read.isStalled) return stalled()
        val own = FamilySnapshot(descriptor, Temporality.Cumulative, points)
        return Collected(listOf(own) + listOfNotNull(activeSnapshot(now)), unreadable = null)
    }

    override fun held(): Int = core.held

    private fun stalled(): Collected {
        val entry = Unreadable(descriptor.name, Unreadability.Stalled)
        context.listener.unreadable(entry)
        return Collected(emptyList(), entry)
    }

    private fun activeSnapshot(now: Instant): FamilySnapshot? {
        val activeDescriptor = active ?: return null
        val points = core.entries().map { entry ->
            val count = (entry.meter as? LongTaskCell)?.activeCount ?: 0
            GaugePoint(labels = entry.labels, start = entry.start, value = count.toDouble(), observedAt = now)
        }
        return FamilySnapshot(activeDescriptor, Temporality.Cumulative, points)
    }
}
