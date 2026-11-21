# AGENTS.md

Binding rules for every agent and every person working in this repository. kollect is the metrics library of Davils:
meters with bounded cardinality, consistent snapshots, Prometheus and OpenMetrics exposition for Grafana, OTLP/HTTP
protobuf export and ingestion, fail-closed health and binders for the JVM, coroutines and
[konfetti](https://github.com/davils-com/konfetti), for Kotlin on the JVM and on WebAssembly (`wasmJs`), built on
[Kore](https://github.com/davils-com/kore). It is published under the Apache License 2.0 and is built for critical
infrastructure, with security and correctness first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kollect-core` | `com.davils:kollect-core` | The library: registry, meters, filters, snapshots, Prometheus and OpenMetrics exposition, OTLP encoding and ingestion, the export pipeline, health, tracing, events and the binders |
| `build-logic` | - | One convention plugin per concern, composed by `kollect.kotlin-library` for the module and `kollect.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

`kollect-core` builds on `com.davils:kore-core`, `com.davils:konfetti-core` and `kotlinx-coroutines-core`. The code is
ported from the metrics library of an internal Davils project and hardened on the way. kollect is one module on
purpose: it speaks no HTTP, so every feature works on every target, and there is no split into client and server.

The root package is `com.davils.kollect.core` and the Java module has the same name. It is declared in
`kollect-core/src/jvmMain/java/module-info.java`, which exports only packages with public API, never an `internal`
package.

## Targets

`kollect-core` is a Kotlin Multiplatform library with two targets: `jvm` and `wasmJs`. The Wasm tests run under
Node.js.

- Code lives in `src/commonMain/kotlin`. What only the JVM offers, such as `LongAdder` and `ReentrantLock`, hides behind
  an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and `<Role>.wasmJs.kt`, and a parity
  test proves that both targets behave the same. kollect declares no `expect` or `actual` class: they are a Beta
  language feature.
- The JVM binder in `src/jvmMain/kotlin` is the only code for one target: it reads the MXBeans of the JDK. Its access
  to the platform lives in one `@DavilsPlatformBinding` object behind the `ManagementReadings` seam.
- kollect never chooses a dispatcher and never starts a thread or a timer: the exporter and the health registry run on
  the scope their caller hands them.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and extra
warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and `allRules` over
the main and the test sources, the tests on the JVM and on Wasm, Kover verification at 97 % of lines, branches and
instructions, the API and semver check, the license header check, the changelog check, every Trivy scan, the
standards audit and the Kreate assurance gate, which blocks on every finding. The gate reads OSV.dev and deps.dev and
Trivy downloads its database, so `check` needs the network. The build runs on JDK 25; toolchains are never downloaded.

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without a
baseline, for main and test sources alike. Build scripts and `build-logic` follow the same rules.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`. A new declaration gets the version it
  first ships in; existing `@since` values never change. KDoc states behaviour on invalid input, the limits that apply,
  thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`. At most two
  `return` statements per function.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **Named arguments** for every call with more than three arguments or with several arguments of one type.
- **One top-level type per file**, named after it; a file of functions is named after its role.
- **No package for a single file**, except the root package and `internal` packages.
- **No `!!`, no `lateinit`.**
- **Explicit API.** Everything is `internal` unless a consumer needs it.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `MeterRegistry`, never
  `KollectRegistry`. The ids of the Gradle plugins keep their `kollect.` prefix. kollect uses the annotations of Kore,
  such as `DavilsInternal` and `DavilsDsl`, and declares none of its own.
- **Immutable values.** Public types are immutable. No public `data class`: a public value writes `equals`,
  `hashCode` and `toString` by hand and is listed in the `UseDataClass` excludes of `detekt.yaml`; `value class` stays
  allowed.
- **Kotlin first.** Atomics come from `kotlin.concurrent.atomics` through a local `@OptIn(ExperimentalAtomicApi::class)`;
  experimental API never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no global registry, no global caches, no threads
  or timers of the library's own.
- **No logging.** kollect has no logger, no log observer and no log types. Telemetry runs only through its own meters,
  the `ExportObserver` and the events on konfetti topics; an application maps them to its own logs.
- **No HTTP.** kollect produces and consumes bytes with a content type and HTTP status codes. Routes, servers, clients
  and TLS belong to the application. No HTTP library, no Ktor, no SNMP.

## Metric rules

- **Snapshots are consistent.** A histogram point always satisfies `count == sum of buckets` and its sum, minimum and
  maximum belong to the same observations. The phased double buffer of `HistogramCell` keeps this without locks on the
  recording path; a reader that waits longer than `maximumSnapshotStall` reports the family as `Unreadable`, never a
  torn point.
- **Cumulative inside, delta at the edge.** The registry is always cumulative. Delta temporality exists only in the
  `DeltaConverter` of one exporter.
- **Cardinality is bounded.** Every family has an overflow series `otel.metric.overflow="true"`; the series of the
  registry are reserved against a global budget; a refused declaration or label set costs no allocation and returns a
  shared no-op meter. Nothing grows without a limit in `RegistryLimits`.
- **Names are checked after translation.** OTel-style names reach Prometheus through `PrometheusNaming` only; a
  collision after translation, suffixes included, refuses the later declaration or family.
- **Invalid values are counted, not recorded.** NaN, negative and too large values are rejected per meter and counted
  in `kollect.measurements.rejected`.
- **Health fails closed.** No data, stale data, NaN, too few samples, an unreadable family, a failing or slow indicator
  and an empty group are Down unless a rule explicitly loosens them to Degraded.
- **Every exported point is accounted for.** `produced = delivered + rejected + dropped + pending` holds for every
  exporter under retries, throttling, cancellation and shutdown. A change to the export pipeline ships with a test of
  this identity.
- **Numbers print the same everywhere.** Every number in text goes through `ShortestDecimal` of Kore, so the JVM and
  Wasm write identical expositions.

## Security rules

Every input of kollect may come from an attacker: label values, OTLP request bodies, `Accept` headers, names that come
from configuration and the text of failures. kollect is built so that a mistake of a caller fails closed.

- **Limits before work.** Families, series, labels, lengths, buckets, states, tasks, filters, exposition size, export
  queue and payloads, ingestion sources, series, rates and time windows and the health tree are bounded by
  `RegistryLimits`, `ExpositionLimits`, `ExportLimits`, `IngestLimits` and `HealthLimits`, whose hard ceilings no
  configuration exceeds. Nothing is truncated silently.
- **Hostile bytes are decoded within a budget.** The protobuf reader checks every length before it allocates, reads at
  most ten bytes of a varint, refuses groups, decodes UTF-8 strictly, never recurses and charges every step to a
  `WorkBudget`. A malformed request ends in `IngestVerdict.Malformed`, never in an exception.
- **Tenants cannot impersonate each other.** The application authenticates a sender and hands its `Source` to the
  ingestion; the ingestion writes it as the label `kollect.source` and refuses reserved attributes, so a sender can
  never set it. Only declared families are accepted unless `acceptAnyFamily` opens a prefix.
- **No input decides the complexity.** Structures keyed by input are `OrderedMap` and `OrderedSet` of Kore, never hash
  maps alone; grammars are `NameRule`s, never regular expressions.
- **Escape untrusted text exactly once, at the sink.** Exposition escapes label values by the specification and refuses
  a family with control characters or lone surrogates instead of changing it. Every `toString` and exception message
  that shows text from a caller writes it through `SafeText` of Kore.
- **Secrets never leak.** Labels whose key looks sensitive are written as `****` in every exposition by default. A
  resource carries only attributes the application set; kollect detects nothing about the host.
- **Foreign code is contained.** Filters, observed callbacks, health indicators, tracers, transports, observers and the
  entropy run inside `contained` or `attempted` of Kore. Their failure becomes a refusal, an unreadable family, a Down
  member or a counted failure, never an exception in the caller.
- **Randomness is secure by default.** The jitter of retries comes from the `Entropy` of konfetti, which defaults to
  secure randomness.
- **Hide what Java must not reach.** The runtime lives in `internal` packages that `module-info.java` never exports.
- **Every sink has an injection test** with hostile text, and every limit has a test at the limit and one past it.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test.
  Tests that need real threads or the JDK, such as the race tests of the histogram and the JVM binder, live in
  `src/jvmUnitTest/kotlin`. A `fixture` folder holds at least two files. Test support is never published.
- Virtual time through the test scheduler of Kotest and the `TestClock` of the fixtures.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- The module applies exactly one plugin, `id("kollect.kotlin-library")`, and declares nothing but its description and
  its dependencies.
- `build-logic` holds no Kotlin classes, no tasks and no Gradle API calls beyond reading the version catalog. Every
  plugin configures one concern and applies `kollect.base`:

  | Plugin | Configures |
  |---|---|
  | `kollect.base` | The Kreate plugin, the group and the version |
  | `kollect.dependency-locking` | Strict locking of every configuration |
  | `kollect.git-hooks` | The Git hooks |
  | `kollect.trivy` | Every Trivy scan |
  | `kollect.assurance` | The assurance gate and its `ignore` rules |
  | `kollect.standards` | The standards kollect follows, reviewed in `standards.lock` |
  | `kollect.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `kollect.web-tools` | Node.js and Yarn for the Wasm target |
  | `kollect.testing` | Kotest and the test suites |
  | `kollect.detekt` | Detekt and its reports |
  | `kollect.coverage` | Kover and its verification |
  | `kollect.api-validation` | The API dump and the semver check |
  | `kollect.docs` | Dokka |
  | `kollect.publish` | The POM, the release guard and the Davils repository |
  | `kollect.archives` | The legal files and the manifest of every archive |
  | `kollect.tool-classpaths` | The version floors on the class paths of the build tools |
  | `kollect.changelog` | The changelog check |
  | `kollect.license-header` | The license header check |

- Dependencies are declared in `gradle/libs.versions.toml`. Libraries of JetBrains, Kore and konfetti may be added
  when they are needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils`, through `exclusiveContent`; nothing is resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata in one run and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache
  ```

- kollect builds only with released versions of Kreate, Kore and konfetti, trusted by the Davils release key
  `36F578E8AD2D4391E0FA6C18154C625FAD370063`; no trust rule for unsigned snapshots is ever added.
- An `ignore` rule of the assurance gate has a reason, the exact module and an expiry no more than six months ahead.
  CRITICAL and HIGH findings can never be ignored.
- Every standard kollect follows is declared in `build-logic/src/main/kotlin/kollect.standards.gradle.kts` and
  reviewed with `./gradlew kreateStandardsReview --standard=<id> --note="..."`.

## Kore internals

kollect opts into these `@DavilsInternal` declarations of Kore; Kore changes them only in a major version:
`ShortestDecimal`, `OrderedMap`, `OrderedSet`, `SafeText.text`, `SafeText.quoted` and `SafeText.REDACTED`,
`WorkBudget`, `immutableList` and `immutableMap`, and the annotation `DavilsPlatformBinding`. A new use is added here
and to the table of Kore's `AGENTS.md`.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and `kreateApiSemverCheck`
on a version that does not match it. A deliberate change is recorded with `./gradlew kreateApiDump` and gets a
`CHANGELOG.md` entry. A breaking change needs a major version and a commit marked as breaking. Deprecate before
removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard` refuses every other version. Every
build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`, on a commit of `main`.
Snapshots go to `https://repo.davils.com/snapshots`, releases to `https://repo.davils.com/releases`, never to Maven
Central. The pipeline publishes with `publishAllPublicationsToDavilsRepository --no-configuration-cache` and signs
with `ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(core): ...` or `build: ...`. Small commits, one concern each. Commit only when asked. No `Co-Authored-By`
trailer and no other attribution to tools or AI assistants.
