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

@file:OptIn(DavilsInternal::class)

package com.davils.kollect.core.internal.registry

import com.davils.kollect.core.internal.cell.ReadContext
import com.davils.kollect.core.internal.text.TextRules
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.Labels
import com.davils.kollect.core.snapshot.Point
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard

private const val MINIMUM_TOMBSTONES: Int = 64

@OptIn(DavilsInternal::class)
internal class FamilyCore<out M>(
    val descriptor: Descriptor,
    private val arity: Int,
    private val kept: List<Int>,
    private val context: RegistryContext,
    private val create: () -> Created<M>,
) {
    private val guard: Guard = guard()
    private var series: OrderedMap<SeriesKey, SeriesEntry<M>> = OrderedMap()
    private var tombstones: Int = 0
    private var overflow: SeriesEntry<M>? = null
    private var isOverflowing: Boolean = false
    private var sorted: List<SeriesEntry<M>>? = null
    private val isKeyed: Boolean = kept.isNotEmpty()

    init {
        if (!isKeyed) series[SeriesKey(emptyList())] = entryOf(SeriesKey(emptyList()))
    }

    val held: Int get() = guard.locked { series.size + (1.takeIf { isKeyed } ?: 0) }

    val size: Int get() = guard.locked { series.size + (1.takeIf { overflow != null } ?: 0) }

    fun with(values: Array<out String>): M? {
        if (!isValid(values)) return null.also { context.rejections.reject(Rejection.InvalidLabels) }
        val key = keyOf(values)
        return guard.locked { series[key] ?: admitted(key) }.meter
    }

    fun remove(values: Array<out String>): Boolean {
        if (!isValid(values) || !isKeyed) return false
        val key = keyOf(values)
        val removed = guard.locked { series.remove(key)?.also { forget() } }
        if (removed != null) context.budget.release(1)
        return removed != null
    }

    fun read(context: ReadContext): List<Point> = entries().mapNotNull { entry ->
        entry.cell.read(entry.labels, entry.start, context)
    }

    fun entries(): List<SeriesEntry<M>> = guard.locked { sorted ?: sortedEntries().also { sorted = it } }

    private fun sortedEntries(): List<SeriesEntry<M>> {
        val live = series.entries.map { (_, entry) -> entry }
        return (live + listOfNotNull(overflow)).sortedBy(SeriesEntry<M>::labels)
    }

    private fun admitted(key: SeriesKey): SeriesEntry<M> {
        val isFull = series.size >= context.limits.maximumSeriesPerFamily - 1
        if (isFull || !context.budget.tryAcquire()) return overflowEntry()
        val entry = entryOf(key)
        series[key] = entry
        sorted = null
        isOverflowing = false
        return entry
    }

    private fun overflowEntry(): SeriesEntry<M> {
        if (!isOverflowing) context.listener.overflowed(descriptor.name)
        isOverflowing = true
        return overflow ?: overflowCreated()
    }

    private fun overflowCreated(): SeriesEntry<M> {
        val created = create()
        val entry = SeriesEntry(
            labels = Labels.OVERFLOW,
            start = context.clock.now(),
            meter = created.meter,
            cell = created.cell,
        )
        overflow = entry
        sorted = null
        return entry
    }

    private fun entryOf(key: SeriesKey): SeriesEntry<M> {
        val created = create()
        val labels = Labels(descriptor.labelKeys, key.values)
        return SeriesEntry(labels = labels, start = context.clock.now(), meter = created.meter, cell = created.cell)
    }

    private fun forget() {
        sorted = null
        tombstones += 1
        if (tombstones > maxOf(a = MINIMUM_TOMBSTONES, b = series.size)) compact()
    }

    private fun compact() {
        val compacted = OrderedMap<SeriesKey, SeriesEntry<M>>()
        series.entries.forEach { (key, entry) -> compacted[key] = entry }
        series = compacted
        tombstones = 0
    }

    private fun isValid(values: Array<out String>): Boolean {
        val maximum = context.limits.maximumLabelValueLength
        return values.size == arity && values.all { value -> TextRules.isValidText(value, maximum) }
    }

    private fun keyOf(values: Array<out String>): SeriesKey = SeriesKey(kept.map { index -> values[index] })
}
