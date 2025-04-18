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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

/**
 *
 * SigV4 implementation of {@link HttpService}.
 *
 */
public final class HttpServiceSigv4 implements HttpService {

  /** Headers that are not allowed to be added to {@link HttpClient}. */
  private static final Set<String> NOT_ALLOWED_HEADERS = Set.of("connection", "content-length",
      "date", "expect", "from", "host", "upgrade", "via", "warning");

  /** {@link HttpClient}. */
  private final HttpClient client;
  /** {@link AwsCredentials}. */
  private final AwsCredentials signingCredentials;
  /** {@link Region}. */
  private final Region signingRegion;
  /** Sigv4 Signing Name. */
  private final String signingName;

  /**
   * constructor.
   *
   * @param httpClient {@link HttpClient}
   * @param region {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param serviceName {@link String}
   */
  private HttpServiceSigv4(final HttpClient httpClient, final Region region,
      final AwsCredentials awsCredentials, final String serviceName) {
    this.client = httpClient;
    this.signingRegion = region;
    this.signingCredentials = awsCredentials;
    this.signingName = serviceName;

    if (region == null || awsCredentials == null) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * constructor.
   *
   * @param region {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param serviceName {@link String}
   */
  public HttpServiceSigv4(final Region region, final AwsCredentials awsCredentials,
      final String serviceName) {
    this(HttpClient.newHttpClient(), region, awsCredentials, serviceName);
  }

  /**
   * constructor.
   *
   * @param region {@link Region}
   * @param awsCredentials {@link AwsCredentials}
   * @param serviceName {@link String}
   * @param executor {@link Executor}
   */
  public HttpServiceSigv4(final Region region, final AwsCredentials awsCredentials,
      final String serviceName, final Executor executor) {
    this(HttpClient.newBuilder().executor(executor).build(), region, awsCredentials, serviceName);
  }

  /**
   * Build a {@link SdkHttpFullRequest.Builder}.
   *
   * @param uri URI
   * @param method {@link SdkHttpMethod}
   * @param headers {@link HttpHeaders}
   * @param parameters {@link Map}
   * @param payload {@link String}
   * @return {@link SdkHttpFullRequest.Builder}
   * @throws IOException IOException
   */
  private SdkHttpFullRequest.Builder buildRequest(final String uri, final SdkHttpMethod method,
      final Optional<HttpHeaders> headers, final Optional<Map<String, String>> parameters,
      final Optional<String> payload) throws IOException {

    SdkHttpFullRequest.Builder requestBuilder =
        SdkHttpFullRequest.builder().uri(toUri(uri)).method(method);

    if (parameters.isPresent()) {
      for (Map.Entry<String, String> e : parameters.get().entrySet()) {
        requestBuilder.appendRawQueryParameter(e.getKey(), e.getValue());
      }
    }

    if (headers.isPresent()) {
      for (Map.Entry<String, String> e : headers.get().getAll().entrySet()) {
        requestBuilder = requestBuilder.appendHeader(e.getKey(), e.getValue());
      }
    }

    if (payload.isPresent()) {
      StringContentStreamProvider provider = new StringContentStreamProvider(payload.get());
      requestBuilder = requestBuilder.contentStreamProvider(provider);
    }

    return requestBuilder;
  }

  @Override
  public HttpResponse<String> delete(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters) throws IOException {

    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.DELETE, headers, parameters, Optional.empty());
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  /**
   * Execute {@link SdkHttpFullRequest}.
   *
   * @param request {@link SdkHttpFullRequest}
   * @return {@link HttpResponse}
   * @throws IOException IOException
   */
  private HttpResponse<String> execute(final SdkHttpFullRequest request) throws IOException {

    Builder builder = HttpRequest.newBuilder().uri(request.getUri()).timeout(Duration.ofMinutes(1));

    Map<String, List<String>> headers = request.headers();
    for (Map.Entry<String, List<String>> e : headers.entrySet()) {

      if (!NOT_ALLOWED_HEADERS.contains(e.getKey().toLowerCase())) {
        String value = String.join(",", e.getValue());
        builder = builder.setHeader(e.getKey(), value);
      }
    }

    if (request.contentStreamProvider().isPresent()) {
      try (InputStream is = request.contentStreamProvider().get().newStream()) {
        byte[] data = IoUtils.toByteArray(is);
        builder.method(request.method().name(), BodyPublishers.ofByteArray(data));
        builder.header("x-amz-content-sha256", sha256Hex(data));
      }
    } else {
      builder.method(request.method().name(), BodyPublishers.noBody());
    }

    try {
      return this.client.send(builder.build(), BodyHandlers.ofString());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public HttpResponse<String> get(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters) throws IOException {
    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.GET, headers, parameters, Optional.empty());
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  @Override
  public HttpResponse<String> get(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters, final String payload) throws IOException {
    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.GET, headers, parameters, Optional.of(payload));
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  @Override
  public HttpResponse<InputStream> getAsInputStream(final String url,
      final Optional<HttpHeaders> headers, final Optional<Map<String, String>> parameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpResponse<String> patch(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters, final String payload) throws IOException {
    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.PATCH, headers, parameters, Optional.of(payload));
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  @Override
  public HttpResponse<String> post(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters, final String payload) throws IOException {
    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.POST, headers, parameters, Optional.of(payload));
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  @Override
  public HttpResponse<String> put(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters, final Path payload) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpResponse<String> put(final String url, final Optional<HttpHeaders> headers,
      final Optional<Map<String, String>> parameters, final String payload) throws IOException {
    SdkHttpFullRequest.Builder request =
        buildRequest(url, SdkHttpMethod.PUT, headers, parameters, Optional.of(payload));
    SdkHttpFullRequest req = sign(request);
    return execute(req);
  }

  /**
   * AWS Signature Version 4 signing.
   *
   * @param request {@link SdkHttpFullRequest.Builder}
   * @return {@link SdkHttpFullRequest}
   */
  private SdkHttpFullRequest sign(final SdkHttpFullRequest.Builder request) {

    SdkHttpFullRequest req = request.build();

    Aws4SignerParams params = Aws4SignerParams.builder().signingName(signingName)
        .signingRegion(this.signingRegion).awsCredentials(this.signingCredentials).build();

    Aws4Signer signer = Aws4Signer.create();

    req = signer.sign(req, params);

    return req;
  }

  /**
   * Convert {@link String} to {@link URI}.
   *
   * @param uri {@link String}
   * @return {@link URI}
   * @throws IOException IOException
   */
  private URI toUri(final String uri) throws IOException {
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private static String sha256Hex(final byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(data);
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
