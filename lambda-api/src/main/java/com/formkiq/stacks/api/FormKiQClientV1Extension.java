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

import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceExtension;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * {@link FormKiqClient} implementation of {@link AwsServiceExtension}.
 *
 */
public class FormKiQClientV1Extension implements AwsServiceExtension<FormKiqClientV1> {

  /** {@link FormKiqClientV1}. */
  private FormKiqClientV1 formkiqClient = null;
  /** {@link Region}. */
  private Region region;
  /** {@link AwsCredentials}. */
  private AwsCredentials credentials;

  /**
   * constructor.
   * 
   * @param awsRegion {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   */
  public FormKiQClientV1Extension(final Region awsRegion, final AwsCredentials awsCredentials) {
    this.region = awsRegion;
    this.credentials = awsCredentials;
  }

  @Override
  public FormKiqClientV1 loadService(final AwsServiceCache awsServiceCache) {

    if (this.formkiqClient == null) {
      initFormKiQClient(awsServiceCache);
    }

    return this.formkiqClient;
  }

  private void initFormKiQClient(final AwsServiceCache awsServiceCache) {

    String appEnvironment = awsServiceCache.environment("APP_ENVIRONMENT");
    SsmService ssm = awsServiceCache.getExtension(SsmService.class);
    String documentsIamUrl =
        ssm.getParameterValue("/formkiq/" + appEnvironment + "/api/DocumentsIamUrl");

    FormKiqClientConnection fkqConnection =
        new FormKiqClientConnection(documentsIamUrl).region(this.region);

    if (this.credentials != null) {
      fkqConnection = fkqConnection.credentials(this.credentials);
    }

    this.formkiqClient = new FormKiqClientV1(fkqConnection);
  }
}
