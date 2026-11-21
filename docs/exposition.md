# Exposition for Prometheus and Grafana

Prometheus scrapes an application over HTTP and Grafana reads from Prometheus, or from Mimir or Thanos behind it.
kollect writes the body of that scrape; your application serves it on a route of its choice.

## Formats

| Format | Content type | Written when |
|---|---|---|
| `ExpositionFormat.PrometheusText` | `text/plain; version=0.0.4; charset=utf-8` | by default |
| `ExpositionFormat.OpenMetrics` | `application/openmetrics-text; version=1.0.0; charset=utf-8` | the scrape accepts OpenMetrics 1.0.0 at least as much as anything else |

`negotiateExposition(accept)` reads the `Accept` header after RFC 9110 and picks the format. It reads at most 4,096
characters and 32 media ranges, falls back to the Prometheus text format for a missing or unreadable header and
never throws.

OpenMetrics carries more than the text format: `# UNIT` lines, `_created` samples with the start of every counter and
histogram series, state sets, info families and the closing `# EOF`. Prometheus asks for OpenMetrics by default.

## Writing a scrape

```kotlin
val exposition = exposition {
    limits = expositionLimits {
        maximumBytes = 8 * 1024 * 1024
        maximumSeries = 200_000
    }
}

val result = exposition.write(negotiateExposition(accept), registry.snapshot(), ingestion.snapshot())
```

`write` takes one or more snapshots and writes them in order, so a server can expose its own registry and the series it
ingested from clients in one scrape. The result is either

- `ExpositionResult.Written` with the `payload` to answer with, whose `contentType` is the content type of the format,
  and a `report`, or
- `ExpositionResult.Refused` with the `limit` that would have been exceeded: `maximumBytes` or `maximumSeries`.

An exposition is all or nothing: it never sends a truncated body that Prometheus would read as a drop of every series
that is missing. Answer a refusal with 503 and raise the limit or reduce the series.

## Families an exposition refuses

The text formats cannot carry everything a snapshot may hold. kollect never changes the text it is given to make it
fit; it leaves out the family and lists it in `report.refused` with its name and a `FamilyProblem`:

| Problem | Cause |
|---|---|
| `NameCollision` | The Prometheus name of the family, or of one of its samples such as `_total`, `_bucket` or `_created`, is taken by a family written before it, for example by an ingested family. |
| `InvalidText` | A label value or description holds a control character or a lone surrogate. |
| `DuplicateSeries` | Two series end up with equal labels, usually after redaction. |

Every other family is written. `report.families` and `report.series` count what was written. Watch `report.refused` in
your application, for example by counting it in a family of your own.

## Redaction

A secret that ends up in a label value must not leave the process. By default every label whose key contains
`password`, `secret`, `token`, `authorization`, `cookie`, `session` or `api_key` is written as `****`. Choose another
rule with `redaction`:

```kotlin
val exposition = exposition {
    redaction = LabelRedaction { key -> key.value.endsWith("_email") || LabelRedaction.Sensitive.redacts(key) }
}
```

`LabelRedaction.None` writes every value. Redaction may make two series equal; the family is then refused as
`DuplicateSeries` rather than merged.

## Escaping and numbers

Label values escape `\\` and `\"`; help texts escape `\\`, and `\"` as well in OpenMetrics. Text with a control
character, a line break included, is refused rather than escaped, so no value can start a line of its own. Names and
keys follow a grammar that needs no escaping. Numbers are written in their shortest exact decimal form, with `+Inf`,
`-Inf` and `NaN` where the formats ask for them, and identically on the JVM and on Wasm.

## Protecting the route

Metrics show the structure and the load of an application. Serve them on an internal port, or behind authentication
that your Prometheus can satisfy, such as a bearer token or mutual TLS.
