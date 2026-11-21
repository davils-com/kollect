# Contributing to kollect

Thank you for helping to build kollect. This document explains how a change gets in. The binding rules for code,
tests and the build are in [AGENTS.md](AGENTS.md); read it before your first change.

## Code of Conduct

Everyone who takes part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Open an issue for anything larger than a small fix, so the approach can be agreed on first. Security problems are
   never reported as an issue; see [SECURITY.md](SECURITY.md).
2. Branch from `develop` and keep the change to one concern.
3. Run `./gradlew check` until it is green. It needs JDK 25 and network access.
4. Open a pull request against `develop` and describe what changed and why.

## Development Standards

### One command before you push

```bash
./gradlew check
```

It compiles with warnings as errors, runs Detekt, the tests on the JVM and on Wasm, coverage, the API check, the
license header check, the changelog check, Trivy, the standards audit and the Kreate assurance gate. A pull request is
only merged when it is green.

### Every point counts

kollect promises consistent snapshots and an exact account of every exported point. A change to the meters, the
histogram cells, the snapshot or the export pipeline ships with a test of these invariants and keeps the race tests in
`src/jvmUnitTest` green when they run many times in a row.

### Security first

Every label value, OTLP request, `Accept` header and failure may come from an attacker. A change checks its input
against the limits of kollect first, runs every callback contained, escapes every untrusted value it shows through
`SafeText` of Kore, never shows a sensitive label and fails closed. It ships with tests that feed hostile text and
hostile bytes through every new sink and that run every new limit up to its bound and past it.

### The public API is a gate

The `api` folder of `kollect-core` records its binary API, for the JVM and for the klib. A deliberate change is
recorded with `./gradlew kreateApiDump`, gets an entry in [CHANGELOG.md](CHANGELOG.md) and, when it breaks
compatibility, waits for the next major version.

### Documentation

Every public declaration has KDoc with `@since`. Internal and private declarations have none, and there are no other
comments.

## Changing a Dependency

Every dependency, the classpath of `build-logic` included, is locked and verified. After changing
`gradle/libs.versions.toml`, refresh both in one run and review what changed:

```bash
./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache
```

A new dependency other than a library of JetBrains, Kore or konfetti needs the approval of a maintainer. Kore, konfetti
and Kreate are used in released versions only, never as snapshots.

## Snapshots and Releases

`gradle.properties` holds the version in development. Every push to `develop` publishes it as a `-SNAPSHOT` to
`https://repo.davils.com/snapshots`. A release is the tag of that version without a `v`, such as `1.0.0`, pushed on a
commit of `main`; the pipeline signs it, publishes it to `https://repo.davils.com/releases` and attests it. Before the
tag, the version gets its section in [CHANGELOG.md](CHANGELOG.md).

## Commit Messages

Conventional Commits in English with the module as scope, such as `feat(core): bound the stall of a snapshot` or
`fix(core): keep the order of exposed series`. A breaking change is marked with `!` and a `BREAKING CHANGE:`
footer.
