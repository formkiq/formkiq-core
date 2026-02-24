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
public record CognitoCustomMessageEvent(String version, String region, String userPoolId,
    String userName, CallerContext callerContext, String triggerSource, Request request,
    Response response) {

  @Reflectable
  public record CallerContext(String awsSdkVersion, String clientId) {
  }

  @Reflectable
  public record Request(Map<String, String> userAttributes, String codeParameter,
      String linkParameter, String usernameParameter) {
  }

  @Reflectable
  public record Response(String smsMessage, String emailMessage, String emailSubject) {
  }

  /** Return a copy of the event with updated email subject + message (keeps smsMessage as-is). */
  public CognitoCustomMessageEvent withEmail(final String subject, final String message) {
    final Response r = this.response != null ? this.response : new Response(null, null, null);
    return new CognitoCustomMessageEvent(this.version, this.region, this.userPoolId, this.userName,
        this.callerContext, this.triggerSource, this.request,
        new Response(r.smsMessage(), message, subject));
  }
}
