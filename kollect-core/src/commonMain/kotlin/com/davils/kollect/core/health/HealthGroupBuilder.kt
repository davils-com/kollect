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

package com.davils.kollect.core.health

import com.davils.kollect.core.health.rule.StateRuleBuilder
import com.davils.kollect.core.health.rule.ThresholdRuleBuilder
import com.davils.kollect.core.internal.health.BuildState
import com.davils.kollect.core.internal.health.GroupNode
import com.davils.kollect.core.internal.health.HealthNode
import com.davils.kollect.core.internal.health.IndicatorNode
import com.davils.kollect.core.internal.health.StateNode
import com.davils.kollect.core.internal.health.ThresholdNode
import com.davils.kollect.core.internal.health.ThresholdSpec
import com.davils.kollect.core.internal.text.NameRules
import com.davils.kollect.core.name.MetricName
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Declares the members of a group of a health registry: indicators, rules over the meters of the registry and nested
 * groups.
 *
 * Every member has a name of lower-case letters, digits and `_`, unique within its group. A declaration that breaks a
 * limit of [com.davils.kollect.core.limit.HealthLimits] or names nothing valid throws [IllegalArgumentException]
 * while the registry is built, so a registry never starts with a broken check.
 *
 * @since 1.0.0
 */
@DavilsDsl
public open class HealthGroupBuilder internal constructor(internal val state: BuildState, private val depth: Int) {
    internal val members: MutableList<HealthNode> = mutableListOf()

    /**
     * Adds an indicator.
     *
     * @param name the name of the member.
     * @param impact how far it may pull down its group.
     * @param timeout how long it may take; 5 seconds by default.
     * @param indicator the check.
     * @throws IllegalArgumentException if the name is invalid or taken, the timeout is not positive or above the
     *   limit, or the registry would have too many members.
     * @since 1.0.0
     */
    public fun indicator(
        name: String,
        impact: Impact = Impact.Critical,
        timeout: Duration = 5.seconds,
        indicator: HealthIndicator,
    ) {
        val maximum = state.limits.maximumCheckTimeout
        require(timeout.isPositive() && timeout <= maximum) { "The timeout of an indicator lies in (0, $maximum]" }
        add(IndicatorNode(name = checkedName(name), impact = impact, timeout = timeout, indicator = indicator))
    }

    /**
     * Adds a threshold rule over a family of the meter registry.
     *
     * @param name the name of the member.
     * @param impact how far it may pull down its group.
     * @param configure describes the rule.
     * @throws IllegalArgumentException if the name is invalid or taken, the registry has no meter registry, the rule
     *   names no valid family or no threshold, or the thresholds are not finite.
     * @since 1.0.0
     */
    public fun threshold(name: String, impact: Impact = Impact.Critical, configure: ThresholdRuleBuilder.() -> Unit) {
        val rule = ThresholdRuleBuilder().apply(configure)
        val thresholds = listOfNotNull(rule.degraded, rule.down)
        require(
            thresholds.isNotEmpty() && thresholds.all(Double::isFinite),
        ) { "A threshold rule has finite thresholds" }
        val spec = ThresholdSpec(
            statistic = rule.statistic,
            direction = rule.direction,
            degraded = rule.degraded,
            down = rule.down,
            maximumAge = rule.maximumAge,
            maximumCheckpoints = state.limits.maximumCheckpoints,
        )
        add(
            ThresholdNode(
                name = checkedName(name),
                impact = impact,
                meter = meterOf(rule.meter),
                undetermined = rule.undetermined,
                rule = spec,
            ),
        )
    }

    /**
     * Adds a state rule over a state set of the meter registry.
     *
     * @param name the name of the member.
     * @param impact how far it may pull down its group.
     * @param configure describes the rule.
     * @throws IllegalArgumentException if the name is invalid or taken, the registry has no meter registry or the rule
     *   names no valid family.
     * @since 1.0.0
     */
    public fun state(name: String, impact: Impact = Impact.Critical, configure: StateRuleBuilder.() -> Unit) {
        val rule = StateRuleBuilder().apply(configure)
        val node = StateNode(
            name = checkedName(name),
            impact = impact,
            meter = meterOf(rule.meter),
            undetermined = rule.undetermined,
            downStates = rule.downStates.toSet(),
            degradedStates = rule.degradedStates.toSet(),
        )
        add(node)
    }

    /**
     * Adds a group, as healthy as its worst member; a group without members is Down.
     *
     * @param name the name of the member.
     * @param impact how far it may pull down its group.
     * @param configure declares the members of the group.
     * @throws IllegalArgumentException if the name is invalid or taken, the groups nest too deep or the registry would
     *   have too many members.
     * @since 1.0.0
     */
    public fun group(name: String, impact: Impact = Impact.Critical, configure: HealthGroupBuilder.() -> Unit) {
        require(depth < state.limits.maximumDepth) { "Groups nest at most ${state.limits.maximumDepth} deep" }
        val checked = checkedName(name)
        val group = HealthGroupBuilder(state, depth + 1).apply(configure)
        add(GroupNode(checked, impact, group.members.toList()))
    }

    private fun checkedName(name: String): String {
        val isValid = NameRules.METRIC.matches(name) && '.' !in name
        require(isValid) { "${SafeText.quoted(name)} is no valid member name" }
        require(members.none { member -> member.name == name }) { "The member ${SafeText.quoted(name)} exists" }
        return name
    }

    private fun meterOf(meter: String): MetricName {
        require(state.hasMeters) { "A rule needs a health registry over a meter registry" }
        return MetricName.ofOrNull(
            meter,
        ) ?: throw IllegalArgumentException("${SafeText.quoted(meter)} is no metric name")
    }

    private fun add(member: HealthNode) {
        state.admit()
        members.add(member)
    }
}
