# OTLP export and ingestion

Processes that Prometheus cannot scrape, such as browser and desktop applications or short jobs, push their metrics
instead. kollect writes and reads OTLP/HTTP requests in Protocol Buffers, `application/x-protobuf`, the format of the
OpenTelemetry Collector, Grafana Alloy, Mimir and Prometheus with its OTLP receiver. kollect sends nothing itself: you
hand it a transport built on your HTTP client.

## Export

```kotlin
val exporter = metricExporter(
    scope = applicationScope,
    registry = registry,
    resource = resource("storefront") {
        attribute("service.version", "4.2.0")
        allow("cloud.region")
        attribute("cloud.region", "eu-central-1")
    },
    transport = Transport { payload ->
        val answer = httpClient.post(otlpUrl, payload.contentType, payload.copyBytes())
        OtlpResponses.deliveryOf(answer.status, answer.body, answer.retryAfter)
    },
) {
    interval = 30.seconds
    observer = ExportObserver { event -> exportEvents.add(event) }
}
```

The exporter runs one coroutine on the scope you give it, chooses no dispatcher and starts no thread, so it runs in a
browser as well as on a server. Cancelling the scope stops it without sending.

### The resource

`resource(serviceName)` names the process. kollect adds `service.instance.id`, a random UUID new for every resource, and
the name and version of kollect as the telemetry SDK. It detects nothing about the host: `service.version`,
`service.namespace` and `deployment.environment.name` may be added, and every other attribute only after `allow(key)`,
so no attribute reaches a receiver by accident.

### Deliveries

A transport returns a `Delivery`. `OtlpResponses.deliveryOf(status, body, retryAfter)` maps an OTLP/HTTP answer:

| Answer | Delivery | The exporter |
|---|---|---|
| 2xx | `Accepted(rejectedPoints)`, read from the partial success of the body | counts the points as delivered and rejected |
| 429, 503 | `Throttled(retryAfter)` | waits as long as the receiver asks, within `maximumRetryAfter` |
| 502, 504 | `Retryable` | retries with exponential backoff and full jitter, within `maximumAttempts` and `maximumBackoff` |
| every other status | `Rejected` | drops the request |

A transport that throws counts as `Retryable`. The jitter comes from the `Entropy` of konfetti, secure by default.

### Temporality and the queue

With `Temporality.Cumulative`, the default, a newer snapshot replaces every request that was not sent yet, because its
sums contain them; the replaced points count as `Superseded`. During an outage the receiver sees fewer samples, but
never wrong ones. With `Temporality.Delta` every request carries the change since the previous one and the queue drops
its oldest request when it holds `maximumQueuedBatches`.

### Accounting

`accounting()` returns an `ExportAccounting` for which `produced = delivered + rejected + dropped + pending` holds at
every moment, counted in points. Every dropped point has a `DropReason`: `QueueFull`, `Superseded`, `Rejected`,
`RetriesExhausted`, `ShutdownDeadline` or `TooLarge`. The observer receives `Delivered`, `Retrying` and `Dropped`
events; an observer that throws is ignored.

`flush(timeout)` takes a snapshot now and sends every waiting request. `shutdown(timeout)` sends a last snapshot and
drops what it could not send in time as `ShutdownDeadline`. Call it when the application stops.

### Encoding without an exporter

`encodeOtlp(snapshot, resource, limits)` returns an `OtlpBatch` of payloads within `maximumPayloadBytes` and
`maximumPointsPerPayload`, for an application that sends on its own schedule, such as with `navigator.sendBeacon`
when a page closes. Counters become monotonic sums, up-down counters non-monotonic sums, gauges gauges and histograms
explicit bucket histograms; state sets and info families follow the Prometheus compatibility rules of OpenTelemetry.
The encoding is deterministic.

## Ingestion

A server can take OTLP requests from its clients and expose them next to its own metrics in one scrape:

```kotlin
val ingestion = otlpIngestion {
    acceptFamily("client.page.load", MeterKind.Histogram, MeasureUnit.SECONDS, "page")
    acceptFamily("client.errors", MeterKind.Counter, MeasureUnit.annotation("error"), "kind")
}

fun receive(principal: Principal, contentType: String, body: ByteArray): Response {
    val result = ingestion.ingest(Source.of(principal.tenantId), contentType, body)
    return Response(result.verdict.statusCode, result.response.contentType, result.response.copyBytes())
}

val scrape = exposition().write(format, registry.snapshot(), ingestion.snapshot())
```

### Sources

A `Source` is who sent a request, as your application decided after it authenticated the caller: a tenant, a client id
or a device. kollect never takes it from the request. Every ingested series carries it as the label `kollect.source`,
and a point that tries to set `kollect.source`, `le`, `quantile` or an `otel.` attribute is rejected, so a sender cannot
pose as another. `forget(source)` drops every series of a source, such as after its access was revoked.

### Families

By default an ingestion accepts only declared families, with exactly their kind, unit and attribute keys.
`acceptAnyFamily("client.")` opens a prefix for generic OTLP clients; every limit still applies.

### What an ingestion does with a point

- **Instances are summed.** Several instances of a source, told apart by `service.instance.id`, add up to one series
  per source. When an instance falls silent for `instanceExpiry`, its sums stay in the totals, so counters stay
  monotonic.
- **Deltas become cumulative.** A delta that overlaps the time range of the previous one is a replay and rejected as
  `OutOfOrder`.
- **Time is checked.** Points further in the future than `maximumClockSkew` or older than `maximumPointAge` are
  rejected as `OutOfTime`.
- **Series expire.** A series without points for `seriesExpiry` disappears. Expiry runs when the ingestion is used; it
  starts no timer.

### Verdicts

| Verdict | Status | Meaning |
|---|---|---|
| `Accepted` | 200 | every point was taken |
| `PartiallyAccepted` | 200 | some points were rejected; the response reports how many |
| `Malformed` | 400 | not a valid OTLP request; the sender must not retry |
| `TooLarge` | 413 | the body exceeds `maximumPayloadBytes` or `maximumPointsPerRequest` |
| `UnsupportedContent` | 415 | the content type is not `application/x-protobuf` |
| `Throttled` | 429 | the source sends faster than `maximumPointsPerMinute`, or the ingestion holds `maximumSources` |

Rejected points are counted by `IngestRejection`: `UndeclaredFamily`, `Mismatch`, `InvalidLabels`, `InvalidValue`,
`OutOfTime`, `OutOfOrder`, `SeriesLimit` and `UnsupportedType` for exponential histograms and summaries. The response
never echoes the request.

### Hostile requests

The ingestion reads every request as hostile. The protobuf reader checks every length before it allocates, reads at
most ten bytes of a varint, refuses groups, decodes UTF-8 strictly, skips unknown fields without recursion and charges
every step to a work budget. A malformed request ends in `Malformed`; it never throws.

kollect reads uncompressed bodies. If your clients send gzip, decompress with a bound no larger than
`maximumPayloadBytes` before you call `ingest`.
