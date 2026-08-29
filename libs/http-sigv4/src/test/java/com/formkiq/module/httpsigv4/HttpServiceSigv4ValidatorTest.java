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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpServiceSigv4ValidatorTest {

  /** Signing credentials. */
  private static final AwsCredentials CREDENTIALS =
      AwsBasicCredentials.create("access-key", "secret-key");
  /** Signing time. */
  private static final Instant SIGNING_TIME = Instant.parse("2026-08-29T12:00:00Z");
  /** Request URI. */
  private static final URI URI =
      java.net.URI.create("http://api:8080/documents/id/ocr?siteId=default");

  private HttpServiceSigv4Validator createValidator(final Instant now) {
    return new HttpServiceSigv4Validator("execute-api", Region.US_EAST_2, CREDENTIALS,
        Duration.ofMinutes(5), Clock.fixed(now, ZoneOffset.UTC));
  }

  private Map<String, List<String>> headers(final SdkHttpFullRequest request) {
    Map<String, List<String>> headers = new HashMap<>();
    request.headers().forEach((key, values) -> headers.put(key, new ArrayList<>(values)));
    return headers;
  }

  private SdkHttpFullRequest signedRequest(final byte[] payload) {
    SdkHttpFullRequest request = SdkHttpFullRequest.builder().uri(URI).method(SdkHttpMethod.PUT)
        .appendHeader("Content-Type", "application/json")
        .contentStreamProvider(() -> new ByteArrayInputStream(payload)).build();
    Aws4SignerParams params = Aws4SignerParams.builder().signingName("execute-api")
        .signingRegion(Region.US_EAST_2).awsCredentials(CREDENTIALS)
        .signingClockOverride(Clock.fixed(SIGNING_TIME, ZoneOffset.UTC)).build();
    return Aws4Signer.create().sign(request, params);
  }

  @Test
  public void testExpiredRequest() {
    byte[] payload = "{\"content\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    SdkHttpFullRequest request = signedRequest(payload);

    assertFalse(createValidator(SIGNING_TIME.plus(Duration.ofMinutes(6))).isValid("PUT", URI,
        headers(request), payload));
  }

  @Test
  public void testInvalidPayload() {
    byte[] payload = "{\"content\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    SdkHttpFullRequest request = signedRequest(payload);

    assertFalse(createValidator(SIGNING_TIME).isValid("PUT", URI, headers(request),
        "changed".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void testInvalidRequestPath() {
    byte[] payload = "{\"content\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    SdkHttpFullRequest request = signedRequest(payload);

    assertFalse(createValidator(SIGNING_TIME).isValid("PUT",
        java.net.URI.create("http://api:8080/documents/other/ocr?siteId=default"), headers(request),
        payload));
  }

  @Test
  public void testInvalidSignature() {
    byte[] payload = "{\"content\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    SdkHttpFullRequest request = signedRequest(payload);
    Map<String, List<String>> headers = headers(request);
    headers.computeIfPresent("Authorization", (key, values) -> {
      String authorization = values.getFirst();
      char replacement = authorization.endsWith("0") ? '1' : '0';
      return List.of(authorization.substring(0, authorization.length() - 1) + replacement);
    });

    assertFalse(createValidator(SIGNING_TIME).isValid("PUT", URI, headers, payload));
  }

  @Test
  public void testValidRequest() {
    byte[] payload = "{\"content\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    SdkHttpFullRequest request = signedRequest(payload);

    assertTrue(createValidator(SIGNING_TIME).isValid("PUT", URI, headers(request), payload));
  }
}
