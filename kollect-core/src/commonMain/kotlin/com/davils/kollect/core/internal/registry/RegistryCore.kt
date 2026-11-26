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

import com.davils.kollect.core.Family
import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Refusal
import com.davils.kollect.core.Registration
import com.davils.kollect.core.filter.MeterFilter
import com.davils.kollect.core.meter.Descriptor
import com.davils.kollect.core.meter.ObservationSink
import com.davils.kollect.core.meter.Rejection
import com.davils.kollect.core.name.MetricName
import com.davils.kollect.core.snapshot.MetricsSnapshot
import com.davils.kollect.core.snapshot.Unreadable
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.concurrency.Guard
import com.davils.kore.core.concurrency.guard
import com.davils.kore.core.outcome.Outcome

private const val MINIMUM_TOMBSTONES: Int = 64

internal class RegistryCore(val context: RegistryContext, filters: List<MeterFilter>) {
    private val guard: Guard = guard()
    private val checker: Checker = Checker(context.limits)
    private val chain: FilterChain = FilterChain(filters)
    private var members: OrderedMap<MetricName, Member> = OrderedMap()
    private var claims: OrderedMap<String, Descriptor> = OrderedMap()
    private var tombstones: Int = 0
    private var isClosed: Boolean = false
    private val refusals: LongArray = LongArray(REFUSAL_LABELS.size)
    private val reported: LongArray = LongArray(Rejection.entries.size)

    val familyCount: Int get() = guard.locked { members.size }

    fun refusalCount(label: String): Long = guard.locked { refusals[REFUSAL_LABELS.indexOf(label)] }

    fun <M> declare(request: Request, noop: M, create: () -> Created<M>): Registration<Family<M>> {
        val shaped = shapedOf(request)
        if (shaped !is Outcome.Success) return refused(request.name, failureOf(shaped), Family(null, null, noop))
        val checked = shaped.value
        val registration = guard.locked {
            admitted(
                checked = checked,
                variant = request.variant,
                noop = noop,
                create = create,
            )
        }
        context.listener.flush()
        return registration
    }

    @Suppress("MissingUseCall")
    fun observe(request: Request, callback: ObservationSink.() -> Unit): Registration<ObservedFamily> {
        val shaped = shapedOf(request)
        if (shaped !is Outcome.Success) return refused(request.name, failureOf(shaped), ObservedFamily(null) {})
        val registration = guard.locked { observed(shaped.value, callback) }
        context.listener.flush()
        return registration
    }

    fun remove(name: MetricName): Boolean {
        val removed = guard.locked { members.remove(name)?.also(::forget) }
        if (removed != null) context.listener.removed(name)
        context.listener.flush()
        return removed != null
    }

    fun close() {
        guard.locked {
            isClosed = true
            members.entries.forEach { (_, member) -> context.budget.release(member.held()) }
            members = OrderedMap()
            claims = OrderedMap()
        }
        context.listener.flush()
    }

    fun collect(): MetricsSnapshot {
        val now = context.clock.now()
        val current = guard.locked { members.entries.map { (_, member) -> member } }
        val collected = current.map { member -> member.collect(now) }
        val all = collected.flatMap(Collected::families)
        val families = all.sortedWith { first, second -> first.descriptor.name.compareTo(second.descriptor.name) }
        val unreadable = collected.mapNotNull(Collected::unreadable).sortedBy(Unreadable::name)
        reportRejections()
        context.listener.flush()
        return MetricsSnapshot(now, families, unreadable)
    }

    private fun shapedOf(request: Request): Outcome<Checked, Refusal> {
        val checked = checker.check(request)
        if (checked !is Outcome.Success) return checked
        return chain.apply(checked.value)
    }

    private fun <M> admitted(
        checked: Checked,
        variant: Variant,
        noop: M,
        create: () -> Created<M>,
    ): Registration<Family<M>> {
        val blocked = blockingOf(checked, variant)
        if (blocked != null) return existingOrRefused(blocked, checked, Family(null, null, noop))
        val active = checker.activeDescriptorOf(checked.descriptor).takeIf { variant == Variant.LongTask }
        val core = FamilyCore(
            descriptor = checked.descriptor,
            arity = checked.arity,
            kept = checked.kept,
            context = context,
            create = create,
        )
        val family = Family(checked.descriptor, core, noop)
        insert(
            RecordedMember(
                core = core,
                variant = variant,
                claims = checked.claims,
                family = family,
                active = active,
                context = context,
            ),
        )
        return Registration.Declared(family)
    }

    @Suppress("MissingUseCall")
    private fun observed(checked: Checked, callback: ObservationSink.() -> Unit): Registration<ObservedFamily> {
        val blocked = blockingOf(checked, Variant.Observed)
        if (blocked != null) return existingOrRefused(blocked, checked, ObservedFamily(null) {})
        val core = ObservedCore(checked.descriptor, context, callback)
        val family = ObservedFamily(checked.descriptor) { closeObserved(checked.descriptor.name, core) }
        insert(ObservedMember(core, checked.claims, family))
        return Registration.Declared(family)
    }

    private fun closeObserved(name: MetricName, core: ObservedCore) {
        val isCurrent = guard.locked { (members[name] as? ObservedMember)?.core === core }
        if (isCurrent) remove(name)
    }

    private fun blockingOf(checked: Checked, variant: Variant): Any? {
        val existing = members[checked.descriptor.name]
        if (isClosed || existing == null) return Refusal.Closed.takeIf { isClosed } ?: limitOf(checked, variant)
        return existingOrConflict(existing, checked, variant)
    }

    private fun existingOrConflict(member: Member, checked: Checked, variant: Variant): Any {
        val isEqual = member.descriptor == checked.descriptor && member.variant == variant
        return member.takeIf { isEqual } ?: Refusal.Conflict(member.descriptor)
    }

    private fun limitOf(checked: Checked, variant: Variant): Refusal? {
        val refusal = familyLimitOf() ?: claimConflictOf(checked)
        if (refusal != null || variant == Variant.Observed) return refusal
        return Refusal.LimitReached("maximumSeries").takeUnless { context.budget.tryAcquire() }
    }

    private fun familyLimitOf(): Refusal? {
        val isFull = members.size >= context.limits.maximumFamilies
        return Refusal.LimitReached("maximumFamilies").takeIf { isFull }
    }

    private fun claimConflictOf(checked: Checked): Refusal? {
        val owner = checked.claims.firstNotNullOfOrNull { claim -> claims[claim] }
        return owner?.let(Refusal::Conflict)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <F> existingOrRefused(blocked: Any, checked: Checked, refusedFamily: F): Registration<F> {
        if (blocked is Member) return Registration.Existing(blocked.family as F)
        val refusal = blocked as Refusal
        count(refusal)
        context.listener.refused(checked.descriptor.name.value, refusal)
        return Registration.Refused(refusedFamily, refusal)
    }

    private fun <F> refused(name: String, refusal: Refusal, refusedFamily: F): Registration<F> {
        guard.locked { count(refusal) }
        context.listener.refused(name, refusal)
        context.listener.flush()
        return Registration.Refused(refusedFamily, refusal)
    }

    private fun failureOf(shaped: Outcome<Checked, Refusal>): Refusal = (shaped as Outcome.Failure).failure

    private fun count(refusal: Refusal) {
        val index = REFUSAL_LABELS.indexOf(refusal.label)
        refusals[index] = refusals[index] + 1
    }

    private fun insert(member: Member) {
        members[member.descriptor.name] = member
        member.claims.forEach { claim -> claims[claim] = member.descriptor }
        context.listener.declared(member.descriptor)
    }

    private fun forget(member: Member) {
        member.claims.forEach { claim -> claims.remove(claim) }
        context.budget.release(member.held())
        tombstones += 1
        if (tombstones > maxOf(a = MINIMUM_TOMBSTONES, b = members.size)) compact()
    }

    private fun compact() {
        val compactedMembers = OrderedMap<MetricName, Member>()
        members.entries.forEach { (name, member) -> compactedMembers[name] = member }
        val compactedClaims = OrderedMap<String, Descriptor>()
        claims.entries.forEach { (claim, owner) -> compactedClaims[claim] = owner }
        members = compactedMembers
        claims = compactedClaims
        tombstones = 0
    }

    private fun reportRejections() {
        Rejection.entries.forEach { rejection ->
            val count = context.rejections.count(rejection)
            val delta = guard.locked { swapReported(rejection, count) }
            if (delta > 0) context.listener.rejected(rejection, delta)
        }
    }

    private fun swapReported(rejection: Rejection, count: Long): Long {
        val delta = count - reported[rejection.ordinal]
        reported[rejection.ordinal] = count
        return delta
    }

    companion object {
        val REFUSAL_LABELS: List<String> =
            listOf("invalid", "conflict", "limit_reached", "denied", "filter_failed", "closed")
    }
}
