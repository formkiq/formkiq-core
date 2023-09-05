/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.testutils.aws;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * 
 * JUnit 5 Extension for {@link LocalStackContainer}.
 *
 */
public class LocalStackExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public static AwsServiceCache getAwsServiceCache() {
    return serviceCache;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    TestServices.startLocalStack();

    Map<String, String> env = Map.of("AWS_REGION", "us-east-1", "DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    serviceCache =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(),
                new SmsAwsServiceRegistry())
            .build();

    serviceCache.register(S3Service.class, new S3ServiceExtension());
    serviceCache.register(SsmService.class, new SsmServiceExtension());

    S3Service s3service = serviceCache.getExtension(S3Service.class);
    if (!s3service.exists(BUCKET_NAME)) {
      s3service.createBucket(BUCKET_NAME);
    }

    if (!s3service.exists(STAGE_BUCKET_NAME)) {
      s3service.createBucket(STAGE_BUCKET_NAME);
    }

    if (!s3service.exists(OCR_BUCKET_NAME)) {
      s3service.createBucket(OCR_BUCKET_NAME);
    }

    SsmService ssm = serviceCache.getExtension(SsmService.class);
    ssm.putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/version", "1.1");
  }

  @Override
  public void close() throws Throwable {
    TestServices.stopLocalStack();
  }
}
