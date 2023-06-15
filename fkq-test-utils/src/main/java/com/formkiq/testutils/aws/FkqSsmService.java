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

import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * 
 * {@link SsmService} configured specifically for FormKiQ.
 *
 */
public class FkqSsmService implements SsmService {

  /** {@link SsmService}. */
  private SsmService service;
  /** {@link SsmConnectionBuilder}. */
  private SsmConnectionBuilder ssmBuilder;

  /**
   * constructor.
   * 
   * @param awsProfile {@link String}
   * @param awsRegion {@link Region}
   */
  public FkqSsmService(final String awsProfile, final Region awsRegion) {
    this.ssmBuilder =
        new SsmConnectionBuilder(false).setCredentials(awsProfile).setRegion(awsRegion);
    this.service = new SsmServiceImpl(this.ssmBuilder);
  }

  /**
   * Build {@link SsmClient}.
   * 
   * @return {@link SsmClient}
   */
  public SsmClient build() {
    return this.ssmBuilder.build();
  }

  @Override
  public String getParameterValue(final String key) {
    return this.service.getParameterValue(key);
  }

  @Override
  public void putParameter(final String key, final String value) {
    this.service.putParameter(key, value);
  }

  @Override
  public void removeParameter(final String key) {
    this.service.removeParameter(key);
  }
}
