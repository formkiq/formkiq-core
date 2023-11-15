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
package com.formkiq.aws.s3;

import java.net.URI;
import java.util.Map;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceRegistry;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * S3 {@link AwsServiceRegistry}.
 */
public class S3AwsServiceRegistry implements AwsServiceRegistry {

  @Override
  public void initService(final AwsServiceCache serviceCache,
      final Map<String, URI> awsServiceEndpoints,
      final AwsCredentialsProvider credentialsProvider) {

    S3ConnectionBuilder s3 =
        new S3ConnectionBuilder(serviceCache.enableXray()).setRegion(serviceCache.region())
            .setCredentials(credentialsProvider).setEndpointOverride(awsServiceEndpoints.get("s3"));

    serviceCache.register(S3ConnectionBuilder.class,
        new ClassServiceExtension<S3ConnectionBuilder>(s3));

    S3PresignerConnectionBuilder s3Presigner = new S3PresignerConnectionBuilder()
        .setRegion(serviceCache.region()).setCredentials(credentialsProvider)
        .setEndpointOverride(awsServiceEndpoints.get("s3presigner"));

    if ("true".equals(serviceCache.environment("PATH_STYLE_ACCESS_ENABLED"))) {
      s3Presigner = s3Presigner.pathStyleAccessEnabled(Boolean.TRUE);
    }

    serviceCache.register(S3PresignerConnectionBuilder.class,
        new ClassServiceExtension<S3PresignerConnectionBuilder>(s3Presigner));
  }
}
