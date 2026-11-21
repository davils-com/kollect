# Metrics with kollect

kollect measures what an application does in a registry of meter families. This guide explains how to declare
families, how names, units and labels work, what keeps the number of series bounded, and how to read snapshots.

## What kollect guarantees

| Guarantee | What it means |
|---|---|
| Consistent points | A histogram point always has `count == sum of buckets`; its sum, minimum and maximum belong to the same observations, also while other threads record. |
| Bounded cardinality | Every family holds at most `maximumSeriesPerFamily` series and the registry at most `maximumSeries`; what does not fit lands in the overflow series of its family. |
| No silent loss | Invalid values are rejected and counted in `kollect.measurements.rejected`; refused declarations are counted in `kollect.declarations.refused`. |
| Same output everywhere | Snapshots come in a deterministic order and every number is written by the same shortest-decimal algorithm on the JVM and on Wasm. |
| Nothing global | There is no global registry. You create one and pass it where it is needed. |

## The registry

```kotlin
val registry = meterRegistry {
    limits = registryLimits { maximumSeriesPerFamily = 500 }
    filter(MeterFilter.denyPrefix("debug."))
    events(bus)
}
```

`meterRegistry` takes a `Clock` for the timestamps of points and a `TimeSource` for durations; tests pass a test
clock. `events(bus)` publishes `RegistryEvent`s on the topic `MetricTopics.REGISTRY` of a konfetti bus: declared,
removed, overflowed and unreadable families, refused declarations and, once per snapshot, rejected measurements.

`close()` drops every family and refuses every later declaration with `Refusal.Closed`; meters obtained before keep
working but are no longer read.

## Declaring families

| Function | Meter | Prometheus type |
|---|---|---|
| `counter` | `Counter`: `increment()`, `add(n)` | counter |
| `upDownCounter` | `UpDownCounter`: `increment()`, `decrement()`, `add(n)` | gauge |
| `gauge` | `Gauge`: `set(value)` | gauge |
| `histogram` | `Histogram`: `record(value)` with explicit bounds | histogram |
| `timer` | `Timer`: `record(duration)`, `time { }`, in seconds | histogram |
| `longTaskTimer` | `LongTaskTimer`: `start()` returns a `LongTask` to close | gauges of active tasks and their duration |
| `stateSet<E>` | `StateSet<E>`: `set(state)` | stateset (OpenMetrics), gauges per state (Prometheus) |
| `info` | `Info`: `with(values)` | info (OpenMetrics), gauge `_info` (Prometheus) |
| `observedCounter`, `observedUpDownCounter`, `observedGauge` | a callback that observes values at every snapshot | counter or gauge |

```kotlin
val queue = registry.histogram("queue.wait") {
    description = "How long a job waits in the queue"
    unit = MeasureUnit.SECONDS
    bounds = ExplicitBounds.SECONDS
    labelKeys("queue")
}

when (queue) {
    is Registration.Declared -> queue.family
    is Registration.Existing -> queue.family
    is Registration.Refused -> queue.refusal
}
```

Every declaration returns a `Registration`:

- `Declared` for a new family.
- `Existing` for a family that was declared before with exactly the same kind, unit, description, label keys and
  bounds.
- `Refused` with a `Refusal`: `Invalid` (a name, key, unit or description breaks a rule), `Conflict` (the name or
  its Prometheus translation is taken), `Denied` (a filter denied it), `FilterFailed`, `LimitReached` or `Closed`.

`orThrow()` returns the family or throws, for declarations at start-up that must not fail. A refused registration still
carries a family whose meters record nothing, so code that ignores the refusal keeps working.

An observed family keeps its callback until you close it:

```kotlin
val pool = registry.observedGauge("db.pool.connections") { observe(pool.active().toDouble()) }.orThrow()
pool.close()
```

A callback that throws makes its family `Unreadable` for that snapshot; it never produces NaN.

## Names, units and labels

Names follow the OpenTelemetry style: parts separated by `.`, each starting with a lower-case ASCII letter and
continuing with lower-case letters, digits and single `_`, at most `maximumNameLength` characters. The grammar leaves
no room for text that could break out of an exposition. `PrometheusNaming` translates them in one deterministic way: dots become underscores,
the unit adds its suffix such as `_seconds`, and a counter gets `_total`. Two families whose translations collide,
suffixes of histograms included, are a `Conflict`; the second declaration is refused.

Names may not end in `_total`, `_created`, `_bucket` or `_info`, because the translation adds those.

Units are UCUM codes: `MeasureUnit.SECONDS`, `BYTES`, `RATIO`, `PERCENT`, `CELSIUS`, `VOLTS`, `AMPERES`, `WATTS`,
`JOULES`, `HERTZ`, `METERS` and `NONE`, or an annotation such as `MeasureUnit.annotation("request")`, written as
`{request}`, that adds no suffix.

Label keys are declared with the family and their values are given in the same order:

```kotlin
val requests = registry.counter("http.server.requests") { labelKeys("method", "route") }.orThrow()
requests.with("GET", "/orders").increment()
```

The keys `le` and `quantile` and every key starting with `otel.` are reserved. A value has at most
`maximumLabelValueLength` characters; control characters are refused by the exposition.

## Cardinality

Label values are where cardinality explodes: a user id or a raw path as a label makes a series per user or path.
kollect bounds that in three places:

1. A family has at most `maximumSeriesPerFamily` series.
2. The registry has at most `maximumSeries` series in all families together.
3. A family has at most `maximumLabels` label keys.

`with(...)` beyond a limit returns the **overflow series** of the family, labelled `otel.metric.overflow="true"`, and
publishes `FamilyOverflowed`. The overflow series keeps the totals right; it tells you that a label carries too
many values. `remove(values)` frees a series and `MeterRegistry.remove(name)` a family.

## Filters

A `MeterFilter` decides once, when a family is declared, with its descriptor: `Accept`, `Deny`, `Neutral` or
`Reshape(dropLabels)`. Filters see neither values nor measurements, cannot rename a family and fail closed when they
throw.

```kotlin
meterRegistry {
    filter(MeterFilter.dropLabels("instance_id"))
    filter { descriptor -> FilterDecision.Deny.takeIf { descriptor.name.value.startsWith("legacy.") } ?: FilterDecision.Neutral }
}
```

## Snapshots

`registry.snapshot()` returns a `MetricsSnapshot` of every family in name order and every series in label order.
Points are `SumPoint`, `GaugePoint`, `HistogramPoint`, `StateSetPoint` and `InfoPoint`, each with the labels and the
start of its series. A family whose histogram writers do not finish within `maximumSnapshotStall`, or whose callback
failed, is listed in `unreadable` instead.

The registry is always cumulative. An exporter with delta temporality computes the deltas from its own previous
snapshot.

The registry reports itself in the families `kollect.measurements.rejected`, `kollect.declarations.refused`,
`kollect.registry.families`, `kollect.registry.series` and `kollect.events.rejected`.

## Binders

| Binder | Families |
|---|---|
| `registry.bindBusMetrics(bus)` | `konfetti.topic.*` and `konfetti.consumer.*` per topic and consumer, `konfetti.bus.*` failures of observers and interceptors |
| `dispatcher.metered(registry, name)` | `coroutine.dispatcher.dispatched`, `.refused`, `.completed`, `.queued`, `.running`, `.wait` and `.duration` |
| `registry.jobMonitor(name).watch(job)` | `coroutine.jobs.active` and `coroutine.jobs.ended` by outcome |
| `registry.bindJvmMetrics(groups)` (JVM only) | `jvm.memory.*`, `jvm.gc.duration`, `jvm.thread.count`, `jvm.class.*`, `jvm.cpu.*` and, where the system has them, `jvm.file_descriptor.*`, after the OpenTelemetry semantic conventions |

The JVM binder reads garbage collections from the notifications of the JVM; its other families are observed at every
snapshot. Choose the groups with `JvmMetricGroup`; closing the returned `JvmMetrics` removes the families and the
subscription.

## Tracing

`Tracer` is a minimal interface for spans, with `Tracer.None` as the default. `tracer.metered(registry)` times every
span in the timer family `span.duration` with the labels `operation` and `outcome`, and `traced("operation") { span -> }`
ends the span with `Ok` or `Error`. A tracer that throws yields a span that does nothing.
