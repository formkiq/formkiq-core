<br/>

<div align="center" style="margin: 30px;">
<a href="https://formkiq.com/">
  <img src="https://github.com/formkiq/formkiq-core/raw/master/images/logo.png" style="width:600px;" align="center" />
</a>
<br />
<br />

<div align="center">
    <a href="https://formkiq.com">Home Page</a> |
    <a href="https://docs.formkiq.com">Documentation</a> | 
    <a href="https://blog.formkiq.com">Blog</a> |
    <a href="https://github.com/formkiq/formkiq-core#Installation">Installation</a>
</div>
</div>

<br />

<div align="center"><strong>Add Document Management functionality to your  applications, without constraints.</strong><br>An open core, API-First Document Management System (DMS), developed with flexibility in mind.
<br />
<br />
</div>

## What is FormKiQ?

<div align="center" style="margin: 30px;">
<img src="https://raw.githubusercontent.com/formkiq/formkiq-core/master/images/formkiq-console.png" style="width:600px;" align="center" />
</div>
<br />
FormKiQ Core is a flexible Open Source Document Management System (DMS) that can be used as headless software or run using our web-based client interface. FormKiQ runs in your [Amazon Web Services (AWS) Cloud](https://aws.amazon.com/), and can be used for document workflows, records management, and other document storage and processing needs using an extendable Document API.
<br />
FormKiQ’s API-first design allows for anyone to quickly and easily add document management functionality to any application, cutting months off of development time. Alternatively, with FormKiQ’s customizable Front-End Web Interface, the Document Console, FormKiQ can be used as a full-featured stand-alone document management system.


### Features

✅ One-click Installation, though [AWS CloudFormation](https://aws.amazon.com/cloudformation)

✅ API-First (FormKiQ API) and Cloud-Native Architecture

✅ Easy Integration with Existing Applications

✅ Built Using Serverless Services (no servers to maintain or manage)

✅ Easily scales to thousands of concurrent requests

✅ Only pay AWS for usage (all services come with a generous monthly free tier)

✅ Supports Unlimited Document Tagging

✅ Supports Both Multi-Tenant and Multi-Instance

✅ Includes an Intuitive User Interface (FormKiQ Console) for Document Management

✅ [Professional and Enterprise Features and Support Available](https://www.formkiq.com)

## Installation

The following are AWS CloudFormation scripts that can be used to install FormKiQ and other resources in a single click!

[<img src="https://github.com/formkiq/formkiq-core/blob/3b64a239b76426550408b40b43fd815b69292441/install-play.png">](https://www.youtube.com/watch?v=jVIK2ZJZsKE "Install FormKiQ Core into any AWS Account - Click to Watch!")

### FormKiQ Core CloudFormation

*Prerequisite*

Before installation verify that the `AWSServiceRoleForECS` has been enabled on your AWS Account. The easiest fix is to open up AWS CLI and run the following against your account once.

```
aws iam create-service-linked-role --aws-service-name ecs.amazonaws.com
```

| AWS Region   | Install Link  |
| ------------- | -------------|
| us-east-1 | [Install FormKiQ Core in US-EAST-1 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-us-east-1.s3.amazonaws.com/1.9.0/template.yaml)
| us-east-2 | [Install FormKiQ Core in US-EAST-2 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-us-east-2.s3.amazonaws.com/1.9.0/template.yaml)
| ca-central-1| [Install FormKiQ Core in CA-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=ca-central-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-ca-central-1.s3.amazonaws.com/1.9.0/template.yaml)
| eu-central-1| [Install FormKiQ Core in EU-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=formkiq-core-prod&templateURL=https://formkiq-core-distribution-eu-central-1.s3.amazonaws.com/1.9.0/template.yaml)

### FormKiQ VPC CloudFormation

| AWS Region   | Install Link  |
| ------------- | -------------|
| us-east-1 | [Install FormKiQ VPC in US-EAST-1 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/new?stackName=formkiq-vpc&templateURL=https://formkiq-core-distribution-us-east-1.s3.amazonaws.com/1.9.0/vpc.yaml)
| us-east-2 | [Install FormKiQ VPC in US-EAST-2 region](https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=formkiq-vpc&templateURL=https://formkiq-core-distribution-us-east-2.s3.amazonaws.com/1.9.0/vpc.yaml)
| ca-central-1| [Install FormKiQ VPC in CA-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=ca-central-1#/stacks/new?stackName=formkiq-vpc&templateURL=https://formkiq-core-distribution-ca-central-1.s3.amazonaws.com/1.9.0/vpc.yaml)
| eu-central-1| [Install FormKiQ VPC in EU-CENTRAL-1 region](https://console.aws.amazon.com/cloudformation/home?region=eu-central-1#/stacks/new?stackName=formkiq-vpc&templateURL=https://formkiq-core-distribution-eu-central-1.s3.amazonaws.com/1.9.0/vpc.yaml)

## License

MIT - 2020-2022 (c) FormKiQ, Inc. For more details, see LICENSE file.
