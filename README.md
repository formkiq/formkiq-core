<img width="100%" src="https://github.com/formkiq/formkiq-core/raw/master/images/logo.png"/>

<center>

[Get started](https://formkiq.com) · [Docs](https://docs.formkiq.com/) · [Issues](https://github.com/formkiq/formkiq-core/issues) · [Discord](https://discord.gg/q5K5cRhymW) · [Get in touch](mailto:info@formkiq.com)

</center>

[![Star us on GitHub](https://img.shields.io/github/stars/formkiq/formkiq-core?color=FFD700&label=Stars&logo=Github)](https://github.com/formiq/formkiq-core)
[![twitter](https://img.shields.io/twitter/follow/formkiq?style=social)](https://twitter.com/intent/follow?screen_name=RevertdotDev) 
<a href="https://github.com/formkiq/formkiq-core/tree/main/LICENSE.txt" target="_blank">
<img src="https://img.shields.io/static/v1?label=license&message=MIT&color=white" alt="License">
</a>

## Document Layer for your Application

Storing documents in your application isn't as easy as it should be and most of the time they end up getting stuffed into your data layer.

FormKiQ acts as your document layer along side your data layer of your application. It handles the storage and metadata of your documents so you don't have to.

<div align="center" style="margin: 30px;">
<img src="https://github.com/formkiq/formkiq-core/blob/1af9e28504f467c8b2ea91b4a497a868ee7f2740/docs/images/multi-tier-architecture.png" style="width:600px;" align="center" />
</div>

### Why storing documents in a database is not ideal:

* Storage: Increased storage costs
* Performance: Slows down database performance
* Security Concerns: Lack of data controls

### Why storing documents in AWS S3 is not enough:

* Lacks robust Metadata and Tagging
* Does not support Full-text searching
* Missing fine grain access controls

FormKiQ is an API document layer platform built on top of [AWS Managed Service](https://aws.amazon.com/managed-services/) to store and manage your documents, so you don't have to build it.

And it ALL runs in *your* AWS account.
</div>

### Features

✅ API-First Full Featured Document Management API

✅ Easy Integration with Existing Applications

✅ Supports Document Metadata / Tagging

✅ Built Using Serverless Services (no servers to maintain or manage)

✅ Easily scales to thousands of concurrent requests

✅ Google Drive like Web UI for Document Management

✅ Supports Both Multi-Tenant and Multi-Instance

✅ [Commerical Support Available](https://www.formkiq.com)

## Demo

Try out our [FormKiQ Readonly Demo](https://demo.tryformkiq.com/?demo=tryformkiq)

## Installation

The following are AWS CloudFormation scripts that can be used to install FormKiQ and other resources in a single click!

Follow our [Quickstart Guide](https://docs.formkiq.com/docs/getting-started/quick-start) to get started!

Then checkout out our [API Walkthrough](https://docs.formkiq.com/docs/getting-started/api-walkthrough)

## Archecture

<div align="center" style="margin: 30px;">
<br />
<img src="https://raw.githubusercontent.com/formkiq/formkiq-core/master/docs/images/formkiq_architecture.png" style="width:600px;" align="center" />
<br /><br /><br />
</div>

## Client SDKs

The following are official FormKiQ client SDKs that were generated using the [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator)

* [Java](https://github.com/formkiq/formkiq-client-sdk-java)
* [Python](https://github.com/formkiq/formkiq-client-sdk-python)
  
## Tutorials

After FormKiQ is installed checkout the following links to continue on your journey!

* [FormKiQ API Reference](https://docs.formkiq.com/docs/category/api-reference)
* [Tutorials](https://docs.formkiq.com/docs/category/tutorials)
* [How-To](https://docs.formkiq.com/docs/category/how-to)

## Support

* [Documentation](https://docs.formkiq.com)
* [Blog](https://blog.formkiq.com)
* [File a bug report](https://github.com/formkiq/formkiq-core/issues/new)
* [Paid Support](https://www.formkiq.com/pricing)

## License

MIT - 2020-2023 (c) FormKiQ, Inc. For more details, see LICENSE file.
