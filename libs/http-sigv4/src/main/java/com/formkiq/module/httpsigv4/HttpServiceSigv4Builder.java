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
package com.formkiq.module.httpsigv4;

import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.formkiq.module.http.HttpHeaders;
import java.net.http.HttpResponse;

/**
 * Fluent builder to execute HTTP calls via an existing {@link HttpService} without needing separate
 * GET/POST/PUT/DELETE/PATCH methods at call sites. You configure the request once (URL, headers,
 * query parameters, and an optional body in String/byte[]/Path form), choose the HTTP method, and
 * then call {@link #execute()}.
 *
 * <p>
 * Usage example:
 * 
 * <pre>
 * HttpResponse&lt;String&gt; resp = new HttpServiceBuilder(httpService)
 *     .url("<a href="https://api.example.com/items">...</a>")
 *     .queryParams(Map.of("limit", "25"))
 *     .headers(HttpHeaders.of(Map.of("Authorization", List.of("Bearer ...")), (k, v) -> true))
 *     .post()
 *     .body("{\"name\":\"Alpha\"}")
 *     .execute();
 * </pre>
 */
public class HttpServiceSigv4Builder {

  /** Supported HTTP methods. */
  private enum HttpMethod {
    /** GET. */
    GET,
    /** POST. */
    POST,
    /** PUT. */
    PUT,
    /** DELETE. */
    DELETE,
    /** PATCH. */
    PATCH
  }

  /** {@link HttpService}. */
  private final HttpService service;
  /** Url. */
  private String url;
  /** {@link HttpHeaders}. */
  private Optional<HttpHeaders> headers = Optional.empty();
  /** Query Params. */
  private Optional<Map<String, String>> queryParams = Optional.empty();
  /** Body String. */
  private Optional<String> bodyString = Optional.empty();
  /** {@link HttpMethod}. */
  private HttpMethod method = HttpMethod.GET;

  /**
   * constructor.
   */
  public HttpServiceSigv4Builder() {
    this.service = new HttpServiceJdk11();
  }

  /**
   * constructor.
   * 
   * @param region {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param serviceName {@link String}
   */
  public HttpServiceSigv4Builder(final Region region, final AwsCredentials awsCredentials,
      final String serviceName) {
    this.service = new HttpServiceSigv4(region, awsCredentials, serviceName);
  }

  /**
   * constructor.
   * 
   * @param region {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param serviceName {@link String}
   * @param executor {@link Executor}
   */
  public HttpServiceSigv4Builder(final Region region, final AwsCredentials awsCredentials,
      final String serviceName, final Executor executor) {
    this.service = new HttpServiceSigv4(region, awsCredentials, serviceName, executor);
  }

  /**
   * Set the target URL.
   * 
   * @param httpUrl {@link String}
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder url(final String httpUrl) {
    this.url = Objects.requireNonNull(httpUrl, "httpUrl is required");
    return this;
  }

  /**
   * Set headers using {@link HttpHeaders}.
   * 
   * @param httpHeaders {@link Optional}
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder headers(final Optional<HttpHeaders> httpHeaders) {
    this.headers = httpHeaders;
    return this;
  }

  /**
   * Set query parameters appended to the URL when the call is executed.
   * 
   * @param params {@link Map}
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder queryParams(final Map<String, String> params) {
    this.queryParams = Optional.ofNullable(params);
    return this;
  }

  /**
   * Use HTTP GET.
   * 
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder get() {
    this.method = HttpMethod.GET;
    return this;
  }

  /**
   * Use HTTP POST.
   * 
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder post() {
    this.method = HttpMethod.POST;
    return this;
  }

  /**
   * Use HTTP PUT.
   * 
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder put() {
    this.method = HttpMethod.PUT;
    return this;
  }

  /**
   * Use HTTP DELETE.
   * 
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder delete() {
    this.method = HttpMethod.DELETE;
    return this;
  }

  /**
   * Use HTTP PATCH.
   * 
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder patch() {
    this.method = HttpMethod.PATCH;
    return this;
  }

  /**
   * Provide a String body (e.g., JSON).
   * 
   * @param body {@link String}
   * @return {@link HttpServiceSigv4Builder}
   */
  public HttpServiceSigv4Builder body(final String body) {
    this.bodyString = Optional.ofNullable(body);
    return this;
  }

  /**
   * Execute the configured request through the backing {@link HttpService} and return the
   * {@link HttpResponse}. If multiple body setters were called, the most recent one wins.
   * 
   * @return {@link HttpResponse}
   */
  public HttpResponse<String> execute() throws IOException {
    ensureUrl();

    return switch (this.method) {
      case GET -> service.get(url, headers, queryParams);
      case DELETE -> service.delete(url, headers, queryParams);
      case PATCH -> service.patch(url, headers, queryParams, bodyString.orElse(""));
      case POST -> service.post(url, headers, queryParams, bodyString.orElse(""));
      case PUT -> service.put(url, headers, queryParams, bodyString.orElse(""));
    };
  }

  private void ensureUrl() {
    if (this.url == null || this.url.isBlank()) {
      throw new IllegalStateException("url must be set");
    }
  }
}

