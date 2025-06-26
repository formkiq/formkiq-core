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

import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Convert {@link ApiGatewayRequestEvent} to {@link UserActivity}.
 */
public class ApiGatewayRequestToUserActivityFunction
    implements Function<ApiGatewayRequestEvent, UserActivity.Builder> {

  @Override
  public UserActivity.Builder apply(final ApiGatewayRequestEvent request) {

    String documentId = getDocumentId(request);
    String activityType = getType(request);

    return UserActivity.builder().documentId(documentId).type(activityType)
        .insertedDate(getInsertedDate(request)).status(UserActivityStatus.SUCCESS)
        .sourceIpAddress(getSourceIp(request));
  }

  private String getType(final ApiGatewayRequestEvent request) {
    return request != null ? request.getHttpMethod() : null;
  }

  private static String getDocumentId(final ApiGatewayRequestEvent request) {
    Map<String, String> map = getPathParameters(request);
    return map.get("documentId");
  }

  private static Map<String, String> getPathParameters(final ApiGatewayRequestEvent request) {
    return request != null && request.getPathParameters() != null ? request.getPathParameters()
        : Collections.emptyMap();
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
      insertedDate = Instant.now(); // Fallback
    }
    return insertedDate;
  }
}
