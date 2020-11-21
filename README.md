![FormKiQ](https://raw.githubusercontent.com/formkiq/formkiq-core/master/logo.png)

# FormKiQ Core

**FormKiQ Core is an Open Source Headless Document Management System (DMS) that runs in your [Amazon Web Services (AWS) Cloud](https://aws.amazon.com).**

FormKiQ Core is built for any size organization, from personal websites to large, enterprise organizations requiring full control of any number of internal and external documents. 

You can use FormKiQ Core to power the forms on your website, handling text and file attachments of any size. You can also use FormKiQ Core for file uploads and document tagging.

FormKiQ Core is built using AWS Serverless services like [AWS Lambda](https://aws.amazon.com/lambda/), [Amazon API Gateway](https://aws.amazon.com/api-gateway/), [Amazon DynamoDB](https://aws.amazon.com/dynamodb/) and [Amazon S3](https://aws.amazon.com/s3/); this means that there are no servers for you to maintain or manage, and all of your data stays within your AWS cloud.

## Features

✅ API First (FormKiQ API) and Cloud-Native Architecture

✅ Easy Integration with Existing Applications

✅ Built Using Serverless Services (no servers to maintain or manage)

✅ Supports Unlimited Document Tagging & Versioning

✅ Document Processing Through Subscribing to Document Events

✅ Supports Both Multi-Tenant and Multi-Instance

✅ Includes an Intuitive User Interface (FormKiQ Console) for Document Management

## Table of Contents

<details open>
<summary>Table of Contents</summary>

- [Table of Contents](#table-of-contents)
- [Demos](#demos)
- [Getting Started](#getting-started)
  - [Installation](#installation)
    - [Outputs](#outputs)
  - [API Reference](#api-reference)
  - [Document Events](#document-events) 
  - [Console](#console)
  - [Authentication](#authentication)
    - [Users](#users)
    - [Groups](#groups)
    - [IAM](#iam)
  - [SiteId](#siteid)
  - [License](#license)

</details>

## Demos

**Coming Soon**

- **FormKiQ Core Website Contact Form Example:** Coming Soon

- **FormKiQ Core Website Job Application Form Example:** Coming Soon

- **FormKiQ Console with API Explorer:** Coming Soon


## Getting Started

FormKiQ Core has been architected using [Amazon Web Services (AWS)](https://aws.amazon.com) Serverless technologies. This provides several benefits:

✅ Only pay AWS for usage (all services come with a generous monthly free tier)

✅ Easily scales to thousands of concurrent requests

✅ No servers to maintain or manage

**FormKiQ Core uses the following AWS technologies:**

- Amazon S3 - for storage of files / documents
- AWS Lambda - for document processing
- Amazon DynamoDB - storing of document metadata
- API Gateway - to serve the RESTful API platform
- Amazon Simple Notification Service (SNS) - document status notify system, allows applications to be notified that a document has been create/deleted or updated
- AWS IAM and Amazon Cognito - User and System authentication

### Installation

FormKiQ Core can be installed from the [AWS Serverless Application Repository](https://aws.amazon.com/serverless/serverlessrepo). The Serverless Application Repository is an AWS-managed repository for serverless applications that allow for the easy distribution of applications from AWS Verified Authors. 

[Install FormKiQ Core](https://us-east-1.console.aws.amazon.com/lambda/home?region=us-east-1#/create/app?applicationId=arn:aws:serverlessrepo:us-east-1:622653865277:applications/FormKiQ-Core)

After installation is completed, you will receive an email with a link to the FormKiQ Console that has been installed in your AWS Cloud as part of the setup. You will be asked to login to your account, and once you do, you will be prompted to change your password.

Once you have set your password, you are ready to use FormKiQ Core.

#### Outputs

After the FormKiQ Cloudformation Stack completes output values from the installation are made available in the CloudFormation Outputs and in the SSM Parameter Store. Below you'll find a description of the outputs.

**CloudFormation Outputs**

|Key|Description|
|--------------|--------------------------|
| ConsoleUrl | The URL for the FormKiQ Console |
| HttpApiUrl | The URL for the API endpoint that uses Cognito authorization |
| IamApiUrl | The URL for the API endpoint that uses IAM authorization |

**SSM Parameter Store**

SSM parameters made it easy for applications to automatically lookup FormKiQ configuration. All configuration keys start with '/formkiq/{AppEnvironment}'

|Parameter|Description|
|--------------|--------------------------|
| api/DocumentsHttpUrl | The URL for the API endpoint that uses Cognito authorization |
| api/DocumentsIamUrl | The URL for the API endpoint that uses IAM authorization |
| cognito/AdminGroup | Cognito Admin Group |
| cognito/IdentityPoolId | Cognito Identity Pool |
| cognito/UserPoolArn | Cognito User Pool Arn |
| cognito/UserPoolClientId | Cognito User Pool Client |  
| cognito/UserPoolId | Cognito User Pool |  
| cognito/UserPoolProviderName | Cognito User Pool Provider Name |  
| cognito/UserPoolProviderUrl | Cognito User Pool Provider URL |  
| console/AdminEmail | Console Admin Email |  
| console/Url | The URL for the FormKiQ Console |  
| console/version | Console Version |  
| dynamodb/CacheTableName | DynamoDB Cache table name |  
| dynamodb/DocumentsTableName | DynamoDB Documents table name |  
| iam/ApiGatewayInvokeGroup | API Gateway Group that allows invoking of endpoints |  
| iam/ApiGatewayInvokeGroupArn | API Gateway Group Arn that allows invoking of endpoints |  
| iam/ApiGatewayInvokeRole | API Gateway Role that allows invoking of endpoints |  
| iam/ApiGatewayInvokeRoleArn | API Gateway Role Arn that allows invoking of endpoints |  
| lambda/ConsoleInstaller | Lambda for Console Installation |  
| lambda/DocumentsApiRequests | Lambda for processing API Requests |  
| lambda/DocumentsUpdateObject | Lambda for processing Document Update Events |  
| lambda/StagingCreateObject | Lambda for processing Staging Document Create Events |  
| region | Installation Region |  
| s3/Console | Console S3 Bucket |
| s3/ConsoleArn | Console S3 Bucket Arn |    
| s3/ConsoleDomainName | Console S3 Bucket Domain Name |    
| s3/ConsoleRegionalDomainName | Console S3 Bucket Regional Domain Name |
| s3/DocumentsS3Bucket | Documents S3 Bucket Name |
| s3/DocumentsStageS3Bucket | Documents Staging S3 Bucket Name |    
| sns/SnsDocumentsCreateEventTopicArn | SNS Topic for Document Create Events |    
| sns/SnsDocumentsDeleteEventTopicArn | SNS Topic for Document Delete Events |  
| sns/SnsDocumentsUpdateEventTopicArn | SNS Topic for Document Update Events |
| sqs/DocumentsUpdateArn | SQS ARN for processing Document Update Events |    
| sqs/DocumentsUpdateUrl | SQS URL for processing Document Update Events |
| version | FormKiQ Stacks Version |

### API Reference

FormKiQ creates 2 API on installation. One API uses Cognito authorization and the other uses IAM authorization. 

See [Outputs](#Outputs) for API URLs.

Below is a summary of the FormKiQ Core FormKiQ API. The API was built using the [OpenAPI Specification](https://swagger.io/specification/).

[Full FormKiQ Core OpenAPI Spec](https://raw.githubusercontent.com/formkiq/formkiq-core/master/lambda-api/src/main/resources/cloudformation/api.yml)

|Method|Url|Description|
|--------------|--------------------------|--------------------------|
| GET | /documents | Returns a list of documents in reverse inserted date order |
| POST | /documents | Create document |
| GET | /documents/{documentId} | Get document details |
| PATCH | /documents/{documentId} | Update document details |
| DELETE | /documents/{documentId} | Delete document |
| GET | /documents/{documentId}/versions | Get versions of document |
| GET | /documents/{documentId}/content | Get document content |
| GET | /documents/{documentId}/tags | Get document tags |
| POST | /documents/{documentId}/tags | Add tag to document |
| GET | /documents/{documentId}/tags/{tagKey} | Get a specific tag |
| PUT | /documents/{documentId}/tags/{tagKey} | Update tag |
| DELETE | /documents/{documentId}/tags/{tagKey} | Delete tag |
| GET | /documents/{documentId}/url | Get document URL |
| GET | /documents/upload | Returns URL that can accept uploads largers than 5 MB |
| GET | /documents/{documentId}/upload | Returns URL that can accept uploads largers than 5 MB to update a specific document |
| POST | /search | Document search |
| POST | /public/documents | Public (unauthenticated) URL for creating a document, used for web forms |

### Document Events

Document events are a powerful feature of FormKiQ Core. This feature allows operations to be triggered on documents automatically on a change event. For example, when a document is created, a document event could trigger it to:

- send an email notification
- scan for viruses
- insert data into a database
- etc. 

Document event are created through [Amazon Simple Notification Service (SNS)](https://aws.amazon.com/sns/). Amazon SNS is a messaging service that can be used for application to application communication. FormKiQ Core uses it as a publish/subscribe service, where applications can listen to the SNS service and be notified when that event has been created.

FormKiQ Core creates the following SNS topics:

- SnsDocumentsCreateEventTopic
- SnsDocumentsDeleteEventTopic
- SnsDocumentsUpdateEventTopic

These SNS topics allow your application to be notified when a document is create, deleted, or updated, respectively. This makes it really easy to add your own customization and to automate your document processing.

See [Outputs](#Outputs) for SNS Topics

### Console

While FormKiQ Core was built using an API First design methodology that allows easy integration with existing applications, you can also use the console interface that is set up with FormKiQ Core.

The FormKiQ Console supports:

✅ Adding / Removing Documents

✅ Adding / Removing Document Tags

✅ Search Documents

✅ Executing FormKiQ API Calls within the API Explorer - you can use API methods directly from the console

The FormKiQ Console is open source and can be found on [Github](https://github.com/formkiq/formkiq-console).

See [Outputs](#Outputs) for Console Url

### Authentication

FormKiQ Core follows AWS' best practices when it comes to protect data and services. [Amazon Cognito](https://aws.amazon.com/cognito/) is the default authentication and authorization for the FormKiQ API and the FormKiQ Console. ([AWS Identity and Access Management (IAM)](#iam) is also available for accessing the API.)

#### Users

Each FormKiQ Core installation creates its own User Pool. By default, FormKiq Core uses the "AdminEmail" parameter to create a user with administrator privileges. FormKiq Core sends a confirmation link to the "AdminEmail" during installation.

All user maintenance operations can be done via the Amazon Cognito console. To learn how to add additional users see [Amazon Cognito's Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/managing-users.html) for instructions.

See [Outputs](#Outputs) for FormKiQ configuration.

#### Groups

During installation, FormKiQ Core creates three Cognito Groups within its Cognito User Pool:

- Admins
- default
- default_read

Users in the "Admins" group have full access to all documents in FormKiQ Core.

Users in the "default" group will have read/write access to documents in the default siteid.

Users in the "default_read" group will have read only access to documents in the default siteid.

All group maintenance operations can be done via the Amazon Cognito console. To learn how to add users to a cognito group see [Amazon Cognito's Developer Guide](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-user-groups.html) for instructions.

#### IAM

When integrating FormKiQ with existing application or other AWS services, AWS Identity and Access Management (IAM) is a preferred authorization mechanism. For this reason FormKiQ creates 2 APIs, one uses Cognito authorization and the other uses IAM authorization.

FormKiQ creates an IAM group that provides access to invoke FormKiq APIs in Amazon API Gateway. Users added to this group and can [signing the request](https://docs.aws.amazon.com/apigateway/api-reference/signing-requests/) which authenticates it with API Gateway.

See [Outputs](#Outputs) for FormKiQ configuration.

### SiteId

FormKiQ Core supports running as a multi-tenant application. This can be used for internal departments or teams, or for external clients. During installation, a "default" SiteId is created; all documents are stored in that tenant by default.

To create another SiteId is as simple as adding a [Cognito group to the user pool](https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-pools-user-groups.html)

Creating a Cognito Group with the same name as the SiteId but ending in "_read" will create a readonly group. The users in this group will have readonly access to that SiteId.

Each API requests has a "SiteId" parameter you can pass to specify which SiteId you would like to use.

**NOTE:** This parameter is only needed if a user belongs to multiple SiteIds or if the user is in the "Admins" Group (full access) and wants to perform an operation in a SiteId other than "default".

### License

MIT - 2020 (c) FormKiq Inc. More details see LICENSE file.
