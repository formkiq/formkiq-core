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
package com.formkiq.aws.ssm;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

/**
 * 
 * SSM Services Implementation.
 *
 */
public class SsmServiceImpl implements SsmService {

  /** {@link SsmClient}. */
  private SsmClient ssm = null;

  /**
   * constructor.
   * 
   * @param builder {@link SsmConnectionBuilder}
   */
  public SsmServiceImpl(final SsmConnectionBuilder builder) {
    this.ssm = builder.build();
  }

  @Override
  public String getParameterValue(final String parameterKey) {

    String value;
    GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameterKey).build();

    try {
      GetParameterResponse response = this.ssm.getParameter(parameterRequest);
      value = response.parameter().value();

    } catch (ParameterNotFoundException e) {
      value = null;
    }

    return value;
  }

  @Override
  public void putParameter(final String name, final String value) {
    PutParameterRequest put = PutParameterRequest.builder().name(name).value(value)
        .type(ParameterType.STRING).overwrite(Boolean.TRUE).build();
    this.ssm.putParameter(put);
  }

  @Override
  public void removeParameter(final String name) {
    DeleteParameterRequest req = DeleteParameterRequest.builder().name(name).build();
    this.ssm.deleteParameter(req);
  }
}
