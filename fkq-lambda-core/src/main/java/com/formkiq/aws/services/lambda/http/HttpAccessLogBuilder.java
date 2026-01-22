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
package com.formkiq.aws.services.lambda.http;

import java.util.Map;

/**
 * Builder for {@link HttpAccessLog}.
 */
public class HttpAccessLogBuilder {

  /** Request Time. */
  private String requestTime;
  /** Request Id. */
  private String requestId;
  /** Request Client. */
  private HttpAccessLog.Client client;
  /** Request User. */
  private HttpAccessLog.User user;
  /** Request Http. */
  private HttpAccessLog.Http http;
  /** Request Response. */
  private HttpAccessLog.Resp resp;
  /** Request User Agent. */
  private String userAgent;

  /**
   * Builds the {@link HttpAccessLog}.
   * 
   * @return {@link HttpAccessLog}
   */
  public HttpAccessLog build() {
    return new HttpAccessLog(requestTime, requestId, client, user, http, resp, userAgent, null);
  }

  // ---- Client ----
  public HttpAccessLogBuilder client(final HttpAccessLog.Client httpClient) {
    this.client = httpClient;
    return this;
  }

  public HttpAccessLogBuilder clientIp(final String ip) {
    this.client = new HttpAccessLog.Client(ip);
    return this;
  }

  // ---- Http ----
  public HttpAccessLogBuilder http(final HttpAccessLog.Http httpRequest) {
    this.http = httpRequest;
    return this;
  }

  public HttpAccessLogBuilder http(final String method, final String protocol, final String route,
      final Map<String, String> pathParameters, final Map<String, String> queryStringParameters) {
    this.http =
        new HttpAccessLog.Http(method, protocol, route, pathParameters, queryStringParameters);
    return this;
  }

  public HttpAccessLogBuilder requestId(final String httpRequestId) {
    this.requestId = httpRequestId;
    return this;
  }

  public HttpAccessLogBuilder requestTime(final String ts) {
    this.requestTime = ts;
    return this;
  }

  // ---- Response ----
  public HttpAccessLogBuilder resp(final HttpAccessLog.Resp httpResponse) {
    this.resp = httpResponse;
    return this;
  }

  public HttpAccessLogBuilder resp(final int status, final String message) {
    this.resp = new HttpAccessLog.Resp(status, message);
    return this;
  }

  // ---- User ----
  public HttpAccessLogBuilder user(final HttpAccessLog.User httpUser) {
    this.user = httpUser;
    return this;
  }

  // ---- User Agent & Referer ----
  public HttpAccessLogBuilder userAgent(final String ua) {
    this.userAgent = ua;
    return this;
  }

  public HttpAccessLogBuilder userId(final String id) {
    this.user = new HttpAccessLog.User(id);
    return this;
  }
}
