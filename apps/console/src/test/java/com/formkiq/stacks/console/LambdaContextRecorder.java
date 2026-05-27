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
package com.formkiq.stacks.console;

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
  public ClientContext getClientContext() {
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
  public CognitoIdentity getIdentity() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public String getInvokedFunctionArn() {
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
  public LambdaLogger getLogger() {
    return this.loggerRecorder;
  }

  /**
   * Get Logger Recorder.
   * 
   * @return {@link LambdaLoggerRecorder}
   */
  public LambdaLoggerRecorder getLoggerRecorder() {
    return this.loggerRecorder;
  }

  @Override
  public int getMemoryLimitInMB() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }

  @Override
  public int getRemainingTimeInMillis() {
    throw new UnsupportedOperationException("Method is not implemented.");
  }
}
