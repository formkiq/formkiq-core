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
package com.formkiq.testutils.aws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mockserver.model.HttpRequest;

/**
 * 
 * {@link Function} for transfoming {@link HttpRequest} to {@link ApiHttpRequest}.
 *
 */
public class HttpRequestToApiHttpRequest implements Function<HttpRequest, ApiHttpRequest> {

  /**
   * Is {@link String} a {@link UUID}.
   *
   * @param s {@link String}
   * @return boolean
   */
  private static boolean isUuid(final String s) {
    try {
      UUID.fromString(s);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public ApiHttpRequest apply(final HttpRequest httpRequest) {

    String authorization = httpRequest.getHeader("Authorization").get(0);
    JwtTokenDecoder decoder = new JwtTokenDecoder(authorization);

    String path = httpRequest.getPath().getValue();

    Map<String, String> pathParameters = generateResource(path);
    String resource = pathParameters.get("resource");
    pathParameters.remove("resource");

    String body = httpRequest.getBodyAsString();

    Map<String, String> queryParameters =
        httpRequest.getQueryStringParameterList().stream().collect(
            Collectors.toMap(p -> p.getName().getValue(), p -> p.getValues().get(0).getValue()));

    String group = decoder.getGroups().stream().collect(Collectors.joining(" "));

    return new ApiHttpRequest().httpMethod(httpRequest.getMethod().getValue()).resource(resource)
        .path(path).pathParameters(pathParameters).queryParameters(queryParameters)
        .user(decoder.getUsername()).group(group).body(body);
  }

  /**
   * Generate {@link ApiGatewayRequestEvent} resource.
   * 
   * @param path {@link String}
   * @return {@link Map}
   */
  private Map<String, String> generateResource(final String path) {

    Map<String, String> map = new HashMap<>();

    List<String> resource = new ArrayList<>();
    String[] split = path.split("/");

    String last = null;
    for (String s : split) {
      if (last != null && isUuid(s)) {
        String key = last.replaceAll("s$", "Id");
        map.put(key, s);

        resource.add(String.format("{%s}", key));
      } else {
        resource.add(s);
      }

      last = s;
    }

    map.put("resource", resource.stream().collect(Collectors.joining("/")));
    return map;
  }
}
