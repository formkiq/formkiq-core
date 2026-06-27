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
package com.formkiq.stacks.api.handler;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.mockserver.mock.action.ExpectationResponseCallback;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.testutils.aws.AbstractFormKiqApiResponseCallback;

/**
 * 
 * FormKiQ implementation of {@link ExpectationResponseCallback}.
 *
 */
public class FormKiQResponseCallback extends AbstractFormKiqApiResponseCallback {

  /** {@link TestCoreRequestHandler}. */
  private TestCoreRequestHandler handler;

  /**
   * constructor.
   */
  public FormKiQResponseCallback() {}

  @Override
  public RequestStreamHandler getHandler() {
    return this.handler;
  }

  @Override
  public Collection<String> getResourceUrls() {
    return this.handler.getUrlMap().values().stream().map(ApiGatewayRequestHandler::getRequestUrl)
        .collect(Collectors.toList());
  }

  @Override
  public void initHandler(final Map<String, String> environmentMap) {
    this.handler = new TestCoreRequestHandler(environmentMap);
  }
}
