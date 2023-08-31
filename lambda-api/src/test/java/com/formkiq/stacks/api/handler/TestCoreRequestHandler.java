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
package com.formkiq.stacks.api.handler;

import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.ssm.SmsAwsServiceRegistry;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPluginEmpty;
import com.formkiq.stacks.api.AbstractCoreRequestHandler;
import com.formkiq.testutils.aws.TestServices;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Test {@link AbstractCoreRequestHandler}.
 */
public class TestCoreRequestHandler extends AbstractCoreRequestHandler {

  /** {@link AwsServiceCache}. */
  private AwsServiceCache serviceCache;

  /**
   * constructor.
   * 
   * @param environment {@link Map}
   */
  public TestCoreRequestHandler(final Map<String, String> environment) {
    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    this.serviceCache =
        new AwsServiceCacheBuilder(environment, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
                new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(),
                new SmsAwsServiceRegistry())
            .build();

    initialize(this.serviceCache, new DocumentTagSchemaPluginEmpty());
  }

  @Override
  public AwsServiceCache getAwsServices() {
    return this.serviceCache;
  }
}
