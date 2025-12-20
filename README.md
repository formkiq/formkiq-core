<img width="100%" src="https://github.com/formkiq/formkiq-core/raw/master/images/logo.png"/>

<div align="center">

[Get Started](https://formkiq.com) · [Documentation](https://docs.formkiq.com/) · [API Reference](https://docs.formkiq.com/docs/category/api-reference) · [Contact Us](mailto:info@formkiq.com)

[![GitHub Stars](https://img.shields.io/github/stars/formkiq/formkiq-core?color=FFD700&label=Stars&logo=Github)](https://github.com/formkiq/formkiq-core)
<a href="https://github.com/formkiq/formkiq-core/tree/main/LICENSE.txt" target="_blank">
<img src="https://img.shields.io/static/v1?label=license&message=MIT&color=white" alt="License">
</a>

</div>

# FormKiQ Core

FormKiQ is a production-ready document management platform built on AWS serverless infrastructure that deployed in **your** AWS account. Deploy a complete API-first document layer alongside your application's data tier—handling storage, metadata, search, and access control without the complexity of building it yourself.

## Who it’s for
- Teams building secure file workflows that must remain in their AWS accounts.
- Regulated workloads needing auditability, retention, and least-privilege access.
- SaaS platforms delivering per-tenant document storage with isolation and signed links.
- Ops/support teams needing fast search over PDFs and images without custom pipelines.

## Capabilities at a glance
- Ingest via API Gateway, S3, or email; optional OCR pipeline for images/PDFs.
- Metadata and tagging for every document plus custom attributes.
- Full-text/OCR search and filters; show only what each user/tenant should see.
- Versioning, retention, and audit trails built-in; lifecycle controls via policies.
- Event hooks via Lambda/SNS/SQS/EventBridge to enrich, classify, notify.
- SDKs for Java and Python, plus OpenAPI for other languages.

## Why teams choose FormKiQ
- Data and keys stay in your AWS account; IAM-first access control and audit trails.
- Built-in metadata/tagging, OCR search, versioning, and retention—no custom boilerplate.
- Serverless stack that auto-scales; no servers to patch or capacity to size.
- Multi-tenant ready and SOC 2–aligned design for regulated workloads.

## Common use cases
- Records and retention vaults for HR, legal, and finance with audit trails and versioning.
- Intelligent ingestion: drop into S3/email/API, OCR + metadata tagging, and route via events.
- Compliance-focused file storage where data must remain in your AWS account with IAM-scoped access.
- Customer/partner file portals with per-tenant isolation and signed access links.
- Workflow enrichment: trigger Lambdas on uploads to classify, enrich metadata, notify via SNS/SQS/EventBridge.
- Searchable knowledge stores over PDFs/images for support and operations teams.

## Deployment options
- Deploy to your AWS account via CloudFormation (Quickstart below).
- Evaluate in the hosted demo environment (read-only).
- Inquire about managed workspaces if you prefer a turnkey setup.

## Quick Start
1. **AWS deploy**: [Quickstart Guide](https://docs.formkiq.com/docs/getting-started/quick-start) (CloudFormation).
2. **Hosted demo**: [Explore](https://explore.tryformkiq.com/) — Email: `demo@formkiq.com`, Password: `tryformkiq`.
3. **API walkthrough**: [Step-by-step](https://docs.formkiq.com/docs/getting-started/api-walkthrough) to integrate quickly.
   - Requirements: AWS account, CLI/CloudFormation access, Java/Gradle for local builds.

## Architecture
Serverless on AWS: Lambda + API Gateway + S3 + DynamoDB + OpenSearch, with optional modules for OCR, Typesense, and event handling.

<div align="center" style="margin: 30px;">
<img src="https://raw.githubusercontent.com/formkiq/formkiq-core/master/docs/images/formkiq_architecture.png" style="width:600px;" alt="FormKiQ architecture diagram" />
</div>

## Project structure & commands
- Modules: `lambda-*` and `lambda-*-graalvm` functions, AWS adapters (`aws-*`), shared libs (`fkq-*`, `http*`, `strings`), console UI (`console`).
- Templates/assets: `src/main/resources/cloudformation`, `docs/`, `images/`, `docker/`.
- Build/test: `./gradlew clean build` (full build + tests), `./gradlew test` (tests only), `./gradlew spotlessCheck` (format), `./gradlew licenseReport` (license inventory).

## Security, compliance, and scale
- Data, encryption keys, and access policies remain in your AWS account; IAM secures every interaction.
- Versioning and audit trails support retention and evidence needs for SOC 2/HIPAA-style controls.
- Serverless footprint scales with demand; designed for thousands of concurrent requests.

## Client SDKs
- **[Java SDK](https://github.com/formkiq/formkiq-client-sdk-java)** — best for JVM backends.
- **[Python SDK](https://github.com/formkiq/formkiq-client-sdk-python)** — scripting/data workflows.
- **[TypeScript SDK](https://github.com/formkiq/formkiq-client-sdk-typescript)** — web and Node.js apps.
- Other languages via [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) using the published spec.

## Client Tutorials
- **[Python SDK Tutorial](https://docs.formkiq.com/docs/tutorials/using-python-client-sdk)** — Python SDK Document API walkthrough
- **[TypeScript SDK Tutorial](https://docs.formkiq.com/docs/tutorials/using-typescript-client-sdk)** — TypeScript SDK Document API walkthrough

## Resources & support
- Docs: [Complete Documentation](https://docs.formkiq.com), [API Reference](https://docs.formkiq.com/docs/category/api-reference), [Tutorials](https://docs.formkiq.com/docs/category/tutorials), [How-To Guides](https://docs.formkiq.com/docs/category/how-to)
- Updates: [Blog](https://blog.formkiq.com)
- Issues/questions: [GitHub Issues](https://github.com/formkiq/formkiq-core/issues)
- Enterprise inquiries: [Contact](mailto:info@formkiq.com) or [formkiq.com](https://www.formkiq.com/pricing)

## License

MIT License - © 2020-2025 FormKiQ, Inc. See [LICENSE](LICENSE.txt) for full details.
