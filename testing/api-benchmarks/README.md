# FormKiQ API benchmarks

Run the same Gradle test against each deployed FormKiQ instance and retain the reports
for comparison. This module does not deploy or change Lambda architecture.

```sh
./gradlew :testing:api-benchmarks:test \
  --tests 'com.formkiq.benchmarks.ApiBenchmarkTest' \
  -PbenchmarkUrl=https://YOUR-IAM-API.execute-api.us-east-2.amazonaws.com \
  -Ptestregion=us-east-2 \
  -Ptestprofile=YOUR_SANDBOX_PROFILE \
  -PbenchmarkSiteId=YOUR_BENCHMARK_SITE \
  -PbenchmarkArchitecture=x86_64 \
  -PbenchmarkLabel=intel-baseline \
  -PbenchmarkRevision=YOUR_DEPLOYED_COMMIT
```

Repeat with the ARM instance URL, `-PbenchmarkArchitecture=arm64`, and a different label.
Keep the deployed revision, region, Lambda memory, timeout, concurrency, enabled modules,
authentication, logging, and backing-service capacity equivalent. Architecture and revision
are supplied labels; the runner does not inspect the AWS deployment.

Without `benchmarkUrl`, the live benchmark is skipped and only the local verification tests
run. Supplying the URL opts in to API traffic and document creation. Live test results are
never considered up to date or restored from the Gradle build cache.

## Authentication

- IAM: use `testregion` and `testprofile`. Use `-Ptestprofile=` for the AWS default credential
  provider chain, including an EC2 instance role. The repository's default profile is `dev`.
- JWT: set `FORMKIQ_BENCHMARK_TOKEN` in the test process environment. Supply the complete
  Authorization header value accepted by the target.
- API key: set `FORMKIQ_BENCHMARK_API_KEY` in the environment and use the API-key endpoint.
  FormKiQ expects this key in the `Authorization` header.

Set only one of the JWT/API-key variables. Credentials are not Gradle arguments and are not
written to benchmark reports. Tokens and IAM session credentials must remain valid for the
whole run. HTTPS is required except for loopback endpoints used in local verification.

The target principal needs document read/create/update/delete, attribute definition create/delete, and attribute search permissions
on the selected site. Use a dedicated benchmark site with compatible configuration and no
unrelated processing actions or required schema fields. Site configuration and other existing
documents are preserved. Site-wide list results include any existing data in that site.

## Workloads and settings

Each scenario is measured independently at every offered rate and repetition, after its own
warm-up. The default workload uses plain-text inline content. It covers:

| Scenario | API work |
| --- | --- |
| `version` | `GET /version` |
| `list_documents` | `GET /documents`, first page, limit 20 |
| `get_document` | `GET /documents/{documentId}` |
| `get_content` | `GET /documents/{documentId}/content`, verify text |
| `search` | `POST /search`, equality on a unique per-run string attribute, limit 20 |
| `create_document` | `POST /documents`, unique ID/path and inline text |
| `update_document` | `PATCH /documents/{documentId}`, one metadata value |

All API requests, including setup and cleanup, use the existing test-utils request builders.
Reads cycle through the fixture population; updates use one exclusive fixture per worker lane.
Setup registers a unique temporary string attribute definition and verifies content and search
readiness before measurement. The definition is removed after all owned documents are removed. Creates use unique document IDs
allocated before submission, including warm-up creates, and are tracked even if a response
fails or times out. Created documents are removed between warm-up and measurement and between
cells so the fixture population stays consistent.

| Gradle property | Default | Meaning |
| --- | --- | --- |
| `benchmarkUrl` | unset | API base URL, including stage path when required |
| `benchmarkArchitecture` | `unknown` | `x86_64`, `arm64`, or `unknown` |
| `benchmarkLabel` | architecture | Display label |
| `benchmarkSiteId` | `default` | Target site |
| `benchmarkRevision` | `unspecified` | Deployed source revision |
| `benchmarkScenarios` | all seven | Comma-separated scenario names |
| `benchmarkRates` | `10,50` | Offered requests per second, per scenario |
| `benchmarkRequests` | `100` | Measured scheduled requests per scenario/rate/repetition |
| `benchmarkWarmup` | `10` | Warm-up scheduled requests per cell |
| `benchmarkRepetitions` | `3` | Number of repetitions |
| `benchmarkConcurrency` | `16` | Maximum in-flight requests |
| `benchmarkFixtures` | `100` | Initial document count; at least the concurrency, maximum 10,000 |
| `benchmarkPayloadBytes` | `1024` | Inline ASCII text bytes |
| `benchmarkTimeoutSeconds` | `30` | Whole-call timeout |

The default schedules 4,200 measured and 420 warm-up operations, plus setup and cleanup.
Nominal scheduled traffic takes approximately 4 minutes 40 seconds; response drain and fixture
operations add time. Numeric inputs are bounded, total scheduled operations including warm-up
cannot exceed one million, and the live JUnit test has a two-hour timeout.

For a short connectivity check:

```sh
./gradlew :testing:api-benchmarks:test \
  --tests 'com.formkiq.benchmarks.ApiBenchmarkTest' \
  -PbenchmarkUrl=https://YOUR-API-ENDPOINT \
  -Ptestregion=us-east-2 -Ptestprofile=YOUR_SANDBOX_PROFILE \
  -PbenchmarkScenarios=version \
  -PbenchmarkRates=1 -PbenchmarkRequests=5 \
  -PbenchmarkWarmup=1 -PbenchmarkRepetitions=1
```

For a larger comparison, increase requests and repetitions after calibrating the generator
and the instance's sustainable rates. Collect at least 10,000 attempted requests per cell
before relying on p99. Alternate which architecture runs first and retain each repetition.

## Reports

Every execution writes a unique directory under:

```text
testing/api-benchmarks/build/reports/api-benchmarks/<timestamp>-<unique-id>/
  report.html       Standalone, readable configuration and results report
  results.json      Versioned configuration, status, aggregate measurements and failures
  samples.csv       One row per measured scheduled request
  documents.txt     IDs allocated by this run, including ambiguous creates
  attributes.txt    Temporary attribute definition allocated by this run
```

The HTML table shows offered rate, sent/success/error/drop counts, successful throughput,
all-attempt p50/p95/p99, successful-request p95, and scheduling delay p95. Raw samples include
HTTP status and API Gateway request ID where returned. Failures do not include API response
bodies, tokens, or request content. Report text is HTML escaped.

The generator schedules a fixed arrival rate with bounded concurrency. It records a drop
instead of queuing requests when all lanes are busy, or issuing catch-up bursts when the
scheduler falls behind. Drops count as unsuccessful scheduled operations and have no HTTP
latency. Connection retries and redirects are disabled. Timing covers the synchronous SDK
operation and response validation, including serialization/deserialization and network time.
Throughput includes the response drain and at least the scheduled traffic window.

Setup, warm-up, cleanup and manifest persistence are excluded from measured request latency.
Warm-up failures, measured errors, generator drops, setup failure or incomplete cleanup make
the JUnit test fail **after** writing reports. Latency percentiles include all attempted requests,
including errors; successful-request p95 is also reported. Empty distributions are `null` in
JSON and shown as an em dash in HTML. The normal Gradle/JUnit report is also available.

The runner deletes only document IDs allocated by the current run and writes unresolved IDs
to `remainingDocuments` in JSON. Unresolved attribute definitions are listed in
`remainingAttributes`; pre-existing attribute definitions are preserved. A not-found deletion after an ambiguous create is retained as
unresolved because that create could still commit. Flat document paths avoid creating folders.
Keep the manifest if the test process is forcibly terminated;
the normal cleanup/report path cannot execute after a process kill.

These are client-observed API benchmarks. Measured writes time API acceptance, not downstream
document processing completion. The report does not distinguish warm/cold Lambda invocations
or calculate Lambda billed duration, OCR performance, or AWS cost. Those need separate AWS
telemetry and workload extensions for the broader evaluation in Platform issue #313.

## Local verification

```sh
./gradlew :testing:api-benchmarks:test \
  :testing:api-benchmarks:checkstyleMain \
  :testing:api-benchmarks:checkstyleTest \
  :testing:api-benchmarks:spotlessCheck
```

The local tests use a loopback HTTP server, exercise the actual API request builders, and
verify reporting, failure handling, document ownership, percentiles and concurrency bounds.
No Docker or AWS credentials are required.
