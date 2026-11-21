# Health with kollect

A health registry answers whether the application can do its work: for a readiness probe of Kubernetes, a load
balancer or an operator. It runs indicators the application writes and rules over its meters, grouped into a tree.

## Health fails closed

Whatever kollect cannot decide is never Up:

| Situation | Status | Reason |
|---|---|---|
| An indicator throws | Down | `CheckFailed` |
| An indicator does not answer within its timeout | Down | `TimedOut` |
| A rule finds no family or no series | Down | `NoData` |
| A gauge is older than the `maximumAge` of its rule | Down | `Stale` |
| A value is NaN | Down | `NotANumber` |
| A rate or mean has too few readings | Down | `InsufficientSamples` |
| The family could not be read in this snapshot | Down | `Unreadable` |
| A group has no members | Down | `EmptyGroup` |

A rule may loosen this to Degraded with `undetermined = Undetermined.Degraded`, for a signal that is useful but not
essential. Nothing loosens it to Up.

## Declaring the tree

```kotlin
val health = healthRegistry(meters = registry) {
    limits = healthLimits { maximumConcurrentChecks = 8 }
    events(bus)

    group("database") {
        indicator("primary", timeout = 2.seconds) { if (primary.ping()) Health.Up else Health.down() }
        indicator("replica", impact = Impact.Degrading) { if (replica.ping()) Health.Up else Health.down() }
    }

    threshold("errors") {
        meter = "http.server.errors"
        statistic = Statistic.Rate(1.minutes)
        above(degraded = 1.0, down = 10.0)
    }

    threshold("disk") {
        meter = "disk.free.ratio"
        maximumAge = 1.minutes
        below(degraded = 0.2, down = 0.05)
    }

    state("breaker") {
        meter = "payments.breaker.state"
        down("Open")
        degraded("HalfOpen")
    }
}
```

Member names are lower-case and may not contain dots; a member is addressed by its path, such as `database.replica`.

- **Indicators** are suspending functions that return `Health.Up`, `Health.degraded(reasons)` or
  `Health.down(reasons)`. Each runs within its timeout, at most `maximumCheckTimeout`. An indicator that blocks a
  thread without suspending cannot be cancelled; keep them suspending.
- **Threshold rules** read a `Statistic` from every series of a family: `Value` of a counter or gauge, `Count` or
  `Mean` of a histogram, or the `Rate` per second of a counter or a histogram count over at least a window. A family
  is as unhealthy as its worst series. `above` and `below` take a Degraded and a Down threshold, each optional.
- **State rules** read a state set and name the states that are Down or Degraded.
- **Groups** are as healthy as their worst member. A member with `Impact.Degrading` can make its group Degraded at
  worst, for optional dependencies such as a cache; `Impact.Critical`, the default, can make it Down.

Rates are measured from readings the rule keeps between evaluations, at most `maximumCheckpoints`, so a rule needs at
least two evaluations a window apart before it decides. A counter that restarts starts its rate anew.

## Evaluating

`health.evaluate()` takes one snapshot of the meter registry for all rules and runs the indicators in parallel on the
calling coroutine, at most `maximumConcurrentChecks` at a time. Overlapping evaluations run one after the other. The
`HealthReport` holds the overall status and every member with its status and reasons, by path.

With `events(bus)` the registry publishes `HealthEvent.StatusChanged` on `MetricTopics.HEALTH` whenever the status of
the registry or of a member changes.

## Probes

`ProbeRenderer` renders reports in `application/health+json` after the Internet-Draft for health check responses:

| Probe | Response |
|---|---|
| `live()` | 200, `pass`: the process runs |
| `ready(report)` while Up | 200, `pass` |
| `ready(report)` while Degraded | 200, `warn` |
| `ready(report)` while Down | 503, `fail` |

By default the body holds only the status, so a probe anybody can reach learns nothing about the inside of the
application. `ProbeRenderer(ProbeDetail.Checks)` adds every member with its status and reasons; serve it only to
authenticated callers.

Evaluate on a schedule of your own and answer probes from the latest report, so a burst of probes does not run every
check again.
