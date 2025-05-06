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
package com.formkiq.aws.services.lambda.eventsourcing;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.module.eventsourcing.adapters.InboundAdapter;
import com.formkiq.module.eventsourcing.commands.HttpCommand;

import java.time.Instant;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Transforms an ApiGatewayRequestEvent into an HttpCommand for use in the event-sourcing pipeline.
 */
public class ApiGatewayRequestEventToHttpCommandTransformer
    implements InboundAdapter<ApiGatewayRequestEvent, HttpCommand> {

  /** {@link ApiAuthorization}. */
  private final ApiAuthorization auth;

  /**
   * constructor.
   * 
   * @param apiAuthorization {@link ApiAuthorization}
   */
  public ApiGatewayRequestEventToHttpCommandTransformer(final ApiAuthorization apiAuthorization) {
    this.auth = apiAuthorization;
  }

  @Override
  public HttpCommand apply(final ApiGatewayRequestEvent event) {

    HttpCommand.Builder builder = HttpCommand.builder();

    if (event != null) {
      Map<String, List<String>> headers = toMultiValueMap(event.getHeaders());
      Map<String, List<String>> queryParams = toMultiValueMap(event.getQueryStringParameters());

      builder.user(this.auth.getUsername()).timestamp(Instant.now()).method(event.getHttpMethod())
          .path(event.getPath()).headers(headers).queryParams(queryParams).body(event.getBody());

      addEntity(builder, event.getResource(), event.getPathParameters());
    }

    return builder.build();
  }

  /**
   * Add Entity / EntityId to {@link HttpCommand}.
   * 
   * @param builder HttpCommand.Builder
   * @param resource {@link String}
   * @param pathParameters {@link Map}
   */
  private void addEntity(final HttpCommand.Builder builder, final String resource,
      final Map<String, String> pathParameters) {
    String[] parts = resource.split("/");
    int pos = findEntityIdPosition(List.of(parts));
    if (pos > 0) {
      String key = parts[pos].substring(1, parts[pos].length() - 1);
      builder.entityId(pathParameters.get(key));
      builder.entityType(parts[pos - 1]);
    }
  }

  private int findEntityIdPosition(final List<String> list) {
    return IntStream.range(0, list.size()).filter(i -> {
      final String element = list.get(i);
      return element != null && element.endsWith("Id}");
    }).findFirst().orElse(-1);
  }

  private Map<String, List<String>> toMultiValueMap(final Map<String, String> singleValueMap) {
    if (singleValueMap == null) {
      return Collections.emptyMap();
    }
    return singleValueMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue())));
  }
}
