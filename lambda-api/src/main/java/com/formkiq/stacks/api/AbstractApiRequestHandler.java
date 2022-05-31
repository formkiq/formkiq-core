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

import java.util.Map;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.services.lambda.AbstractRestApiRequestHandler;
import com.formkiq.aws.services.lambda.AwsServiceCache;
import com.formkiq.aws.services.lambda.events.DocumentTagSchemaEvents;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;

/**
 * 
 * {@link RequestStreamHandler} for Api Gateway Requests.
 *
 */
public abstract class AbstractApiRequestHandler extends AbstractRestApiRequestHandler {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sqs {@link SqsConnectionBuilder}
   * @param schemaEvents {@link DocumentTagSchemaEvents}
   */
  protected static void setAwsServiceCache(final Map<String, String> map,
      final DynamoDbConnectionBuilder builder, final S3ConnectionBuilder s3,
      final SsmConnectionBuilder ssm, final SqsConnectionBuilder sqs,
      final DocumentTagSchemaEvents schemaEvents) {

    awsServices = new CoreAwsServiceCache().environment(map).dbConnection(builder).s3Connection(s3)
        .sqsConnection(sqs).ssmConnection(ssm).debug("true".equals(map.get("DEBUG")))
        .documentTagSchemaEvents(schemaEvents);

    awsServices.init();
  }

  /** construtor. */
  public AbstractApiRequestHandler() {}

  @Override
  public AwsServiceCache getAwsServices() {
    return awsServices;
  }
}
