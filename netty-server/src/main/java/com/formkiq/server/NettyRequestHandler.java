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
package com.formkiq.server;

import java.net.URI;
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
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Netty {@link AbstractCoreRequestHandler}.
 */
public class NettyRequestHandler extends AbstractCoreRequestHandler {

  /** {@link AwsServiceCache}. */
  private AwsServiceCache serviceCache;

  /**
   * constructor.
   * 
   * @param env {@link Map}
   * @param awsServiceEndpoints {@link Map}
   * @param credentialsProvider {@link AwsCredentialsProvider}
   */
  public NettyRequestHandler(final Map<String, String> env,
      final Map<String, URI> awsServiceEndpoints,
      final AwsCredentialsProvider credentialsProvider) {
    this.serviceCache = new AwsServiceCacheBuilder(env, awsServiceEndpoints, credentialsProvider)
        .addService(new DynamoDbAwsServiceRegistry(), new S3AwsServiceRegistry(),
            new SnsAwsServiceRegistry(), new SqsAwsServiceRegistry(), new SmsAwsServiceRegistry())
        .build();

    initialize(this.serviceCache, new DocumentTagSchemaPluginEmpty());
  }

  @Override
  public AwsServiceCache getAwsServices() {
    return this.serviceCache;
  }
}
