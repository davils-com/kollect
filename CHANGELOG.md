# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.1.0 - 2026-11-26

### Fixed

- **An exporter whose entropy fails waits before it retries.** The export backoff fell back to no wait at all when its
  entropy threw, so the retries of a failing export fired back to back; it now waits the whole ceiling of the attempt,
  as konfetti and kontinuity do.

### Changed

- **The export backoff and the guards of critical sections come from Kore 2.5.0.** kollect drops its own copies of
  the backoff arithmetic, the random fraction and the lock; the delays are unchanged.
- **Built with Kreate 5.5.2 and Gradle 9.9.2 on Kore 2.5.0 and konfetti 1.1.0**, with the version floors of the build
  tools raised.

## 1.0.1 - 2026-11-26

### Changed

- **The default entropy of the exporter comes from `SecureEntropy` of Kore.** The jitter of retries draws from the
  public secure random source of Kore 2.4.1 instead of the random bits of version 4 UUIDs; the behaviour and the
  public API are unchanged.
- **Built on Kore 2.4.1 and konfetti 1.0.1.**

## 1.0.0 - 2026-11-21

The first version of kollect: the metrics library of Davils as an open source library under the Apache License 2.0,
for Kotlin on the JVM and on `wasmJs`, built with Kreate 5.4.1 on Kore 2.3.2 and konfetti 1.0.0 and published to
`https://repo.davils.com`. The code comes from the metrics library of an internal Davils project and is hardened for
critical infrastructure on the way.

### Added

- **The build** of kollect with Kreate 5.4.1: one convention plugin per concern, strict dependency locking and
  verification, Detekt with every rule over main and test sources, Kover at 97 %, the API and semver check, Trivy,
  the standards audit and the assurance gate.
- **The registry** with counters, up-down counters, gauges, histograms, timers, long task timers, state sets, info
  families and observed counters and gauges, declared with OTel-style names, UCUM units and label keys, each
  declaration answered with `Declared`, `Existing` or `Refused`.
- **Filters** that deny families or drop labels when a family is declared.
- **Snapshots** in a deterministic order, with the start of every series, and a delta conversion for exporters.
- **Exposition** in the Prometheus text format 0.0.4 and OpenMetrics 1.0 with content negotiation, units, `_created`,
  state sets, info families, label redaction and a report of refused families.
- **OTLP/HTTP protobuf encoding** of snapshots, split into payloads by bytes and points, with a resource that carries
  only the attributes the application sets.
- **OTLP ingestion** of requests from clients such as browser and desktop applications, with sources set by the
  application, declared or prefixed families, delta to cumulative conversion, instances summed per source, the
  verdict and the response to answer with, for re-exposition next to the registry.
- **The export pipeline** with a transport of the application's choice, cumulative or delta temporality, a coalescing
  queue, retries with full jitter, throttling, flush, shutdown, an observer and exact accounting.
- **Health** with indicators, threshold rules over values, counts, means and rates, state rules, groups with impact,
  bounded parallel evaluation and probes for liveness and readiness in `application/health+json`.
- **Tracing** with a minimal tracer and a metered tracer that times spans by operation and outcome.
- **Events** of the registry and the health registry on konfetti topics.
- **Binders** for the metrics of a konfetti event bus, metered coroutine dispatchers, job monitors and, on the JVM, the
  memory, garbage collection, threads, classes, CPU and file descriptors of the JVM.
- **Limits** in `RegistryLimits`, `ExpositionLimits`, `ExportLimits`, `IngestLimits` and `HealthLimits`, each with a
  hard ceiling for every structure that grows with use.

### Security

- Histogram points are consistent under concurrent recording: their count equals the sum of their buckets, and their
  sum, minimum and maximum belong to the same observations. A reader waits a bounded time, never forever.
- Every family has an overflow series and the registry a global series budget; a refused meter allocates nothing.
- Names are checked for collisions after their translation to Prometheus, suffixes included.
- Invalid values, such as NaN, negative counts and too large increments, are rejected and counted.
- The protobuf reader checks every length before it allocates, reads bounded varints, refuses groups, decodes UTF-8
  strictly, never recurses and charges every step to a work budget; a malformed request is answered, never thrown.
- Ingested series carry the source the application authenticated; reserved attributes, replayed deltas, points out of
  time and senders beyond their rate are rejected.
- Expositions refuse control characters and lone surrogates instead of changing them, and redact sensitive labels.
- Health fails closed on missing, stale, unreadable and NaN data, on too few samples, on failing and slow indicators
  and on empty groups.
- Filters, callbacks, indicators, tracers, transports, observers and the entropy run contained.
- The runtime lives in packages the Java module does not export.
