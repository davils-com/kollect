# Integrating kollect

kollect speaks no HTTP: it hands you bytes, a content type and a status, and takes bytes, a content type and the
source you authenticated. These recipes show the wiring for common stacks. They are sketches of your application's
code, not part of kollect; `readAtMost` stands for reading a body with a bound, which every stack offers in its own way.

## One registry per application

Create the registry, the exposition and, where needed, the ingestion and the health registry once at start-up and pass
them where they are needed. There is no global registry.

```kotlin
val registry = meterRegistry()
val exposition = exposition()
val health = healthRegistry(registry) { indicator("database") { if (database.ping()) Health.Up else Health.down() } }
val probes = ProbeRenderer()
```

## Ktor

```kotlin
routing {
    get("/metrics") {
        val format = negotiateExposition(call.request.headers[HttpHeaders.Accept])
        when (val result = exposition.write(format, registry.snapshot())) {
            is ExpositionResult.Written ->
                call.respondBytes(result.payload.copyBytes(), ContentType.parse(result.payload.contentType))
            is ExpositionResult.Refused -> call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

    get("/health/live") { call.answer(probes.live()) }
    get("/health/ready") { call.answer(probes.ready(latestReport)) }

    authenticate("metric-clients") {
        post("/v1/metrics") {
            val principal = call.principal<UserIdPrincipal>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val body = call.readAtMost(ingestion.limits.maximumPayloadBytes)
            val result = ingestion.ingest(Source.of(principal.name), call.request.contentType().toString(), body)
            call.answer(result.verdict.statusCode, result.response)
        }
    }
}
```

`answer` maps a status and a `Payload` to `respondBytes`. Serve `/metrics` on an internal port, or put it behind
authentication your Prometheus can use.

## Servlet containers

```kotlin
class MetricsServlet(private val registry: MeterRegistry, private val exposition: Exposition) : HttpServlet() {
    override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        val format = negotiateExposition(request.getHeader("Accept"))
        when (val result = exposition.write(format, registry.snapshot())) {
            is ExpositionResult.Written -> {
                response.contentType = result.payload.contentType
                response.setContentLength(result.payload.size)
                result.payload.writeTo { chunk, length -> response.outputStream.write(chunk, 0, length) }
            }
            is ExpositionResult.Refused -> response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
        }
    }
}
```

`writeTo` hands the body over in chunks of at most 8 KiB, so a large exposition is not copied in one piece.

## Browser and desktop applications

A client cannot be scraped, so it pushes. On Wasm, build the transport on `fetch`; on the desktop, on the HTTP client
you already use:

```kotlin
val exporter = metricExporter(
    scope = applicationScope,
    registry = registry,
    resource = resource("storefront-web") { attribute("service.version", BuildInfo.VERSION) },
    transport = Transport { payload ->
        val answer = post("/v1/metrics", payload.contentType, payload.copyBytes())
        OtlpResponses.deliveryOf(answer.status, answer.body, answer.retryAfter)
    },
)
```

Send it to your own server's ingestion, which authenticates the user and exposes the client metrics next to its own,
or to an OpenTelemetry Collector or Grafana Alloy. When a page closes, `exporter.shutdown(timeout)` sends what it can;
for the very last snapshot, `encodeOtlp(registry.snapshot(), resource)` gives payloads small enough for
`navigator.sendBeacon`.

Do not put user ids, e-mail addresses or free text into labels of a client: they become series on your server and
cardinality on Prometheus.

## Prometheus and Grafana

Point a scrape job at `/metrics`; Prometheus negotiates OpenMetrics and reads `_created`, units and state sets. In
Grafana, the families appear with their Prometheus names: `http.server.request.duration` with unit seconds becomes
`http_server_request_duration_seconds_bucket`, `_sum` and `_count`. Ingested client series carry the label
`kollect_source`, so a dashboard can split or filter by tenant.

An OTLP receiver such as Mimir or Prometheus with `--web.enable-otlp-receiver` translates the same names the same way,
so dashboards work for scraped and pushed metrics alike.
