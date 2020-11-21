/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
  private SsmClient ssm;

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
