![FormKiQ](https://raw.githubusercontent.com/formkiq/formkiq-core/master/logo.png)

# FormKiQ Core
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Table of Contents

<details open>
<summary>Table of Contents</summary>

- [What is FormKiQ Core](#-what-is-formkiq-core)
  - [Features](#features)
  - [Demo](#demo)
  - [How much does it cost to run?](#how-much-does-it-cost-to-run)
  - [Examples](#examples)
    - [Web Form Example - Contact Form](#web-form-example---contact-form)
    - [Web Form Example - Job Application Form](#web-form-example---job-application-form)
- [Architecture](#architecture)
- [Documentation](#documentation)
- [Building from source](#building-from-source)
- [License](#license)

</details>

## 💥 What is FormKiQ Core?

**FormKiQ Core is an Open Source Headless Document Management System (DMS) that runs completely in *YOUR* [Amazon Web Services (AWS) Cloud](https://aws.amazon.com).**

You can use **FormKiQ Core** to power:
* Easily store Documents / Form data from your website
* Quickly Tag and Organize your Documents
* Flexible integrate into existing application or build custom workflows

FormKiQ Core is built for any size organization, from personal websites to large, enterprise organizations requiring full control of any number of internal and external documents. 

FormKiQ Core is built using AWS Serverless services like [AWS Lambda](https://aws.amazon.com/lambda/), [Amazon API Gateway](https://aws.amazon.com/api-gateway/), [Amazon DynamoDB](https://aws.amazon.com/dynamodb/) and [Amazon S3](https://aws.amazon.com/s3/); this means that there are no servers for you to maintain or manage, and all of your data stays within your AWS cloud.

### Features

Please visit [our website](https://formkiq.com) to see the full list of features.

✅ API First (FormKiQ API) and Cloud-Native Architecture

✅ Easy Integration with Existing Applications

✅ Built Using Serverless Services (no servers to maintain or manage)

✅ Supports Unlimited Document Tagging & Versioning

✅ Document Processing Through Subscribing to Document Events

✅ Supports Both Multi-Tenant and Multi-Instance

✅ Includes an Intuitive User Interface (FormKiQ Console) for Document Management

### Documentation

Full FormKiQ Documentation can be found at

https://docs.formkiq.com/

or

https://github.com/formkiq/docs.formkiq.com

### Demo

Click the link below to see how you can run the FormKiQ Demo is **YOUR** AWS Account.

[FormKiQ Core Demo](https://github.com/formkiq/formkiq-core/wiki/FormKiQ-Core-Demo)

### How much does it cost to run

FormKiQ Core was created using [serverless technology](https://aws.amazon.com/serverless/). This means that there are no servers to manage; everything is managed by AWS. All AWS services FormKiQ uses pay-per-usage billing. You can start using FormKiQ with very little cost.
AWS provides a [free tier](https://aws.amazon.com/free) to all AWS accounts. This means that some AWS services you can use for **free** pending you stay under the usage limits. Below is the list of services FormKiQ uses and their approximate usage costs, so give you an idea on how much it costs to run FormKiQ. (All costs in USD)
| Service        | Cost  |
| ------------- | -----|
| [Amazon Api Gateway](https://aws.amazon.com/api-gateway/pricing/) |   $1.00 per million requests
| [Amazon DynamoDB](https://aws.amazon.com/dynamodb/pricing/on-demand/)  |  First 25 GB Free |
| |Write request units - $1.25 per million write request units |
| |Read request units   $0.25 per million read request units |
| [Amazon CloudFront](https://aws.amazon.com/cloudfront/pricing/) |   $0.085 per GB of Data Transfer Out to Internet||
| [Amazon S3](https://aws.amazon.com/s3/pricing/) |   $0.023 per GB / Month|
| [AWS Lambda](https://aws.amazon.com/lambda/pricing/) |   approx. first 400,000 requests Free per Month|
| |$0.0000168667 per additional request

### Examples

FormKiQ core can be used immediately after being deployed to handle web form submissions on your website. (In fact, you don't even need to be hosting your site in AWS to use FormKiQ for processing your site visitor's form submissions.)

**The easiest way to include FormKiQ on your website is through the [FormKiQ Client SDK npm module](https://www.npmjs.com/package/formkiq-client-sdk-javascript).**

You can see FormKiQ Core and the FormKiQ Client SDK in action in the examples below:

#### Web Form Example - Contact Form
**https://github.com/formkiq/formkiq-webform-examples-contact**

![Screenshot of Contact Form Example](https://raw.githubusercontent.com/formkiq/formkiq-webform-examples-contact/master/screenshot.png)

#### Web Form Example - Job Application Form
**https://github.com/formkiq/formkiq-webform-examples-jobapplication**

![Screenshot of Job Application Form Example](https://raw.githubusercontent.com/formkiq/formkiq-webform-examples-jobapplication/master/screenshot.png)

## Architecture

![Architecture Diagram](https://raw.githubusercontent.com/formkiq/formkiq-core/master/architecture.svg)

FormKiQ Core has been architected using [Amazon Web Services (AWS)](https://aws.amazon.com) Serverless technologies. This provides several benefits:

✅ Only pay AWS for usage (all services come with a generous monthly free tier)

✅ Easily scales to thousands of concurrent requests

✅ No servers to maintain or manage

### List of AWS Services

**FormKiQ core uses the following AWS technologies:**

- Amazon S3 - for storage of files / documents
- Amazon CloudFront - for hosting the FormKiQ Console
- AWS Lambda - for document processing
- Amazon DynamoDB - storing of document metadata
- API Gateway - to serve the RESTful API platform
- Amazon Simple Notification Service (SNS) - document status notify system, allows applications to be notified that a document has been create/deleted or updated
- AWS IAM and Amazon Cognito - User and System authentication

## Building from source

Please see our [docs](https://docs.formkiq.com/#_building_from_source) for instructions.

## License

MIT - 2020 (c) FormKiq Inc. More details see LICENSE file.
