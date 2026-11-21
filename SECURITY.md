# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kollect/security/advisories/new),
or by email to development@davils.com.

Please include the affected kollect version and target (JVM or Wasm), a description of the impact, the steps to
reproduce it, ideally as a Kotlin test, and any mitigation you are aware of. We acknowledge a report within three
working days and agree on a disclosure date with you.

## Threat Model

kollect is a library: it runs in the process and with the privileges of the application that uses it. It assumes
that an attacker controls:

- **label values** an application records, such as paths, user agents or tenant names,
- the **bodies of OTLP requests** an application passes to an ingestion, every byte of them,
- the **`Accept` header** of a scrape,
- **names** that come from configuration, such as the names of meters, label keys and health members,
- the **failures** that filters, callbacks, indicators, tracers and transports throw, including their messages.

The sinks that must stay safe are the Prometheus and OpenMetrics expositions, the OTLP payloads, the health probes,
`toString` output and exception messages, the series of other sources in an ingestion, and the availability of the
registry, the ingestion and the exporter.

kollect defends against this attacker with limits that are checked before any work, a protobuf reader that checks every
length before it allocates and charges every step to a work budget, a source label that only the application sets,
ordered instead of hashed structures for input, containment of every callback, escaping and refusal at every text
sink, and redaction of sensitive labels. The limits are the properties of `RegistryLimits`, `ExpositionLimits`,
`ExportLimits`, `IngestLimits` and `HealthLimits`; their KDoc lists every default and every hard ceiling.

### What the application is responsible for

kollect speaks no HTTP, so these belong to the application:

- **Authentication of senders.** An ingestion trusts the `Source` the application hands it. Map an authenticated
  identity to a source, never a value from the request.
- **Decompression with a limit.** kollect reads uncompressed protobuf. Decompress a gzip body with a bound no larger
  than `IngestLimits.maximumPayloadBytes`, so a small request cannot expand without limit.
- **Protection of `/metrics`.** Metrics show the structure and the load of an application. Serve them on an internal
  port or behind authentication.
- **Protection of detailed probes.** `ProbeDetail.Checks` shows every health member; serve it only to authenticated
  callers. The default `StatusOnly` is safe for anybody.
- **TLS** for every transport and every route.

## Guarantees

- A histogram point always satisfies `count == sum of buckets`, and its sum, minimum and maximum belong to the same
  observations, also under concurrent recording.
- Every exported point is accounted for exactly once: delivered, rejected by the receiver, dropped with a reason or
  pending. Nothing disappears silently.
- A series of one ingestion source never carries the points of another, and a counter of a source never decreases
  because one of its instances falls silent.
- A failing filter, callback, indicator, tracer, observer or transport never disturbs the registry or its callers.
- Health is Down when kollect cannot tell, unless a rule loosens it to Degraded.

## Scope

The following are **in scope**:

- input within the configured limits that makes kollect use unbounded memory or CPU, crash, or deadlock,
- an OTLP request that makes an ingestion throw, allocate beyond its limits or work beyond its budget,
- a sender that can write into the series of another source, or set the label `kollect.source`,
- untrusted text that reaches an exposition, a probe, a `toString` or an exception message unescaped, or that breaks
  the format of an exposition,
- a sensitive label value that leaves the process although its key matches the active `LabelRedaction`,
- a torn histogram point, a counter that decreases, or an export accounting that does not add up,
- a structure that grows beyond its limit, or a limit beyond its hard ceiling,
- health that reports Up for a member without data, with stale data or with a failing check.

The following are **not** vulnerabilities in kollect:

- a vulnerability in the JDK, the JavaScript host, the Kotlin standard library, kotlinx.coroutines, Kore or konfetti,
- an application that raises a limit to its hard ceiling and runs out of memory within it,
- an application that hands an unauthenticated value to an ingestion as its `Source`,
- an application that serves `/metrics` or detailed probes to the public,
- a secret recorded under a label key that the active `LabelRedaction` does not match,
- a callback that blocks its thread, since callbacks run on the caller's coroutines.

## Known Limitations

- **In memory only.** The registry, the export queue and the ingestion keep their state in memory; what is not
  delivered when the process ends is lost. Every loss before that is counted.
- **Cumulative by default.** A cumulative exporter replaces an unsent request by a newer one, so a receiver sees fewer
  but correct samples during an outage. Delta temporality is opt-in and drops data when its queue is full.
- **No exponential histograms, no summaries, no exemplars.** Ingestion rejects the points of exponential histograms
  and summaries as `UnsupportedType` and ignores exemplars.
- **A snapshot may stall.** A reader waits at most `maximumSnapshotStall` for the writers of a histogram; a family that
  is still busy is reported as `Unreadable` for that snapshot.
- `@DavilsInternal` is a compile-time opt-in, not an access boundary, and Kotlin `internal` is public in JVM bytecode.
  `module-info.java` exports no `internal` package of `kollect-core`.

## Verifying a Release

Releases are signed with the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063` and attested by the
pipeline. Verify the signature of every artifact before you use it.
