# FormKiQ Core

## Get Started

FormKiQ Core was built using the [AWS Serverless Application Model (SAM)](https://aws.amazon.com/serverless/sam/) framework.

This project contains source code and supporting files for the [FormKiQ Core](https://github.com/formkiq/formkiq-core) serverless application that you can deploy with the SAM CLI.

[FormKiQ Core](https://github.com/formkiq/formkiq-core) uses AWS resources, including DynamoDB, CloudFront, Lambda functions and an API Gateway API. These resources are defined in the `template.yaml` file in this project. You can update the template to add AWS resources through the same deployment process that updates your application code.

## Deploy FormKiQ Core

The Serverless Application Model Command Line Interface (SAM CLI) is an extension of the AWS CLI that adds functionality for deploying serverless applications.

To use the AWS/SAM CLI, you need the following tools.

* AWS CLI - [Install the AWS CLI](https://aws.amazon.com/cli/)
* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)

To deploy FormKiQ Core for the first time, run the following in your shell:

```bash
sam deploy --guided --capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM
```

The command will package and deploy your application to AWS, with a series of prompts:

* **Stack Name**: The name of the stack to deploy to CloudFormation. This should be unique to your account and region, and a good starting point would be `formkiq-core-&lt;AppEnvironment&gt;` where AppEnvironment matches your installation environment, e.g. prod, dev, test.
* **AWS Region**: The AWS region you want to deploy your app to.
* **AdminEmail**: The Administration Email you want FormKiQ to use.
* **AppEnvironment**: Your installation environment, e.g. prod, dev, test. Must be unique per account.
* **EnablePublicUrls**: Whether to Enable `/public/` urls.
* **PasswordMinimumLength**: Minimum Password Length for User Accounts.
* **PasswordRequireLowercase**: Whether Lowercase letter is required in User Password.
* **PasswordRequireNumbers**: Whether Number is required in User Password.
* **PasswordRequireSymbols**: Whether Symbol is required in User Password.
* **PasswordRequireUppercase**: Whether Uppercase letter is required in User Password.
* **Confirm changes before deploy**: If set to yes, any change sets will be shown to you before execution for manual review. If set to no, the AWS SAM CLI will automatically deploy application changes.
* **Allow SAM CLI IAM role creation**: FormKiQ Core's AWS SAM templates create AWS IAM roles required for the AWS Lambda function(s) included to access AWS services. The permissions are passed in by the `sam deploy` command above. Set Value to 'Y'
* **Save arguments to samconfig.toml**: If set to yes, your choices will be saved to a configuration file inside the project, so that in the future you can just re-run `sam deploy` without parameters to deploy changes to your application.

You can find your API Gateway Endpoint URL in the output values displayed after deployment.


## Cleanup

To delete the FormKiQ-core application that you created, use the AWS CLI. Assuming you used your project name for the stack name, you can run the following:

```bash
aws cloudformation delete-stack --stack-name formkiq-core-&lt;AppEnvironment&gt;
```