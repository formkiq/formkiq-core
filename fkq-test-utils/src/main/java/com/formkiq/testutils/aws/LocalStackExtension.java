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

import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 
 * JUnit 5 Extension for {@link LocalStackContainer}.
 *
 */
public class LocalStackExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  /** {@link LocalStackContainer}. */
  private LocalStackContainer localstack = null;

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    this.localstack = TestServices.getLocalStack();
    this.localstack.start();

    S3Service s3service = new S3Service(TestServices.getS3Connection());
    try (S3Client s3 = s3service.buildClient()) {

      if (!s3service.exists(s3, BUCKET_NAME)) {
        s3service.createBucket(s3, BUCKET_NAME);
      }

      if (!s3service.exists(s3, STAGE_BUCKET_NAME)) {
        s3service.createBucket(s3, STAGE_BUCKET_NAME);
      }
    }

    new SsmServiceImpl(TestServices.getSsmConnection())
        .putParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/version", "1.1");
  }

  @Override
  public void close() throws Throwable {
    if (this.localstack != null) {
      this.localstack.stop();
    }
  }
}
