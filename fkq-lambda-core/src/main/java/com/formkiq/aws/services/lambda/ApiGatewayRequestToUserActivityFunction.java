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
package com.formkiq.aws.services.lambda;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.useractivities.UserActivityStatus;
import com.formkiq.aws.dynamodb.useractivities.UserActivityType;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.plugins.useractivity.UserActivityContextData;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Convert {@link ApiGatewayRequestEvent} to {@link UserActivity}.
 */
public class ApiGatewayRequestToUserActivityFunction {

  /** List of Change Types. */
  private static final Collection<UserActivityType> CHANGE_TYPES =
      Set.of(UserActivityType.CREATE, UserActivityType.UPDATE, UserActivityType.DELETE);

  /**
   * Build {@link UserActivity.Builder}.
   * 
   * @param authorization {@link ApiAuthorization}
   * @param request {@link ApiGatewayRequestEvent}
   * @param response ApiRequestHandlerResponse
   * @return {@link UserActivity.Builder}
   */
  public UserActivity.Builder apply(final ApiAuthorization authorization,
      final ApiGatewayRequestEvent request, final ApiRequestHandlerResponse response) {

    UserActivity.Builder builder = UserActivity.builder().source("HTTP")
        .status(UserActivityStatus.COMPLETE).insertedDate(getInsertedDate(request));

    if (response != null) {
      builder = builder.status(response.statusCode());
    }

    String documentId = findResourceId(request, response, "documentId");
    String entityId = findResourceId(request, response, "entityId");
    String entityTypeId = findResourceId(request, response, "entityTypeId");

    builder = builder.documentId(documentId).entityId(entityId).entityTypeId(entityTypeId);

    UserActivityContextData data = UserActivityContext.get();
    if (data != null) {
      builder.type(data.activityType());
      builder.changes(data.changeRecords());
    }

    if (request != null) {

      String entityNamespace = request.getQueryStringParameter("namespace");
      String resource = getResource(request);

      builder = builder.resource(resource).entityNamespace(entityNamespace)
          .sourceIpAddress(getSourceIp(request)).body(getBody(request))
          .userId(authorization != null ? authorization.getUsername() : "System");
    }

    return builder;
  }

  private String getBody(final ApiGatewayRequestEvent request) {
    try {
      return request.getBodyAsString();
    } catch (BadException e) {
      return null;
    }
  }

  private String findResourceId(final ApiGatewayRequestEvent request,
      final ApiRequestHandlerResponse response, final String resourceKey) {
    String resourceId = request != null ? request.getPathParameter(resourceKey) : null;

    if (resourceId == null && response != null) {
      if (response.body() instanceof Map) {
        resourceId = (String) ((Map<String, Object>) response.body()).get(resourceKey);
      }
    }

    return resourceId;
  }

  /**
   * Get the resource name.
   * 
   * @param request {@link ApiGatewayRequestEvent}
   * @return String
   */
  private static String getResource(final ApiGatewayRequestEvent request) {
    String[] split = request.getResource().split("/");
    return split[1];
  }

  private static UserActivityType getType(final ApiGatewayRequestEvent request) {

    UserActivityType type;
    String method = request.getHttpMethod().toUpperCase();

    if (request.getResource().equals("/documents/upload")) {
      type = UserActivityType.CREATE;
    } else {
      type = switch (method) {
        case "GET" -> UserActivityType.VIEW;
        case "POST" -> UserActivityType.CREATE;
        case "PUT", "PATCH" -> UserActivityType.UPDATE;
        case "DELETE" -> UserActivityType.DELETE;
        default -> null;
      };
    }

    return type;
  }

  private static String getSourceIp(final ApiGatewayRequestEvent request) {
    return request != null && request.getRequestContext() != null
        && request.getRequestContext().getIdentity() != null
            ? (String) request.getRequestContext().getIdentity().get("sourceIp")
            : "unknown";
  }

  private Instant getInsertedDate(final ApiGatewayRequestEvent request) {
    Instant insertedDate = null;
    if (request != null && request.getRequestContext() != null) {
      Long epochMillis = request.getRequestContext().getRequestTimeEpoch();
      if (epochMillis != null && epochMillis > 0) {
        insertedDate = Instant.ofEpochMilli(epochMillis);
      }
    }

    if (insertedDate == null) {
      insertedDate = Instant.now();
    }
    return insertedDate;
  }

  // private boolean isGenerateS3Key(final ApiGatewayRequestEvent request) {
  // String url = request.getResource();
  // UserActivityType type = getType(request);
  // return type != null
  // && (isDocumentView(url, type) || isDocumentChange(url, type) || isEntityChange(url, type));
  // }
  //
  // private boolean isDocumentChange(final String url, final UserActivityType type) {
  // return CHANGE_TYPES.contains(type) && url.startsWith("/documents");
  // }
  //
  // private boolean isDocumentView(final String url, final UserActivityType type) {
  // return UserActivityType.VIEW.equals(type) && url.startsWith("/documents")
  // && (url.endsWith("/url") || url.endsWith("/content"));
  // }
  //
  // private boolean isEntityChange(final String url, final UserActivityType type) {
  // return CHANGE_TYPES.contains(type)
  // && (url.startsWith("/entities") || url.startsWith("/entityTypes"));
  // }
}
