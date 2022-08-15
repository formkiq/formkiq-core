![FormKiQ](https://raw.githubusercontent.com/formkiq/formkiq-core/master/fq-gh-social.png)

# FormKiQ Core &nbsp; [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Open%20Source%20Headless%20Document%20Management%20System%20that%20runs%20in%20your%20AWS%20Cloud&url=https://www.formkiq.com&via=FormKiQ&hashtags=documentmanagement,dms,headless,serverless)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[**Click Here for One-click Installation**](#installation)

## Table of Contents

<details open>
<summary>Table of Contents</summary>

- [What is FormKiQ Core](#what-is-formkiq-core)
- [Installation](#installation)
- [Architecture](#architecture)
- [Documentation](#documentation)
- [Client Libraries](#client-libraries)
- [Use Cases](#use-cases)
- [Application Examples](#examples)
- [Building from source](#building-from-source)
- [License](#license)

</details>

## What is FormKiQ Core?

**FormKiQ Core is an Open Source Headless Document Management System (DMS) that runs completely in *YOUR* [Amazon Web Services (AWS) Cloud](https://aws.amazon.com).**

You can use **FormKiQ Core** to power:
* Easily store Documents / Form data from your website
* Quickly Tag and Organize your Documents
* Flexible integrate into existing application or build custom workflows

FormKiQ Core is built for any size organization, from personal websites to large, enterprise organizations requiring full control of any number of internal and external documents. 

FormKiQ Core is built using AWS Serverless services like [AWS Lambda](https://aws.amazon.com/lambda/), [Amazon API Gateway](https://aws.amazon.com/api-gateway/), [Amazon DynamoDB](https://aws.amazon.com/dynamodb/) and [Amazon S3](https://aws.amazon.com/s3/); this means that there are no servers for you to maintain or manage, and all of your data stays within your AWS cloud.

### Features

Please visit [our website](https://www.formkiq.com) to see the full list of features.

✅ API First (FormKiQ API) and Cloud-Native Architecture

✅ Easy Integration with Existing Applications

✅ Built Using Serverless Services (no servers to maintain or manage)

✅ Supports Unlimited Document Tagging & Versioning

✅ Document Processing Through Subscribing to Document Events

✅ Supports Both Multi-Tenant and Multi-Instance

✅ Includes an Intuitive User Interface (FormKiQ Console) for Document Management

## Installation

The following are AWS CloudFormation scripts that can be used to install FormKiQ and other resources in a single click!

[<img src="https://github.com/formkiq/formkiq-core/blob/3b64a239b76426550408b40b43fd815b69292441/install-play.png">](https://www.youtube.com/watch?v=jVIK2ZJZsKE "Install FormKiQ Core into any AWS Account - Click to Watch!")

[Full Installation Instructions](https://docs.formkiq.com/docs/1.8.0/reference/README.html#installation)

| AWS Region   | Install Link  |
| ------------- | -------------|
| us-east-1 | [Install FormKiQ Core in US-EAST-1 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-us-east-1.s3.amazonaws.com/1.8.2/template.yaml)
| us-east-2 | [Install FormKiQ Core in US-EAST-2 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-us-east-2.s3.amazonaws.com/1.8.2/template.yaml)
| ca-central-1| [Install FormKiQ Core in CA-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=ca-central-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-ca-central-1.s3.amazonaws.com/1.8.2/template.yaml)
| eu-central-1| [Install FormKiQ Core in EU-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-eu-central-1.s3.amazonaws.com/1.8.2/template.yaml)

## Architecture

![Architecture Diagram](https://raw.githubusercontent.com/formkiq/formkiq-core/master/architecture.svg)

FormKiQ Core has been architected using [Amazon Web Services (AWS)](https://aws.amazon.com) Serverless technologies. This provides several benefits:

✅ Only pay AWS for usage (all services come with a generous monthly free tier)

✅ Easily scales to thousands of concurrent requests

✅ No servers to maintain or manage

###Documentation

Full FormKiQ Documentation can be found at

https://docs.formkiq.com/

### List of AWS Services

**FormKiQ core uses the following AWS technologies:**

- Amazon S3 - for storage of files / documents
- Amazon CloudFront - for hosting the FormKiQ Console
- AWS Lambda - for document processing
- Amazon DynamoDB - storing of document metadata
- API Gateway - to serve the RESTful API platform
- Amazon Simple Notification Service (SNS) - document status notify system, allows applications to be notified that a document has been create/deleted or updated
- AWS IAM and Amazon Cognito - User and System authentication

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

## Client Libraries

## Client libraries

| Language | Client |
|----------|--------|
| Java     | [formkiq-client-sdk-java](https://github.com/formkiq/formkiq-client-sdk-java) | 


## Use Cases

- [Job Application Form](https://www.formkiq.com/use-cases/job-application-form)
- [Document Management Module for an Existing App](https://www.formkiq.com/use-cases/document-management-module-for-an-existing-application)
- [Digital Document Processing Service](https://www.formkiq.com/use-cases/digital-document-processing-service)
- [Product Leasing System](https://www.formkiq.com/use-cases/product-leasing-system)

## Examples

FormKiQ core can be used immediately after being deployed to handle web form submissions on your website. (In fact, you don't even need to be hosting your site in AWS to use FormKiQ for processing your site visitor's form submissions.)

**The easiest way to include FormKiQ on your website is through the [FormKiQ Client SDK npm module](https://www.npmjs.com/package/formkiq-client-sdk-javascript).**

You can see FormKiQ Core and the FormKiQ Client SDK in action in the examples below:

#### Web Form Example - Contact Form
**https://github.com/formkiq/formkiq-webform-examples-contact**

![Screenshot of Contact Form Example](https://raw.githubusercontent.com/formkiq/formkiq-webform-examples-contact/master/screenshot.png)

#### Web Form Example - Job Application Form
**https://github.com/formkiq/formkiq-webform-examples-jobapplication**

![Screenshot of Job Application Form Example](https://raw.githubusercontent.com/formkiq/formkiq-webform-examples-jobapplication/master/screenshot.png)

## Building from source

Please see our [docs](https://docs.formkiq.com/docs/1.7.0/reference/README.html#building-from-source) for instructions.

## License

MIT - 2020-2022 (c) FormKiQ, Inc. For more details, see LICENSE file.
