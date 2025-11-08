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
package com.formkiq.urls;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Parses a URL string into UrlParts.
 */
public class UrlParser implements Function<String, UrlParts> {

  @Override
  public UrlParts apply(final String url) {
    Objects.requireNonNull(url, "url");
    URI uri = URI.create(url);

    String scheme = uri.getScheme();
    String host = uri.getHost();
    Integer port = (uri.getPort() == -1) ? null : uri.getPort();

    List<String> pathSegments = parsePath(uri.getRawPath());
    Map<String, List<String>> queryParams = parseQuery(uri.getRawQuery());

    return new UrlParts(scheme, host, port, pathSegments, queryParams);
  }

  private String decode(final String s) {
    return URLDecoder.decode(s, StandardCharsets.UTF_8);
  }

  private List<String> parsePath(final String rawPath) {
    if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
      return List.of();
    }

    List<String> result = new ArrayList<>();
    for (String seg : rawPath.split("/")) {
      if (!seg.isEmpty()) {
        result.add(decode(seg));
      }
    }

    return List.copyOf(result);
  }

  private Map<String, List<String>> parseQuery(final String rawQuery) {
    Map<String, List<String>> params = new LinkedHashMap<>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return params;
    }

    for (String pair : rawQuery.split("&")) {
      if (!pair.isEmpty()) {
        String[] kv = pair.split("=", 2);
        String key = decode(kv[0]);
        String value = (kv.length > 1) ? decode(kv[1]) : "";
        params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
      }
    }

    return params;
  }
}
