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
  - [Users](#users)
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

** DEMO ** link?

### Installation

FormKiQ can be installed from [AWS Serverless Application Repository](https://aws.amazon.com/serverless/serverlessrepo). The [AWS Serverless Application Repository](https://aws.amazon.com/serverless/serverlessrepo) is a AWS managed repository for serverless applications that allow for the easy distribution of applications. 

[Install FormKiQ Core](https://us-east-1.console.aws.amazon.com/lambda/home?region=us-east-1#/create/app?applicationId=arn:aws:serverlessrepo:us-east-1:622653865277:applications/FormKiQ-Core)

After installation is completed, you'll receive an email

### API Reference

Below is a summary of the Document API. The API was built using the [OpenAPI Specification](https://swagger.io/specification/).

[Full FormKiQ Core OpenAPI Spec](https://raw.githubusercontent.com/formkiq/formkiq-core/master/lambda-api/src/main/resources/cloudformation/api.yml)

|Method|Url|Description|
|--------------|--------------------------|--------------------------|
| GET | /documents | Returns a list of documents in reverse inserted date order. |
| POST | /documents | Save document. |
| GET | /documents/{documentId} | Get document details. |
| PATCH | /documents/{documentId} | Update document details. |
| DELETE | /documents/{documentId} | Delete document. |
| GET | /documents/{documentId}/versions | Get versions of document. |
| GET | /documents/{documentId}/content | Get document content. |
| GET | /documents/{documentId}/tags | Get document tags. |
| POST | /documents/{documentId}/tags | Add Tag to document. |
| GET | /documents/{documentId}/tags/{tagKey} | Get specific TAG. |
| PUT | /documents/{documentId}/tags/{tagKey} | Update TAG. |
| DELETE | /documents/{documentId}/tags/{tagKey} | Delete TAG. |
| GET | /documents/{documentId}/url | Get Document URL. |
| GET | /documents/upload | Returns URL that can accept uploads largers than 5 MB. |
| GET | /documents/{documentId}/upload | Returns URL that can accept uploads largers than 5 MB for a specific document. |
| POST | /search | Document Search. |
| POST | /public/documents | Unauthenticated URL for saving a document. |

### Console

A console comes with FormKiQ Core. The console supports adding/removing Documents, adding/removing Tags from Documents and searching for Documents.

The console code is also open source and can be found on [Github](https://github.com/formkiq/formkiq-console).

### Document Events

### Users

### License

Apache 2 - 2020 (c) FormKiq Inc. More details see LICENSE file.
