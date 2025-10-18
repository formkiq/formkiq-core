<img width="100%" src="https://github.com/formkiq/formkiq-core/raw/master/images/logo.png"/>

<div align="center">

[Get Started](https://formkiq.com) · [Documentation](https://docs.formkiq.com/) · [API Reference](https://docs.formkiq.com/docs/category/api-reference) · [Contact Us](mailto:info@formkiq.com)

[![GitHub Stars](https://img.shields.io/github/stars/formkiq/formkiq-core?color=FFD700&label=Stars&logo=Github)](https://github.com/formkiq/formkiq-core)
<a href="https://github.com/formkiq/formkiq-core/tree/main/LICENSE.txt" target="_blank">
<img src="https://img.shields.io/static/v1?label=license&message=MIT&color=white" alt="License">
</a>

</div>

## Enterprise-Grade Document Management Layer

FormKiQ is a production-ready document management platform built on AWS serverless infrastructure. Deploy a complete document layer alongside your application's data tier—handling storage, metadata, search, and access control without the complexity of building it yourself.

**Fully deployed in your AWS account** for complete control and security.

<div align="center" style="margin: 30px;">
<img src="https://raw.githubusercontent.com/formkiq/formkiq-core/master/docs/images/multi-tier-architecture.png" style="width:600px;" alt="Multi-tier architecture diagram" />
</div>

## Why FormKiQ?

Traditional approaches to document storage create technical debt and operational overhead:

**Database Storage Limitations:**
- Increased storage costs and database bloat
- Performance degradation under load
- Limited access control and audit capabilities

**Raw S3 Storage Gaps:**
- No built-in metadata management or tagging system
- Missing full-text search capabilities
- Lack of fine-grained access controls and versioning

**FormKiQ bridges these gaps** with a complete, API-first document management system built on proven AWS managed services—automatically scaling to handle enterprise workloads.

## Core Features

**Document Management**
- RESTful API with comprehensive document operations
- Advanced metadata and custom tagging
- Full-text search with OCR support
- Document versioning and audit trails

**Enterprise Ready**
- Serverless architecture (zero server management)
- Auto-scales to thousands of concurrent requests
- Multi-tenant and multi-instance deployment options
- SOC 2 compatible infrastructure

**Developer Experience**
- Official SDKs for Java and Python
- OpenAPI specification for custom integrations
- Comprehensive documentation and tutorials
- Web-based UI for document management

## Quick Start

Get FormKiQ running in your AWS account in minutes:

1. **[Follow our Quickstart Guide](https://docs.formkiq.com/docs/getting-started/quick-start)** - Deploy via CloudFormation
2. **[Try the Demo](https://explore.tryformkiq.com/)** - Explore features with read-only access
   - Email: `demo@formkiq.com`
   - Password: `tryformkiq`
3. **[Walk through the API](https://docs.formkiq.com/docs/getting-started/api-walkthrough)** - Integration examples

Or **[sign up for a managed workspace](https://formkiq.com)** to evaluate FormKiQ without AWS setup.

## Architecture

FormKiQ leverages AWS serverless services for reliability and scale:

<div align="center" style="margin: 30px;">
<img src="https://raw.githubusercontent.com/formkiq/formkiq-core/master/docs/images/formkiq_architecture.png" style="width:600px;" alt="FormKiQ architecture diagram" />
</div>

Built on: Lambda, API Gateway, S3, DynamoDB, OpenSearch, and other AWS managed services.

## Client SDKs

Official SDKs generated from our OpenAPI specification:

- **[Java SDK](https://github.com/formkiq/formkiq-client-sdk-java)** - Full-featured Java client
- **[Python SDK](https://github.com/formkiq/formkiq-client-sdk-python)** - Pythonic interface for FormKiQ

Additional language support available via [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator).

## Resources

**Documentation**
- [Complete Documentation](https://docs.formkiq.com)
- [API Reference](https://docs.formkiq.com/docs/category/api-reference)
- [Tutorials](https://docs.formkiq.com/docs/category/tutorials)
- [How-To Guides](https://docs.formkiq.com/docs/category/how-to)

**Community & Support**
- [Blog](https://blog.formkiq.com) - Updates and technical insights
- [GitHub Issues](https://github.com/formkiq/formkiq-core/issues) - Bug reports and feature requests
- [Enterprise Support](https://www.formkiq.com/pricing) - Professional support and SLAs

## License

MIT License - © 2020-2025 FormKiQ, Inc.

See [LICENSE](LICENSE.txt) for full details.