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
package com.formkiq.stacks.api;

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import java.util.Collections;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiGatewayRequestContext;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiPagination;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.CacheService;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.PaginationResults;
import com.google.gson.Gson;

/**
 * Cognito Helper Utils.
 *
 */
public final class ApiGatewayRequestEventUtil {

  /**
   * Create Pagination.
   * 
   * @param cacheService {@link CacheService}
   * @param event {@link ApiGatewayRequestEvent}
   * @param lastPagination {@link ApiPagination}
   * @param results {@link PaginationResults}
   * @param limit int
   * 
   * @return {@link ApiPagination}
   */
  public static ApiPagination createPagination(final CacheService cacheService,
      final ApiGatewayRequestEvent event, final ApiPagination lastPagination,
      final PaginationResults<?> results, final int limit) {

    ApiPagination current = null;
    final Map<String, String> q = getQueryParameterMap(event);

    Gson gson = GsonUtil.getInstance();
    PaginationMapToken token = results.getToken();

    if (isPaginationPrevious(q)) {

      String json = cacheService.read(q.get("previous"));
      current = gson.fromJson(json, ApiPagination.class);

    } else {

      current = new ApiPagination();

      current.setLimit(limit);
      current.setPrevious(lastPagination != null ? lastPagination.getNext() : null);
      current.setStartkey(token);
      current.setHasNext(token != null);

      cacheService.write(current.getNext(), gson.toJson(current));
    }

    return current;
  }

  /**
   * Get the calling Cognito Username.
   *
   * @param event {@link ApiGatewayRequestEvent}.
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  public static String getCallingCognitoUsername(final ApiGatewayRequestEvent event) {

    String username = null;

    ApiGatewayRequestContext requestContext = event.getRequestContext();

    if (requestContext != null) {

      Map<String, Object> authorizer = requestContext.getAuthorizer();
      Map<String, Object> identity = requestContext.getIdentity();

      if (identity != null) {

        Object user = identity.getOrDefault("user", null);
        if (user != null) {
          username = user.toString();
        }

        Object userArn = identity.getOrDefault("userArn", null);
        if (userArn != null && userArn.toString().contains(":user/")) {
          username = userArn.toString();
        }
      }

      if (authorizer != null && authorizer.containsKey("claims")) {

        Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
        if (claims.containsKey("username")) {
          username = claims.get("username").toString();
        } else if (claims.containsKey("cognito:username")) {
          username = claims.get("cognito:username").toString();
        }
      }
    }

    return username;
  }

  /**
   * Get ContentType from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   */
  public static String getContentType(final ApiGatewayRequestEvent event) {

    String contentType = null;
    Map<String, String> headers = event.getHeaders();

    if (headers != null) {
      contentType = headers.get("Content-Type");
      if (contentType == null) {
        contentType = headers.get("content-type");
      }
    }

    return contentType;
  }

  /**
   * Get Site Id.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   */
  public static String getSiteId(final ApiGatewayRequestEvent event) {

    String siteId = null;
    Map<String, String> map = event.getQueryStringParameters();

    if (map != null && map.containsKey("siteId")) {
      siteId = map.get("siteId");
    }

    return siteId;
  }


  /**
   * Get Limit Parameter.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return int
   */
  public static int getLimit(final LambdaLogger logger, final ApiGatewayRequestEvent event) {

    int limit = MAX_RESULTS;

    Map<String, String> q = getQueryParameterMap(event);

    if (q != null && q.containsKey("limit")) {
      try {
        limit = Integer.parseInt(q.get("limit"));
      } catch (NumberFormatException e) {
        logger.log(e.getMessage());
      }
    }

    if (limit < 1) {
      limit = MAX_RESULTS;
    }

    return limit;
  }

  /**
   * Is String NOT empty.
   * 
   * @param s {@link String}
   * @return boolean
   */
  public static boolean isNotBlank(final String s) {
    return s != null && s.length() > 0;
  }

  /**
   * Is has Next Pagination Token.
   * 
   * @param q {@link Map}
   * @return boolean
   */
  public static boolean isPaginationNext(final Map<String, String> q) {
    return isNotBlank(q.getOrDefault("next", null));
  }

  /**
   * Is has Previous Pagination Token.
   * 
   * @param q {@link Map}
   * @return boolean
   */
  public static boolean isPaginationPrevious(final Map<String, String> q) {
    return isNotBlank(q.getOrDefault("previous", null));
  }

  /**
   * Convert {@link String} to {@link ApiPagination}.
   *
   * @param cacheService {@link CacheService}
   * @param key {@link String}
   * 
   * @return {@link ApiPagination}
   */
  public static ApiPagination toPaginationToken(final CacheService cacheService, final String key) {

    ApiPagination pagination = null;

    if (isNotBlank(key)) {

      String json = cacheService.read(key);

      if (isNotBlank(json)) {
        Gson gson = GsonUtil.getInstance();
        pagination = gson.fromJson(json, ApiPagination.class);
      }
    }

    return pagination;
  }

  /**
   * Find Query Parameter 'next' or 'prev' and convert to {@link ApiPagination}.
   *
   * @param cacheService {@link CacheService}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link ApiPagination}
   */
  public static ApiPagination getPagination(final CacheService cacheService,
      final ApiGatewayRequestEvent event) {

    ApiPagination pagination = null;
    Map<String, String> q = getQueryParameterMap(event);

    if (isPaginationNext(q)) {

      pagination = toPaginationToken(cacheService, q.get("next"));

    } else if (isPaginationPrevious(q)) {

      pagination = toPaginationToken(cacheService, q.get("previous"));

      if (pagination.getPrevious() != null) {

        pagination = toPaginationToken(cacheService, pagination.getPrevious());

      } else {
        // if @ start of list, preserve the limit
        int limit = pagination.getLimit();
        pagination = new ApiPagination();
        pagination.setLimit(limit);
      }
    }

    if (pagination != null && pagination.getLimit() < 1) {
      pagination.setLimit(MAX_RESULTS);
    }

    return pagination;
  }

  /**
   * Get Path Parameter.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @return {@link String}
   */
  public static String getPathParameter(final ApiGatewayRequestEvent event, final String key) {
    Map<String, String> q = event.getPathParameters();
    String value = q != null ? q.getOrDefault(key, null) : null;
    return value != null ? value.trim() : null;
  }

  /**
   * Get Query Parameter.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @return {@link String}
   */
  public static String getParameter(final ApiGatewayRequestEvent event, final String key) {
    Map<String, String> q = getQueryParameterMap(event);
    String value = q.getOrDefault(key, null);
    return value != null ? value.trim() : null;
  }

  /**
   * Get Query Parameter Map.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   */
  public static Map<String, String> getQueryParameterMap(final ApiGatewayRequestEvent event) {
    Map<String, String> q =
        event.getQueryStringParameters() != null ? event.getQueryStringParameters()
            : Collections.emptyMap();
    return q;
  }

  /** private constructor. */
  private ApiGatewayRequestEventUtil() {}
}
