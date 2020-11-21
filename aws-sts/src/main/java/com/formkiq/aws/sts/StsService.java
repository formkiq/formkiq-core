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
package com.formkiq.aws.sts;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest.Builder;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

/**
 * 
 * Sts Services.
 *
 */
public class StsService {

  /**
   * Decode Encoded {@link String}.
   * 
   * @param encoded {@link String}
   * @return {@link String}
   */
  private static String decode(final String encoded) {
    try {
      return encoded == null ? null : URLDecoder.decode(encoded, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException("Impossible: UTF-8 is a required encoding", e);
    }
  }

  /** {@link StsConnectionBuilder}. */
  private StsConnectionBuilder builder;

  /**
   * Constructor.
   * 
   * @param s3connectionBuilder {@link StsConnectionBuilder}
   */
  public StsService(final StsConnectionBuilder s3connectionBuilder) {
    this.builder = s3connectionBuilder;
  }

  /**
   * STS Assume Role.
   * 
   * @param sts {@link StsClient}
   * @param roleSessionName {@link String}
   * @param roleArn {@link String}
   * @return {@link AssumeRoleResponse}
   */
  public AssumeRoleResponse assumeRole(final StsClient sts, final String roleSessionName,
      final String roleArn) {
    AssumeRoleResponse response = sts.assumeRole(
        AssumeRoleRequest.builder().roleArn(roleArn).roleSessionName(roleSessionName).build());
    return response;
  }

  /**
   * Build {@link StsClient}.
   * 
   * @return {@link StsClient}
   */
  public StsClient buildClient() {
    return this.builder.build();
  }

  /**
   * Create a Signed Request.
   * 
   * @param requestbuilder {@link Builder}
   * @param params {@link Aws4SignerParams}
   * 
   * @return {@link SdkHttpFullRequest}
   * @throws URISyntaxException URISyntaxException
   */
  public SdkHttpFullRequest createSignedRequest(final SdkHttpFullRequest.Builder requestbuilder,
      final Aws4SignerParams params) throws URISyntaxException {

    Aws4Signer signer = Aws4Signer.create();

    SdkHttpFullRequest req = signer.sign(requestbuilder.build(), params);

    return req;
  }

  /**
   * Create a Signed Request.
   * 
   * @param uri {@link String}
   * @param method {@link SdkHttpMethod}
   * @param signerParams {@link Aws4SignerParams}
   * @param payload {@link RequestBody}
   * @param contentType {@link String}
   * @return {@link SdkHttpFullRequest}
   * @throws URISyntaxException URISyntaxException
   * @throws MalformedURLException MalformedURLException
   */
  public SdkHttpFullRequest createSignedRequest(final String uri, final SdkHttpMethod method,
      final Aws4SignerParams signerParams, final RequestBody payload, final String contentType)
      throws URISyntaxException, MalformedURLException {

    URL url = new URL(uri);

    SdkHttpFullRequest.Builder requestBuilder =
        SdkHttpFullRequest.builder().uri(new URI(uri)).method(method);

    if (url.getQuery() != null) {
      Map<String, List<String>> queryParams = Pattern.compile("&")
          .splitAsStream(new URL(uri).getQuery()).map(s -> Arrays.copyOf(s.split("="), 2))
          .collect(groupingBy(s -> decode(s[0]), mapping(s -> decode(s[1]), toList())));

      for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
        requestBuilder =
            requestBuilder.appendRawQueryParameter(e.getKey(), String.join(",", e.getValue()));
      }
    }

    if (payload != null) {
      if (contentType != null) {
        requestBuilder = requestBuilder.appendHeader("Content-Type", contentType);
      }

      requestBuilder = requestBuilder.contentStreamProvider(payload.contentStreamProvider());
    }

    return createSignedRequest(requestBuilder, signerParams);
  }
}
