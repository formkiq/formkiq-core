# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Gradle build (see `settings.gradle`): AWS adapters (`aws-*`), Lambdas (`lambda-*`, `lambda-*-graalvm`), shared libs (`fkq-*`, `http*`, `strings`), and UI (`console`).
- Source lives under each module’s `src/main/java` and `src/main/resources`; tests under `src/test/java` with fixtures in `src/test/resources`.
- Infrastructure templates and assets: `src/main/resources/cloudformation/`, `docs/`, `images/`, `docker/` and `docker-compose*.yml` for local stacks.

## Build, Test, and Development Commands
- `./gradlew clean build` — compile all modules, generate CloudFormation artifacts, and run unit tests.
- `./gradlew test` — run tests only; respects `-Ptestregion`, `-Ptestprofile`, `-Ptestappenvironment`, `-Ptestchatgptapikey` when AWS context is required.
- `./gradlew spotlessCheck` — verify formatting; use `spotlessApply` before pushing.
- `./gradlew licenseReport` — regenerate license inventory under `docs/licenses/`.

## Coding Style & Naming Conventions
- Java code formatted via Spotless/Eclipse profile (`spotless.eclipseformat.xml`); let the formatter decide indentation and wrapping.
- Use descriptive, AWS-aligned names for modules/resources (e.g., `lambda-s3`, `aws-dynamodb`), and keep package names consistent with service boundaries.
- Prefer immutable data where practical; validate inputs at module edges (API handlers, S3 triggers, event listeners).

## Testing Guidelines
- Default to JUnit tests in `src/test/java`; mirror package structure of the code under test.
- When tests hit AWS-dependent flows, supply Gradle properties (`-Ptestregion`, etc.) pointing to sandbox credentials/profiles.
- Mark slow/integration tests with JUnit tags to allow selective execution (e.g., `./gradlew test -DexcludeTags=integration`).
- Add fixtures in `src/test/resources`; avoid hardcoding secrets.

## Commit & Pull Request Guidelines
- Commit messages follow short, imperative summaries and often include issue references (e.g., `#443 - Enable a "Group" for API Keys`).
- PRs should describe scope, risks, and deployment impacts; link issues/tickets; add screenshots for `console` UI changes.
- Update docs or CloudFormation templates when behavior or contract changes; note backward-compatibility and migration steps.

## Security & Configuration Tips
- Do not commit AWS credentials; rely on `~/.aws/credentials` profiles or runtime environment vars.
- Keep generated CloudFormation outputs reproducible: run Gradle tasks instead of manual edits, and verify hashes produced by `ytt` specs.
