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

package com.davils.kollect.core.health

import com.davils.kollect.core.Payload
import com.davils.kollect.core.internal.health.Json

private const val CONTENT_TYPE: String = "application/health+json"
private const val OK: Int = 200
private const val UNAVAILABLE: Int = 503

/**
 * Renders health reports for liveness and readiness probes, in the format of the Internet-Draft "Health Check
 * Response Format for HTTP APIs".
 *
 * kollect serves no route: the application answers its probes with the response. By default the body tells only the
 * status, so an unauthenticated probe learns nothing about the inside of the application.
 *
 * @property detail how much the body tells.
 * @since 1.0.0
 */
public class ProbeRenderer(public val detail: ProbeDetail = ProbeDetail.StatusOnly) {
    /**
     * Answers a liveness probe: the process runs.
     *
     * @return a 200 response with the status `pass`.
     * @since 1.0.0
     */
    public fun live(): ProbeResponse = ProbeResponse(OK, payloadOf("""{"status":"pass"}"""))

    /**
     * Answers a readiness probe from [report].
     *
     * @param report the latest health report.
     * @return 200 with `pass` while Up, 200 with `warn` while Degraded, 503 with `fail` while Down.
     * @since 1.0.0
     */
    public fun ready(report: HealthReport): ProbeResponse {
        val status = Json.string(statusOf(report.status))
        val checks = ""","checks":${checksOf(report)}""".takeIf { detail == ProbeDetail.Checks }.orEmpty()
        val code = UNAVAILABLE.takeIf { report.status == HealthStatus.Down } ?: OK
        return ProbeResponse(code, payloadOf("""{"status":$status$checks}"""))
    }

    /**
     * Shows the renderer, such as `ProbeRenderer(StatusOnly)`.
     *
     * @return the renderer as text.
     * @since 1.0.0
     */
    override fun toString(): String = "ProbeRenderer($detail)"

    private fun checksOf(report: HealthReport): String = report.members.joinToString(
        prefix = "{",
        postfix = "}",
    ) { member ->
        val output = Json.string(member.reasons.joinToString(separator = ","))
        val status = Json.string(statusOf(member.status))
        """${Json.string(member.path)}:[{"status":$status,"output":$output}]"""
    }

    private fun statusOf(status: HealthStatus): String = when (status) {
        HealthStatus.Up -> "pass"
        HealthStatus.Degraded -> "warn"
        HealthStatus.Down -> "fail"
    }

    private fun payloadOf(json: String): Payload = Payload(json.encodeToByteArray(), CONTENT_TYPE)
}
