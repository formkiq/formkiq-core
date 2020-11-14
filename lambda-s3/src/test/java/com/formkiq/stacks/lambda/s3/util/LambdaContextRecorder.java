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
package com.formkiq.stacks.lambda.s3.util;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * 
 * {@link Context} to record Lambda information.
 *
 */
public class LambdaContextRecorder implements Context {

  /** {@link LambdaLoggerRecorder}. */
  private final LambdaLoggerRecorder loggerRecorder = new LambdaLoggerRecorder();

  @Override
  public String getAwsRequestId() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getLogGroupName() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getLogStreamName() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getFunctionName() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getFunctionVersion() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getInvokedFunctionArn() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public CognitoIdentity getIdentity() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public ClientContext getClientContext() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public int getRemainingTimeInMillis() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public int getMemoryLimitInMB() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public LambdaLogger getLogger() {
    return this.loggerRecorder;
  }

}
