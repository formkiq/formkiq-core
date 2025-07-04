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

import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityStatus;
import com.formkiq.strings.Strings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import static com.formkiq.strings.Strings.isEmpty;

/**
 * Convert {@link ApiGatewayRequestEvent} to {@link UserActivity}.
 */
public class ApiGatewayRequestToUserActivityFunction
    implements BiFunction<ApiGatewayRequestEvent, ApiRequestHandlerResponse, UserActivity.Builder> {

  /** List of Change Types. */
  private static final Collection<String> CHANGE_TYPES = Set.of("create", "update", "delete");

  /** S3 Timestamp Formatter. */
  private static final DateTimeFormatter S3_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  @Override
  public UserActivity.Builder apply(final ApiGatewayRequestEvent request,
      final ApiRequestHandlerResponse response) {

    UserActivity.Builder builder =
        UserActivity.builder().source("HTTP").status(UserActivityStatus.COMPLETE);

    if (response != null) {
      builder = builder.status(response.statusCode());
    }

    String documentId = findResourceId(request, response, "documentId");
    String entityId = findResourceId(request, response, "entityId");
    String entityTypeId = findResourceId(request, response, "entityTypeId");

    builder = builder.documentId(documentId).entityId(entityId).entityTypeId(entityTypeId);

    if (request != null) {

      String entityNamespace = request.getQueryStringParameter("namespace");
      String resource = getResource(request);
      String activityType = getType(request);


      builder = builder.resource(resource).entityNamespace(entityNamespace).type(activityType)
          .insertedDate(getInsertedDate(request)).sourceIpAddress(getSourceIp(request))
          .body(getBody(request));

      if (isGenerateS3Key(request)) {
        String resourceId = Strings.notEmpty(documentId, entityId, entityTypeId);
        String parentId = !isEmpty(entityId) ? entityTypeId : null;
        builder = builder.s3Key(generateS3Key(resource, parentId, resourceId));
      }
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
    return split[1].toLowerCase();
  }

  private static String getType(final ApiGatewayRequestEvent request) {
    String method = request.getHttpMethod().toUpperCase();

    return switch (method) {
      case "GET" -> "view";
      case "POST" -> "create";
      case "PUT", "PATCH" -> "update";
      case "DELETE" -> "delete";
      default -> "";
    };
  }

  private static String getSourceIp(final ApiGatewayRequestEvent request) {
    return request != null && request.getRequestContext() != null
        && request.getRequestContext().getIdentity() != null
            ? (String) request.getRequestContext().getIdentity().get("sourceIp")
            : "unknown";
  }

  private static Instant getInsertedDate(final ApiGatewayRequestEvent request) {
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

  private static boolean isGenerateS3Key(final ApiGatewayRequestEvent request) {
    String url = request.getResource();
    String type = getType(request);
    return isDocumentView(url, type) || isDocumentChange(url, type) || isEntityChange(url, type);
  }

  private static boolean isDocumentChange(final String url, final String type) {
    return CHANGE_TYPES.contains(type) && url.startsWith("/documents");
  }

  private static boolean isDocumentView(final String url, final String type) {
    return "view".equals(type) && url.startsWith("/documents")
        && (url.endsWith("/url") || url.endsWith("/content"));
  }

  private static boolean isEntityChange(final String url, final String type) {
    return CHANGE_TYPES.contains(type)
        && (url.startsWith("/entities") || url.startsWith("/entityTypes"));
  }

  private static String generateS3Key(final String resource, final String parentId,
      final String resourceId) {

    String timestamp = S3_TIMESTAMP_FORMATTER.format(Instant.now());

    LocalDate date = LocalDate.now(ZoneOffset.UTC);
    int year = date.getYear();
    int month = date.getMonthValue();
    int day = date.getDayOfMonth();

    String uuid = UUID.randomUUID().toString();
    String resourceType = parentId != null ? resource + "/" + parentId : resource;

    return !isEmpty(resource) && !isEmpty(resourceId)
        ? String.format("activities/%s/year=%d/month=%02d/day=%02d/%s/%s_%s.json", resourceType,
            year, month, day, resourceId, timestamp, uuid)
        : null;
  }
}
