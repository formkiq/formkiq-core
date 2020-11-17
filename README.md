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
  - [Document Events](#document-events)
    - [Create](#create-event)
    - [Update](#update-event)
    - [Delete](#delete-event)  
  - [Console](#console)
    - [Users](#users)
    - [Groups](#groups)
  - [SiteId](#siteid)
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

### Document Events

### Console

While FormKiQ Core was built using an API first design methodology, which allows easy integration with existing applications. Users can have the option to instead use the console that comes with FormKiQ Core.

The console supports:

✅ Adding / Removing Documents
✅ Adding / Removing Document Tags
✅ Search Documents
✅ Testing API - you can test using APIs directly from the console

The console's code is open source and can be found on [Github](https://github.com/formkiq/formkiq-console).

#### Users

FormKiQ Core uses [Amazon Cognito](https://aws.amazon.com/cognito/) for authentication and authorization. All users maintenance operations can be done via the Amazon Cognito console.

Each FormKiQ Core installation creates it's own user pool. By default FormKiq Core uses the "AdminEmail" to create an Administrator user. FormKiq Core sends a confirmation link to the "AdminEmail" during installation.

To learn how to add additional users see [Amazon Cognito's Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/managing-users.html) for instructions.

#### Groups

FormKiQ Core uses [Amazon Cognito](https://aws.amazon.com/cognito/) for authentication and authorization. All group maintenance operations can be done via the Amazon Cognito console.

During installation, FormKiQ Core creates 3 groups:

- Admins
- default
- default_read

Users in the "Admins" group have full access to all documents in FormKiQ Core.

Users in the "default" group will have read/write access to documents in the default siteid.

Users in the "default_read" group will have read only access to documents in the default siteid.

To learn 

To learn how to add users to a cognito group see [Amazon Cognito's Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-user-groups.html) for instructions.

### SiteId

FormKiQ Core supports running as a multi-tenant application. After installation a "default" SiteId is created and all documents are stored in that tenant.

To create another SiteId is as simple as adding a [Cognito group to the user pool](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-user-groups.html)

Creating a Cognito Group with the same name as the SiteId but ending in "_read" will create a readonly group. Where the users in this group will have readonly access to that SiteId.

Each API requests has a "SiteId" parameter you can pass to specify which SiteId you would like to use. **NOTE:** This parameter is only needed if a user belongs to multiple SiteId or if the user is in the "Admins" group and wants to perform an operation in a SiteId other than "default".

### License

Apache 2 - 2020 (c) FormKiq Inc. More details see LICENSE file.
