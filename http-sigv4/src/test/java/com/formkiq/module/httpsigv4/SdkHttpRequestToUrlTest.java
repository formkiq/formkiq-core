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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Unit Test for {@link SdkHttpRequestToUrl}.
 */
public class SdkHttpRequestToUrlTest {

  /** {@link SdkHttpRequestToUrl}. */
  private final SdkHttpRequestToUrl toUrl = new SdkHttpRequestToUrl();

  @Test
  void testBasicUrlNoQuery() {
    SdkHttpFullRequest req = SdkHttpFullRequest.builder().protocol("https").host("example.com")
        .encodedPath("/file.pdf").method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertEquals("https://example.com/file.pdf", url);
  }

  @Test
  void testMissingPathDefaultsToSlash() {
    SdkHttpFullRequest req = SdkHttpFullRequest.builder().protocol("https").host("example.com")
        .encodedPath("").method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertEquals("https://example.com/", url);
  }

  @Test
  void testNullValueBecomesEmptyValue() {
    SdkHttpFullRequest req =
        SdkHttpFullRequest.builder().protocol("https").host("example.com").encodedPath("/download")
            .putRawQueryParameter("token", (String) null).method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertEquals("https://example.com/download?token=", url);
  }

  @Test
  void testPreservesAlreadyEncodedValues() {
    // value contains %2F - must remain exactly %2F, not double-encoded
    SdkHttpFullRequest req =
        SdkHttpFullRequest.builder().protocol("https").host("example.com").encodedPath("/path")
            .rawQueryParameters(
                Map.of("cred", List.of("ASIA123%2F20250101%2Fus-east-1%2Flambda%2Faws4_request")))
            .method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertEquals(
        "https://example.com/path?cred=ASIA123%252F20250101%252Fus-east-1%252Flambda%252Faws4_request",
        url);
  }

  @Test
  void testQueryParametersSingleAndMultiValue() {
    SdkHttpFullRequest req =
        SdkHttpFullRequest.builder().protocol("https").host("example.com").encodedPath("/search")
            .rawQueryParameters(Map.of("q", List.of("test"), "tag", List.of("a", "b")))
            .method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertTrue(url.startsWith("https://example.com/search?"));
    assertTrue(url.contains("q=test"));
    assertTrue(url.contains("tag=a"));
    assertTrue(url.contains("tag=b"));
  }

  @Test
  void testSecurityTokenPreservesEncoding() {
    String encodedToken = "IQoJ+&";

    SdkHttpFullRequest req = SdkHttpFullRequest.builder().protocol("https").host("lambda-url.aws")
        .encodedPath("/").putRawQueryParameter("X-Amz-Security-Token", encodedToken)
        .method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);

    assertEquals("https://lambda-url.aws/?X-Amz-Security-Token=IQoJ%2B%26", url);
  }

  @Test
  void testWithPort() {
    SdkHttpFullRequest req = SdkHttpFullRequest.builder().protocol("http").host("localhost")
        .encodedPath("/index.html").method(SdkHttpMethod.GET).build();

    String url = toUrl.apply(req);
    assertEquals("http://localhost/index.html", url);
  }
}
