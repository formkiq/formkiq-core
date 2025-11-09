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

import software.amazon.awssdk.http.SdkHttpFullRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * {@link Function} to convert {@link SdkHttpFullRequest} to {@link String}.
 */
public final class SdkHttpRequestToUrl implements Function<SdkHttpFullRequest, String> {

  /** Http Port. */
  private static final int HTTP_PORT = 80;

  /** Https Port. */
  private static final int HTTPS_PORT = 443;

  private static String getEncode(final String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A")
        .replace("%7E", "~");
  }

  @Override
  public String apply(final SdkHttpFullRequest r) {
    var sb = new StringBuilder().append(r.protocol()).append("://").append(r.host());

    if (r.port() > 0 && !isHttps(r.protocol(), r.port()) && !isHttp(r.protocol(), r.port())) {
      sb.append(':').append(r.port());
    }

    // encodedPath() is already RFC3986-encoded; default to "/" if empty
    var path = r.encodedPath();
    sb.append((path == null || path.isEmpty()) ? "/" : path);

    var qp = r.rawQueryParameters(); // already-encoded keys/values
    if (!qp.isEmpty()) {
      var join = new StringJoiner("&");
      qp.forEach((k, vs) -> {
        for (String v : vs) {
          join.add(k + "=" + (v == null ? "" : getEncode(v)));
        }
      });
      sb.append('?').append(join);
    }

    return sb.toString();
  }

  private boolean isHttp(final String protocol, final int port) {
    return "http".equalsIgnoreCase(protocol) && port == HTTP_PORT;
  }

  private boolean isHttps(final String protocol, final int port) {
    return "https".equalsIgnoreCase(protocol) && port == HTTPS_PORT;
  }
}
