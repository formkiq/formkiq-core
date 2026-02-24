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
package com.formkiq.stacks.lambda.s3.cognito;

import com.formkiq.graalvm.annotations.Reflectable;

import java.util.Map;

@Reflectable
public record CognitoCustomMessageEvent(String triggerSource, String region, String userName,
    CallerContext callerContext, Request request, Response response) {
  @Reflectable
  public record CallerContext(String clientId) {
  }

  @Reflectable
  public record Request(String codeParameter, String usernameParameter,
      Map<String, String> userAttributes) {
  }

  @Reflectable
  public record Response(String emailSubject, String emailMessage) {
  }

  public CognitoCustomMessageEvent withEmail(final String subject, final String message) {
    return new CognitoCustomMessageEvent(this.triggerSource, this.region, this.userName,
        this.callerContext, this.request, new Response(subject, message));
  }
}
