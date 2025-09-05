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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.formkiq.testutils.api.JwtTokenDecoder;
import org.mockserver.model.HttpRequest;

/**
 * 
 * {@link Function} for transfoming {@link HttpRequest} to {@link ApiHttpRequest}.
 *
 */
public class HttpRequestToApiHttpRequest implements Function<HttpRequest, ApiHttpRequest> {

  /** {@link List} {@link String}. */
  private final List<String[]> resourceSplits;
  /** {@link Collection} {@link String}. */
  private final Collection<String> resourceUrls;

  /**
   * constructor.
   * 
   * @param resources {@link Collection} {@link String}
   */
  public HttpRequestToApiHttpRequest(final Collection<String> resources) {
    this.resourceUrls = resources;
    this.resourceSplits = resources.stream().filter(Objects::nonNull).map(r -> r.split("/"))
        .collect(Collectors.toList());
  }

  private boolean isPublicEndpoint(final String url) {
    List<String> publicUrls =
        List.of("/login", "/forgotPassword", "/confirmRegistration", "/changePassword");
    return publicUrls.stream().anyMatch(url::contains);
  }

  @Override
  public ApiHttpRequest apply(final HttpRequest httpRequest) {

    List<String> headers = httpRequest.getHeader("Authorization");
    if (!isPublicEndpoint(httpRequest.getPath().getValue()) && headers.isEmpty()) {
      throw new RuntimeException("missing 'Authorization' header");
    }

    JwtTokenDecoder decoder = getDecoder(headers);

    String path = httpRequest.getPath().getValue();

    String resource = findResource(path);

    Map<String, String> pathParameters = generatePathParameters(resource, path);

    String body = httpRequest.getBodyAsString();

    Map<String, String> queryParameters =
        httpRequest.getQueryStringParameterList().stream().collect(Collectors.toMap(
            p -> decode(p.getName().getValue()), p -> decode(p.getValues().get(0).getValue())));

    String group = getGroup(decoder);
    Map<String, List<String>> permissions = getPermissions(decoder);

    Map<String, String> httpHeaders = httpRequest.getHeaders().getEntries().stream().collect(
        Collectors.toMap(h -> h.getName().getValue(), h -> h.getValues().get(0).getValue()));

    return new ApiHttpRequest().headers(httpHeaders).httpMethod(httpRequest.getMethod().getValue())
        .resource(resource).path(path).pathParameters(pathParameters)
        .queryParameters(queryParameters).user(getUsername(decoder)).group(group)
        .permissions(permissions).body(body);
  }

  private Map<String, List<String>> getPermissions(final JwtTokenDecoder decoder) {
    return decoder != null ? decoder.getPermissions() : Map.of();
  }

  private String getGroup(final JwtTokenDecoder decoder) {
    return decoder != null ? String.join(" ", decoder.getGroups()) : "";
  }

  private JwtTokenDecoder getDecoder(final List<String> headers) {
    String bearerToken =
        !headers.isEmpty() && headers.get(0).startsWith("ey") ? headers.get(0) : null;
    return bearerToken != null ? new JwtTokenDecoder(headers.get(0)) : null;
  }

  private String getUsername(final JwtTokenDecoder decoder) {
    return decoder != null ? decoder.getUsername() : "";
  }

  /**
   * URL Decode {@link String}.
   * 
   * @param s {@link String}
   * @return {@link String}
   */
  private String decode(final String s) {
    return URLDecoder.decode(s, StandardCharsets.UTF_8);
  }

  private String findResource(final String path) {

    Optional<String> o =
        this.resourceUrls.stream().filter(r -> r != null && r.equals(path)).findAny();

    if (o.isEmpty()) {

      String[] s = path.split("/");
      List<String[]> matches =
          this.resourceSplits.stream().filter(r -> r.length == s.length).filter(r -> {

            boolean match = false;

            for (int i = 0; i < r.length; i++) {
              match = r[i].equals(s[i]) || (r[i].startsWith("{") && r[i].endsWith("}"));
              if (!match) {
                break;
              }
            }

            return match;
          }).toList();

      if (matches.size() > 1) {

        Optional<String[]> oo =
            matches.stream().filter(m -> m[m.length - 1].equals(s[s.length - 1])).findFirst();

        if (oo.isPresent()) {
          matches = new ArrayList<>();
          matches.add(oo.get());
        } else {
          matches = matches.stream().filter(m -> !m[m.length - 1].startsWith("{")).toList();
        }
      }

      o = matches.size() == 1 ? Optional.of(String.join("/", matches.get(0))) : Optional.empty();
    }

    return o.orElse(null);
  }

  /**
   * Generate Path parameters.
   *
   * @param resource {@link String}
   * @param path {@link String}
   * @return {@link Map}
   */
  private Map<String, String> generatePathParameters(final String resource, final String path) {

    Map<String, String> map = new HashMap<>();

    if (resource == null) {
      throw new RuntimeException("cannot find resource in path '" + path + "'");
    }

    String[] resources = resource.split("/");
    String[] paths = path.split("/");

    for (int i = 0; i < resources.length; i++) {
      if (resources[i].startsWith("{") && resources[i].endsWith("}")) {
        map.put(decode(resources[i].substring(1, resources[i].length() - 1)), decode(paths[i]));
      }
    }

    return map;
  }
}
