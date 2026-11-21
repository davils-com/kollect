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

import com.davils.kollect.core.declare.FamilyBuilder
import com.davils.kollect.core.declare.HistogramBuilder
import com.davils.kollect.core.internal.cell.CounterCell
import com.davils.kollect.core.internal.cell.GaugeCell
import com.davils.kollect.core.internal.cell.HistogramCell
import com.davils.kollect.core.internal.cell.InfoCell
import com.davils.kollect.core.internal.cell.LongTaskCell
import com.davils.kollect.core.internal.cell.NoopMeters
import com.davils.kollect.core.internal.cell.NoopStateSet
import com.davils.kollect.core.internal.cell.NoopTimer
import com.davils.kollect.core.internal.cell.Rejections
import com.davils.kollect.core.internal.cell.StateCell
import com.davils.kollect.core.internal.cell.TimerCell
import com.davils.kollect.core.internal.cell.UpDownCell
import com.davils.kollect.core.internal.event.RegistryEvents
import com.davils.kollect.core.internal.registry.Created
import com.davils.kollect.core.internal.registry.RegistryContext
import com.davils.kollect.core.internal.registry.RegistryCore
import com.davils.kollect.core.internal.registry.Request
import com.davils.kollect.core.internal.registry.SeriesBudget
import com.davils.kollect.core.internal.registry.Variant
import com.davils.kollect.core.limit.RegistryLimits
import com.davils.kollect.core.meter.Counter
import com.davils.kollect.core.meter.ExplicitBounds
import com.davils.kollect.core.meter.Gauge
import com.davils.kollect.core.meter.Histogram
import com.davils.kollect.core.meter.Info
import com.davils.kollect.core.meter.LongTaskTimer
import com.davils.kollect.core.meter.MeasureUnit
import com.davils.kollect.core.meter.MeterKind
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.meter.StateSet
import com.davils.kollect.core.meter.Timer
import com.davils.kollect.core.meter.UpDownCounter
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.MetricsSnapshot
import kotlin.enums.enumEntries
import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * The registry of every family of an application: it declares families, holds their series within
 * [RegistryLimits] and takes consistent snapshots of them.
 *
 * Declaring a family returns a [Registration], which is never null and never throws: an invalid or conflicting
 * declaration is refused, and the family it returns records nothing. Declaring an equal family again returns the
 * existing one. A snapshot reads every family in the order of its name; a histogram whose recordings in flight do not
 * finish within [RegistryLimits.maximumSnapshotStall] and an observed family whose callback throws are reported as
 * unreadable instead of being shown with values the registry cannot vouch for.
 *
 * The registry counts what it rejects in its own families: `kollect.measurements.rejected`,
 * `kollect.declarations.refused`, `kollect.registry.families` and `kollect.registry.series`.
 *
 * A registry holds nothing global, starts no thread and is safe to use from many threads.
 *
 * @since 1.0.0
 */
public class MeterRegistry internal constructor(
    clock: Clock,
    timeSource: TimeSource.WithComparableMarks,
    builder: MeterRegistryBuilder,
) {
    /**
     * The limits of the registry.
     *
     * @since 1.0.0
     */
    public val limits: RegistryLimits = builder.limits

    internal val timeSource: TimeSource.WithComparableMarks = timeSource

    private val rejections: Rejections = Rejections()
    private val context: RegistryContext = RegistryContext(
        clock = clock,
        timeSource = timeSource,
        limits = limits,
        rejections = rejections,
        budget = SeriesBudget(limits.maximumSeries),
        listener = builder.listener,
    )
    private val core: RegistryCore = RegistryCore(context, builder.filters.toList())
    private val noopTimer: NoopTimer = NoopTimer(timeSource)

    init {
        declareOwnFamilies()
    }

    /**
     * Declares a counter family: a sum that only grows.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public fun counter(name: String, configure: FamilyBuilder.() -> Unit = {}): Registration<Family<Counter>> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        return core.declare(requestOf(name, MeterKind.Counter, builder), NoopMeters as Counter) {
            CounterCell(rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares an up-down counter family: a sum that grows and shrinks.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public fun upDownCounter(
        name: String,
        configure: FamilyBuilder.() -> Unit = {},
    ): Registration<Family<UpDownCounter>> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        return core.declare(requestOf(name, MeterKind.UpDownCounter, builder), NoopMeters as UpDownCounter) {
            UpDownCell(rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares a gauge family: the last value of something. A series reports no point until its value was set.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public fun gauge(name: String, configure: FamilyBuilder.() -> Unit = {}): Registration<Family<Gauge>> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        return core.declare(requestOf(name, MeterKind.Gauge, builder), NoopMeters as Gauge) {
            GaugeCell(context.clock, rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares a histogram family: the distribution of values over explicit bucket bounds,
     * [ExplicitBounds.DEFAULT] unless [configure] sets others.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public fun histogram(name: String, configure: HistogramBuilder.() -> Unit = {}): Registration<Family<Histogram>> {
        val builder = HistogramBuilder(MeasureUnit.NONE, ExplicitBounds.DEFAULT).apply(configure)
        val bounds = builder.bounds.values.toDoubleArray()
        val variant = Variant.Negative.takeIf { builder.isNegativeAllowed } ?: Variant.Plain
        val request = requestOf(
            name = name,
            kind = MeterKind.Histogram,
            builder = builder,
            bounds = builder.bounds,
            variant = variant,
        )
        return core.declare(request, NoopMeters as Histogram) {
            HistogramCell(bounds, builder.isNegativeAllowed, rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares a timer family: a histogram of durations in seconds over [ExplicitBounds.SECONDS] unless [configure]
     * sets other bounds.
     *
     * @param name the name of the family, following the grammar of [MetricName]; the unit adds `_seconds` to its
     *   Prometheus name.
     * @param configure describes the family; its unit stays [MeasureUnit.SECONDS].
     * @return the registration.
     * @since 1.0.0
     */
    public fun timer(name: String, configure: HistogramBuilder.() -> Unit = {}): Registration<Family<Timer>> {
        val builder = HistogramBuilder(MeasureUnit.SECONDS, ExplicitBounds.SECONDS).apply(configure)
        val bounds = builder.bounds.values.toDoubleArray()
        val request = requestOf(
            name = name,
            kind = MeterKind.Histogram,
            builder = builder,
            bounds = builder.bounds,
            variant = Variant.Timer,
        )
        return core.declare(request, noopTimer as Timer) {
            TimerCell(bounds, context.timeSource, rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares a long task timer family: a gauge of the seconds the running tasks of a series ran together, and a
     * gauge `<name>.active` of how many run.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family; its unit stays [MeasureUnit.SECONDS].
     * @return the registration.
     * @since 1.0.0
     */
    public fun longTaskTimer(
        name: String,
        configure: FamilyBuilder.() -> Unit = {},
    ): Registration<Family<LongTaskTimer>> {
        val builder = FamilyBuilder(MeasureUnit.SECONDS).apply(configure)
        val request = requestOf(name = name, kind = MeterKind.Gauge, builder = builder, variant = Variant.LongTask)
        return core.declare(request, NoopMeters as LongTaskTimer) {
            LongTaskCell(limits.maximumActiveTasks, context.timeSource, rejections).let { cell -> Created(cell, cell) }
        }
    }

    /**
     * Declares a state set family over the states [E], whose series start in [initial].
     *
     * @param E the states, whose names become the states of the family.
     * @param name the name of the family, following the grammar of [MetricName].
     * @param initial the state a new series starts in.
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public inline fun <reified E : Enum<E>> stateSet(
        name: String,
        initial: E,
        noinline configure: FamilyBuilder.() -> Unit = {},
    ): Registration<Family<StateSet<E>>> = stateSet(
        name = name,
        states = enumEntries<E>().map(Enum<E>::name),
        initial = initial.ordinal,
        configure = configure,
    )

    @PublishedApi
    internal fun <E : Enum<E>> stateSet(
        name: String,
        states: List<String>,
        initial: Int,
        configure: FamilyBuilder.() -> Unit,
    ): Registration<Family<StateSet<E>>> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        val request = requestOf(name = name, kind = MeterKind.StateSet, builder = builder, states = states)
        return core.declare(request, NoopStateSet<E>()) { StateCell<E>(initial).let { cell -> Created(cell, cell) } }
    }

    /**
     * Declares an info family: constant facts carried in the labels of its series, such as `version`. Each call of
     * [Family.with] creates the series with its facts.
     *
     * @param name the name of the family, following the grammar of [MetricName]; `_info` is added to its Prometheus
     *   name.
     * @param configure describes the family.
     * @return the registration.
     * @since 1.0.0
     */
    public fun info(name: String, configure: FamilyBuilder.() -> Unit = {}): Registration<Family<Info>> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        return core.declare(
            requestOf(name, MeterKind.Info, builder),
            NoopMeters as Info,
        ) { Created(InfoCell, InfoCell) }
    }

    /**
     * Declares an observed gauge family, whose callback reports its values while a snapshot collects it.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @param observe reports the values to the sink; it runs on the thread that takes the snapshot.
     * @return the registration.
     * @since 1.0.0
     */
    public fun observedGauge(
        name: String,
        configure: FamilyBuilder.() -> Unit = {},
        observe: ObservationSink.() -> Unit,
    ): Registration<ObservedFamily> = observed(
        name = name,
        kind = MeterKind.Gauge,
        configure = configure,
        observe = observe,
    )

    /**
     * Declares an observed counter family, whose callback reports sums that only grow. A sum below the one reported
     * before starts the series anew.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @param observe reports the sums to the sink; it runs on the thread that takes the snapshot.
     * @return the registration.
     * @since 1.0.0
     */
    public fun observedCounter(
        name: String,
        configure: FamilyBuilder.() -> Unit = {},
        observe: ObservationSink.() -> Unit,
    ): Registration<ObservedFamily> = observed(
        name = name,
        kind = MeterKind.Counter,
        configure = configure,
        observe = observe,
    )

    /**
     * Declares an observed up-down counter family, whose callback reports sums that grow and shrink.
     *
     * @param name the name of the family, following the grammar of [MetricName].
     * @param configure describes the family.
     * @param observe reports the sums to the sink; it runs on the thread that takes the snapshot.
     * @return the registration.
     * @since 1.0.0
     */
    public fun observedUpDownCounter(
        name: String,
        configure: FamilyBuilder.() -> Unit = {},
        observe: ObservationSink.() -> Unit,
    ): Registration<ObservedFamily> = observed(
        name = name,
        kind = MeterKind.UpDownCounter,
        configure = configure,
        observe = observe,
    )

    /**
     * Removes the family named [name] with every series it holds.
     *
     * @param name the name.
     * @return true if the registry held the family.
     * @since 1.0.0
     */
    public fun remove(name: MetricName): Boolean = core.remove(name)

    /**
     * Takes a snapshot of every family, ordered by name.
     *
     * The snapshot runs the callbacks of observed families on the calling thread and waits for recordings in flight
     * at most [RegistryLimits.maximumSnapshotStall] per histogram. It never blocks otherwise.
     *
     * @return the snapshot.
     * @since 1.0.0
     */
    public fun snapshot(): MetricsSnapshot = core.collect()

    /**
     * Closes the registry: it drops every family and refuses every later declaration with [Refusal.Closed]. Meters
     * obtained before keep working but are no longer read.
     *
     * @since 1.0.0
     */
    public fun close() {
        core.close()
    }

    /**
     * Shows the registry, such as `MeterRegistry(12 families, 340 series)`.
     *
     * @return the registry as text.
     * @since 1.0.0
     */
    override fun toString(): String = "MeterRegistry(${core.familyCount} families, ${context.budget.count} series)"

    private fun observed(
        name: String,
        kind: MeterKind,
        configure: FamilyBuilder.() -> Unit,
        observe: ObservationSink.() -> Unit,
    ): Registration<ObservedFamily> {
        val builder = FamilyBuilder(MeasureUnit.NONE).apply(configure)
        val request = requestOf(name = name, kind = kind, builder = builder, variant = Variant.Observed)
        return core.observe(request, observe)
    }

    private fun requestOf(
        name: String,
        kind: MeterKind,
        builder: FamilyBuilder,
        bounds: ExplicitBounds? = null,
        states: List<String> = emptyList(),
        variant: Variant = Variant.Plain,
    ): Request = Request(
        name = name,
        kind = kind,
        unit = builder.unit,
        description = builder.description,
        keys = builder.keys,
        bounds = bounds,
        states = states,
        variant = variant,
    )

    private fun declareOwnFamilies() {
        observedCounter("kollect.measurements.rejected", {
            description = "The measurements and series the registry rejected, by reason."
            labelKeys("reason")
        }) {
            Rejection.entries.forEach { rejection ->
                observe(
                    rejections.count(rejection).toDouble(),
                    rejection.label,
                )
            }
        }
        observedCounter("kollect.declarations.refused", {
            description = "The declarations of families the registry refused, by reason."
            labelKeys("reason")
        }) { RegistryCore.REFUSAL_LABELS.forEach { label -> observe(core.refusalCount(label).toDouble(), label) } }
        observedGauge("kollect.registry.families", {
            description = "The families the registry holds."
            unit = MeasureUnit.annotation("family")
        }) { observe(core.familyCount.toDouble()) }
        observedGauge("kollect.registry.series", {
            description = "The series the registry holds, the reserved overflow series included."
            unit = MeasureUnit.annotation("series")
        }) { observe(context.budget.count.toDouble()) }
        observedCounter("kollect.events.rejected", {
            description = "The events the registry could not publish."
        }) { observe((context.listener as? RegistryEvents)?.run { rejected.toDouble() } ?: 0.0) }
    }
}
