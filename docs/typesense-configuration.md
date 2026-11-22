# Configure Typesense on AWS

Typesense provides optional basic full-text search for FormKiQ Core. This guide covers secret configuration, migration, and API key rotation.

## Configure the secret

Create a Secrets Manager secret in the same AWS account and Region as FormKiQ.
Choose **Other type of secret**, select **Plaintext**, and store only the Typesense
API key as the secret's `SecretString`, without a JSON object or surrounding quotes.
Provide its full ARN as the CloudFormation parameter `TypesenseApiKeySecretArn`,
along with `VpcStackName`. An empty ARN disables Typesense.

## Migrate from the plaintext API key parameter

`TypesenseApiKey` is no longer a supported CloudFormation parameter. When upgrading,
create the secret with your existing Typesense key, remove the old parameter from
deployment overrides, and supply `TypesenseApiKeySecretArn`. The stack update removes
the old `/formkiq/<environment>/typesense/ApiKey` SSM parameter. Keep the secret available
throughout deployment and rollback; FormKiQ does not create or delete this secret.

## Secret access and permissions

The ECS task receives the key through its `Secrets` configuration. API, indexing,
and document-action Lambdas receive the ARN and retrieve the key at runtime. IAM
permissions allow reads of that secret and KMS decryption for its encryption context.
For a customer managed KMS key, its key policy must also permit these execution roles.

## Rotate the API key

Updating the secret does not automatically refresh running tasks or cached Lambda
clients. Coordinate key changes with an ECS service deployment and recycling the
Typesense-consuming Lambda execution environments. The current single-task Typesense
service can have a brief search interruption during deployment. Existing task revisions
may retain the previous plaintext key, so rotate it after migration and validation.

## Local Docker development

Local Docker development continues to use its local `TYPESENSE_API_KEY` setting.

[Back to the README](../README.md#quick-start)
