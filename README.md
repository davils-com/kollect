# kollect

kollect is the metrics library of Davils: meters with bounded cardinality and consistent snapshots, Prometheus and
OpenMetrics exposition for Grafana, OTLP/HTTP export and ingestion, fail-closed health checks and binders for the JVM,
coroutines and [konfetti](https://github.com/davils-com/konfetti), for Kotlin on the JVM and on WebAssembly
(`wasmJs`), built on [Kore](https://github.com/davils-com/kore) for use in critical infrastructure. It is published
under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kollect-core` | `com.davils:kollect-core` | The registry and its meters, filters, snapshots, Prometheus and OpenMetrics exposition, OTLP encoding and ingestion, the export pipeline, health, tracing, events on konfetti topics and the binders for konfetti, coroutines and the JVM |

kollect is a Kotlin Multiplatform library for `jvm` and `wasmJs`, written for Kotlin 2.4 or newer. On the JVM it
requires JDK 25; `kollect-core` is the named Java module `com.davils.kollect.core`. On Wasm it is tested under Node.js.

kollect speaks no HTTP. It writes and reads bytes with a content type and tells you which status to answer with; your
application owns the routes, the server, the client, authentication and TLS. That keeps every feature available on
every target and leaves the choice of the HTTP stack to you.

## Installation

kollect is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:kollect-core:1.0.1")
}
```

A Maven build names the JVM artifact directly: `com.davils:kollect-core-jvm`.

## A short tour

```kotlin
val registry = meterRegistry()

val requests = registry.counter("http.server.requests") {
    description = "Requests the server answered"
    unit = MeasureUnit.annotation("request")
    labelKeys("method", "status")
}.orThrow()

val duration = registry.timer("http.server.request.duration") {
    labelKeys("method")
}.orThrow()

duration.with("GET").time { handle(request) }
requests.with("GET", "200").increment()
```

A declaration returns a `Registration` that is `Declared`, `Existing` or `Refused` with a reason; `orThrow()` stops a
start-up that declares something invalid. `with(...)` never fails: beyond the series limit of a family it returns the
overflow series `otel.metric.overflow="true"`, and for a refused family it returns a meter that records nothing.

Serve the registry to Prometheus from the route of your choice:

```kotlin
val exposition = exposition()

fun scrape(accept: String?): Response {
    val format = negotiateExposition(accept)
    return when (val result = exposition.write(format, registry.snapshot())) {
        is ExpositionResult.Written -> Response(200, result.payload.contentType, result.payload.copyBytes())
        is ExpositionResult.Refused -> Response(503, "text/plain", "refused: ${result.limit}".encodeToByteArray())
    }
}
```

Push to an OpenTelemetry Collector, Grafana Alloy or Mimir instead, through a transport you write with your HTTP
client:

```kotlin
val exporter = metricExporter(
    scope = applicationScope,
    registry = registry,
    resource = resource("checkout") { attribute("deployment.environment.name", "production") },
    transport = Transport { payload ->
        val answer = httpClient.post(otlpUrl, payload.contentType, payload.copyBytes())
        OtlpResponses.deliveryOf(answer.status, answer.body, answer.retryAfter)
    },
)
```

The guides explain every part:

| Guide | What it covers |
|---|---|
| [docs/metrics.md](docs/metrics.md) | The registry, names and units, meters, labels and cardinality, filters, snapshots and the binders |
| [docs/exposition.md](docs/exposition.md) | Prometheus 0.0.4 and OpenMetrics 1.0, content negotiation, redaction and the report of refused families |
| [docs/otlp.md](docs/otlp.md) | OTLP export with retries and exact accounting, and the ingestion of OTLP requests from clients |
| [docs/health.md](docs/health.md) | Indicators, threshold and state rules, groups and the probes for liveness and readiness |
| [docs/integration.md](docs/integration.md) | Wiring kollect into Ktor, a servlet container and a browser application |

## Security

kollect assumes that label values, OTLP requests, `Accept` headers and the text of failures may come from an
attacker. Every structure is bounded by a limits class with hard ceilings, hostile bytes are decoded within a work
budget, every source of ingested metrics is fenced into its own series, sensitive labels are redacted, foreign code
runs contained, and health fails closed. [SECURITY.md](SECURITY.md) describes the threat model and how to report a
vulnerability.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access. [AGENTS.md](AGENTS.md) holds the rules every change follows and
[CONTRIBUTING.md](CONTRIBUTING.md) how a change gets in.

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
