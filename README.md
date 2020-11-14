# FormKiQ - Document Stack

The Document Stack gives you full control of your organization's documents,
including storage, search, data extraction, and workflows.
All in your cloud, using the latest in Amazon Web Services technologies. 

## Install FormKiQ Stacks - Document

```bash
aws cloudformation create-stack \
  --stack-name <STACK_NAME> \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/<FORMKIQ_VERSION>/documentstack.yml \
  --parameters ParameterKey=AppEnvironment,ParameterValue=<ENVIRONMENT> \
  --region <REGION>
```

Here's a full example of a production deployment:

```bash
aws cloudformation create-stack \
  --stack-name FormKiQProd \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/1.0/documentstack.yml \
  --parameters ParameterKey=AppEnvironment,ParameterValue=prod \
  --region us-east-1
```

## Install FormKiQ Stacks - Document on Custom Domain

### Install Certificate Manager's Certificate

```bash
aws cloudformation create-stack \
  --stack-name <STACK_NAME> \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/<FORMKIQ_VERSION>/certificate.yml \
  --parameters ParameterKey=DomainName,ParameterValue=<DOMAIN_NAME> \
  --region <REGION>
```

where <DOMAIN_NAME> is the name of the domain, IE: example.formkiq.com or +.formkiq.com for a wildcard certificate.

```bash
aws cloudformation create-stack \
  --stack-name <STACK_NAME> \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/<FORMKIQ_VERSION>/documentstack.yml \
  --parameters ParameterKey=AppEnvironment,ParameterValue=<ENVIRONMENT> ParameterKey=DomainName,ParameterValue=<DOMAIN_NAME> ParameterKey=AcmCertificateArn,ParameterValue=<CERTIFICATE_ARN> \  
  --region <REGION>
```

where <DOMAIN_NAME> is the name of the domain and <CERTIFICATE_ARN> is the ARN for the certificate

## Update FormKiQ Stacks - Document

```bash
aws cloudformation update-stack \
  --stack-name <STACK_NAME> \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/<FORMKIQ_VERSION>/documentstack.yml \
  --parameters ParameterKey=AppEnvironment,ParameterValue=<ENVIRONMENT> \
  --region <REGION>
```

Here's a full example of a production deployment:

```bash
aws cloudformation update-stack \
  --stack-name FormKiQProd \
  --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM \
  --template-url https://formkiq-distribution-customer.s3.amazonaws.com/formkiq-cloud/1.1/documentstack.yml \
  --parameters ParameterKey=AppEnvironment,ParameterValue=prod \
  --region us-east-1
```

## Build
The Document Stack is a Java Application built on using Gradle.

To Build:
1. Clone repository
2. ./gradlew clean build
