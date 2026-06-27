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
package com.formkiq.aws.dynamodb;

import java.net.URI;
import java.util.Map;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceRegistry;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * {@link AwsServiceRegistry} for DynamoDb.
 */
public class DynamoDbAwsServiceRegistry implements AwsServiceRegistry {

  @Override
  public void initService(final AwsServiceCache serviceCache,
      final Map<String, URI> awsServiceEndpoints,
      final AwsCredentialsProvider credentialsProvider) {

    DynamoDbConnectionBuilder db = new DynamoDbConnectionBuilder(serviceCache.enableXray())
        .setRegion(serviceCache.region()).setCredentials(credentialsProvider)
        .setEndpointOverride(awsServiceEndpoints.get("dynamodb"));

    db.initDbClient();

    serviceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(db));
  }
}
