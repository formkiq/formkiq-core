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
package com.formkiq.stacks.dynamodb.s3;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.s3.S3ServiceInterceptor;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * 
 * {@link AwsServiceExtension} for {@link S3ServiceInterceptor}.
 *
 */
public class S3ServiceInterceptorExtension implements AwsServiceExtension<S3ServiceInterceptor> {

  /** {@link S3ServiceInterceptor}. */
  private S3ServiceInterceptor service;

  /**
   * constructor.
   */
  public S3ServiceInterceptorExtension() {}

  @Override
  public S3ServiceInterceptor loadService(final AwsServiceCache awsServiceCache) {

    if (this.service == null) {

      String versionsTable = awsServiceCache.environment("DOCUMENT_VERSIONS_TABLE");

      if (!isEmpty(versionsTable)) {
        String documentsS3Bucket = awsServiceCache.environment("DOCUMENTS_S3_BUCKET");

        DynamoDbConnectionBuilder connection =
            awsServiceCache.getExtension(DynamoDbConnectionBuilder.class);
        DynamoDbService db = new DynamoDbServiceImpl(connection, versionsTable);
        this.service = new S3ServiceVersioningInterceptor(documentsS3Bucket, db);
      } else {
        this.service = new S3ServiceNoVersioningInterceptor();
      }
    }

    return this.service;
  }
}
