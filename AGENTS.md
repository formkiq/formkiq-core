# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Gradle build (see `settings.gradle`): AWS adapters in `adapters/aws/`, application entry points and the console in `apps/`, business modules in `domain/`, shared code in `libs/`, test infrastructure in `testing/`, and distribution/specification projects in `packaging/`. The shared Lambda request handler remains in `fkq-lambda-core/`.
- Use the actual Gradle project paths, such as `:adapters:aws:s3`, rather than the former flat module names.
- Source lives under each module’s `src/main/java` and `src/main/resources`; tests under `src/test/java` with fixtures in `src/test/resources`.
- Infrastructure templates and assets: `src/main/resources/cloudformation/`, `docs/`, `images/`, `docker/` and `docker-compose*.yml` for local stacks.

## Build, Test, and Development Commands
- Use the Gradle wrapper and Java 25 toolchain configured by the build.
- Prefer checks scoped to the affected modules. For S3 changes: `./gradlew :adapters:aws:s3:test :adapters:aws:s3:checkstyleMain :adapters:aws:s3:checkstyleTest :adapters:aws:s3:spotlessCheck`.
- `./gradlew build` — run the broader build and verification tasks when cross-module changes warrant it; avoid `clean` unless stale outputs are part of the problem.
- `./gradlew test` — run tests only; respects `-Ptestregion`, `-Ptestprofile`, `-Ptestappenvironment`, `-Ptestchatgptapikey` when AWS context is required.
- Run the affected module’s `spotlessApply` after editing Java, then `spotlessCheck`, `checkstyleMain`, and `checkstyleTest`. Formatting alone does not satisfy Checkstyle; document fields and follow its naming and complexity rules.
- `./gradlew licenseReport` — regenerate license inventory under `docs/licenses/`.

## Coding Style & Naming Conventions
- Java code formatted via Spotless/Eclipse profile (`spotless.eclipseformat.xml`); let the formatter decide indentation and wrapping.
- Use descriptive, AWS-aligned names for modules/resources (e.g., `apps/lambda-s3`, `adapters/aws/dynamodb`), and keep package names consistent with service boundaries.
- Prefer immutable data where practical; validate inputs at module edges (API handlers, S3 triggers, event listeners).

## Testing Guidelines
- Default to JUnit tests in `src/test/java`; mirror package structure of the code under test.
- Every new or modified test must clearly separate setup, execution, and assertions with the literal comments `// given`, `// when`, and `// then`, in that order. Follow nearby tests for naming and fixture conventions. For multi-step scenarios, repeat the execution/assertion sections as needed.
- Keep the operation under test in the `// when` section and assertions in `// then`. For exception tests, define an `Executable` in `// when` and use `assertThrows` in `// then`, or follow an established nearby exception-test pattern that preserves these sections.
- Test observable behavior and meaningful failure paths. For concurrency and streaming, use bounded synchronization to prove overlap or incremental progress rather than relying on elapsed-time speed assertions.
- API and integration tests must use the request builders in `testing/test-utils/src/main/java/com/formkiq/testutils/api/` for request setup and execution, including fixture creation, instead of calling API clients directly. Use `submitOk(client, siteId)` for expected success and the builder's response helpers for expected errors. Extend an existing builder or add one in test-utils when the required operation is not supported.
- Prefer the existing local test infrastructure for AWS-dependent flows; tests using LocalStack or DynamoDB containers require Docker. When selected tests require AWS context, supply Gradle properties (`-Ptestregion`, `-Ptestprofile`, etc.) pointing to sandbox credentials/profiles.
- Run a focused test with `--tests 'fully.qualified.TestClass'`, then the affected module’s suite. Mark slow/integration tests with JUnit tags when appropriate, and use tag filters only when the selected Gradle test task explicitly configures them; do not assume `-DexcludeTags=integration` is supported.
- Add fixtures in `src/test/resources`; avoid hardcoding secrets.

## Commit & Pull Request Guidelines
- Commit messages follow short, imperative summaries and often include issue references (e.g., `#443 - Enable a "Group" for API Keys`).
- PRs should describe scope, risks, and deployment impacts; link issues/tickets; add screenshots for `console` UI changes.
- Update docs or CloudFormation templates when behavior or contract changes; note backward-compatibility and migration steps.

## Security & Configuration Tips
- Do not commit AWS credentials; rely on `~/.aws/credentials` profiles or runtime environment vars.
- Keep generated CloudFormation outputs reproducible: run Gradle tasks instead of manual edits, and verify hashes produced by `ytt` specs.
