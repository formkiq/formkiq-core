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

import java.util.StringJoiner;
import java.util.function.Function;

/**
 * {@link Function} to convert {@link SdkHttpFullRequest} to {@link String}.
 */
public final class SdkHttpRequestToUrl implements Function<SdkHttpFullRequest, String> {

  @Override
  public String apply(final SdkHttpFullRequest r) {
    var sb = new StringBuilder().append(r.protocol()).append("://").append(r.host());

    if (r.port() > 0) {
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
          join.add(k + "=" + (v == null ? "" : v));
        }
      });
      sb.append('?').append(join);
    }

    return sb.toString();
  }
}
