![FormKiQ](https://raw.githubusercontent.com/formkiq/formkiq-core/master/docs/logo.png)

# 🥷 FormKiQ

FormKiQ is an open source Headless Document Management System (DMS) that run in your [Amazon Web Services(AWS)](https://aws.amazon.com) that gives you full control of your organization's documents. Built using AWS serverless services like [AWS Lambda](https://aws.amazon.com/lambda/), [Amazon API Gateway](https://aws.amazon.com/api-gateway/), [Amazon DynamoDB](https://aws.amazon.com/dynamodb/) and [Amazon S3](https://aws.amazon.com/s3/) there are no servers for your to maintain or manage.

## Features

✅ Api First and Cloud-Native Architecture
✅ Easy Integration with existing Applications
✅ Built using serverless service (no servers to maintain or manage)
✅ Supports Unlimited Document Tagging & Versioning
✅ Document processing through subscribing to Document Events
✅ Supports both Multi-Tenant or Multi-Instance

## Table of Contents

<details open>
<summary>Table of Contents</summary>

- [Table of Contents](#table-of-contents)
- [Getting Started](#getting-started)
  - [Installation](#basic-usage)
  - [API Reference](#api-reference)
  - [Console](#console)
  - [Document Events](#document-events)
  - [License](#license)

</details>

## Getting Started

FormKiQ has been architected using [Amazon Web Services(AWS)](https://aws.amazon.com) serverless technologies. This provides several benefits:

✅ Only pay AWS for usage (all services come with a generous monthly free tier)
✅ Easy scales to thousands of concurrent requests
✅ No servers to maintain or manage

FormKiQ uses the following AWS technologies:

- Amazon S3 - for storage of files / documents
- AWS Lambda - for document processing
- Amazon DynamoDB - storing of document metadata
- API Gateway - to serve the RESTful API platform
- Amazon Simple Notification Service(SNS) - document status notify system, allows applications to be notified that a document has been create/deleted or updated
- AWS IAM / Amazon Cognito - User / System authentication

### Installation

### API Reference

### Console

### Document Events

### License

Apache 2 - 2020 (c) FormKiq Inc. More details see LICENSE file.
