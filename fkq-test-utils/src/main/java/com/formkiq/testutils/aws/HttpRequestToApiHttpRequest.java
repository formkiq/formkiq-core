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

    return new ApiHttpRequest().method(httpRequest.getMethod().getValue()).resource(resource)
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
