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
package com.formkiq.stacks.api;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.s3.S3AwsServiceRegistry;
import com.formkiq.aws.sns.SnsAwsServiceRegistry;
import com.formkiq.aws.sqs.SqsAwsServiceRegistry;
import com.formkiq.aws.ssm.SsmAwsServiceRegistry;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.lambdaservices.timer.MethodTimer;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

import java.util.Map;

/** {@link RequestStreamHandler} for handling API Gateway 'GET' requests. */
@Reflectable
public class CoreRequestHandler extends AbstractCoreRequestHandler {

  /** {@link AwsServiceCache}. */
  private static final AwsServiceCache SERVICE_CACHE;

  static {

    AwsServiceCacheBuilder builder = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
        EnvironmentVariableCredentialsProvider.create());
    Logger logger = builder.build().getLogger();

    MethodTimer.timer(logger, "Dynamodb", () -> builder.addService(new DynamoDbAwsServiceRegistry()));

    MethodTimer.timer(logger, "S3", () -> builder.addService(new S3AwsServiceRegistry()));

    MethodTimer.timer(logger, "SNS", () -> builder.addService(new SnsAwsServiceRegistry()));

    MethodTimer.timer(logger, "SQS", () -> builder.addService(new SqsAwsServiceRegistry()));

    MethodTimer.timer(logger, "SSM", () -> builder.addService(new SsmAwsServiceRegistry()));

    SERVICE_CACHE = builder.build();

    MethodTimer.timer(logger, "Initialize", () -> initialize(SERVICE_CACHE));
  }

  @Override
  public AwsServiceCache getAwsServices() {
    return SERVICE_CACHE;
  }
}
