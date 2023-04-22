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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link ApiGatewayRequestEvent} helper utilities.
 *
 */
public interface ApiGatewayRequestEventUtil {

  /** The Default maximum results returned. */
  int MAX_RESULTS = 10;

  /** {@link Gson}. */
  Gson GSON = GsonUtil.getInstance();

  /**
   * Create Pagination.
   * 
   * @param cacheService {@link CacheService}
   * @param event {@link ApiGatewayRequestEvent}
   * @param lastPagination {@link ApiPagination}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * 
   * @return {@link ApiPagination}
   */
  default ApiPagination createPagination(final CacheService cacheService,
      final ApiGatewayRequestEvent event, final ApiPagination lastPagination,
      final PaginationMapToken token, final int limit) {

    ApiPagination current = null;
    final Map<String, String> q = getQueryParameterMap(event);

    Gson gson = GsonUtil.getInstance();

    if (isPaginationPrevious(q)) {

      String json = cacheService.read(q.get("previous"));
      current = gson.fromJson(json, ApiPagination.class);

    } else {

      current = new ApiPagination();

      current.setLimit(limit);
      current.setPrevious(lastPagination != null ? lastPagination.getNext() : null);
      current.setStartkey(token);
      current.setHasNext(token != null);

      cacheService.write(current.getNext(), gson.toJson(current), 1);
    }

    return current;
  }

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to {@link DynamicObject}.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link DynamicObject}
   * @throws BadException BadException
   */
  @SuppressWarnings("unchecked")
  default DynamicObject fromBodyToDynamicObject(final LambdaLogger logger,
      final ApiGatewayRequestEvent event) throws BadException {
    return new DynamicObject(fromBodyToObject(logger, event, Map.class));
  }

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to {@link Map}.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   * @throws BadException BadException
   */
  @SuppressWarnings("unchecked")
  default Map<String, Object> fromBodyToMap(final LambdaLogger logger,
      final ApiGatewayRequestEvent event) throws BadException {
    return fromBodyToObject(logger, event, Map.class);
  }

  /**
   * Get the Body from {@link ApiGatewayRequestEvent} and transform to Object.
   *
   * @param <T> Type of {@link Class}
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param classOfT {@link Class}
   * @return T
   * @throws BadException BadException
   */
  default <T> T fromBodyToObject(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final Class<T> classOfT) throws BadException {

    String body = event.getBody();
    if (body == null) {
      throw new BadException("request body is required");
    }

    byte[] data = event.getBody().getBytes(StandardCharsets.UTF_8);

    if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
      data = Base64.getDecoder().decode(body);
    }

    Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8);
    try {
      return GSON.fromJson(reader, classOfT);
    } catch (JsonSyntaxException e) {
      throw new BadException("invalid JSON body");
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        logger.log("Cannot close DocumentItemJSON: " + e.getMessage());
      }
    }
  }

  /**
   * Get {@link ApiGatewayRequestEvent} body as {@link String}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   * @throws BadException BadException
   */
  static String getBodyAsString(final ApiGatewayRequestEvent event) throws BadException {
    String body = event.getBody();
    if (body == null) {
      throw new BadException("request body is required");
    }

    if (Boolean.TRUE.equals(event.getIsBase64Encoded())) {
      byte[] bytes = Base64.getDecoder().decode(body);
      body = new String(bytes, StandardCharsets.UTF_8);
    }

    if (StringUtils.isEmpty(body)) {
      throw new BadException("request body is required");
    }

    return body;
  }

  /**
   * Get the calling Cognito Username.
   *
   * @param event {@link ApiGatewayRequestEvent}.
   * @return {@link String}
   */
  @SuppressWarnings("unchecked")
  static String getCallingCognitoUsername(final ApiGatewayRequestEvent event) {

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
        String u = getCallingCognitoUsernameFromClaims(claims);
        if (u != null) {
          username = u;
        }
      }
    }

    return username;
  }

  private static String getCallingCognitoUsernameFromClaims(final Map<String, Object> claims) {
    String username = null;
    if (claims.containsKey("email")) {
      username = claims.get("email").toString();
    } else if (claims.containsKey("username")) {
      username = claims.get("username").toString();
    } else if (claims.containsKey("cognito:username")) {
      username = claims.get("cognito:username").toString();
    }
    return username;
  }

  /**
   * Get ContentType from {@link ApiGatewayRequestEvent}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   */
  default String getContentType(final ApiGatewayRequestEvent event) {

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
   * Get Limit Parameter.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @return int
   */
  default int getLimit(final LambdaLogger logger, final ApiGatewayRequestEvent event) {

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
   * Find Query Parameter 'next' or 'prev' and convert to {@link ApiPagination}.
   *
   * @param cacheService {@link CacheService}
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link ApiPagination}
   */
  default ApiPagination getPagination(final CacheService cacheService,
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
   * Get Query Parameter.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @return {@link String}
   */
  default String getParameter(final ApiGatewayRequestEvent event, final String key) {
    Map<String, String> q = getQueryParameterMap(event);
    String value = q.getOrDefault(key, null);
    return value != null ? value.trim() : null;
  }

  /**
   * Get Path Parameter.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @return {@link String}
   */
  default String getPathParameter(final ApiGatewayRequestEvent event, final String key) {
    Map<String, String> q = event.getPathParameters();
    String value = q != null ? q.getOrDefault(key, null) : null;
    return value != null ? value.trim() : null;
  }

  /**
   * Get Query Parameter Map.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link Map}
   */
  default Map<String, String> getQueryParameterMap(final ApiGatewayRequestEvent event) {
    Map<String, String> q =
        event.getQueryStringParameters() != null ? event.getQueryStringParameters()
            : Collections.emptyMap();
    return q;
  }

  /**
   * Is String NOT empty.
   * 
   * @param s {@link String}
   * @return boolean
   */
  default boolean isNotBlank(final String s) {
    return s != null && s.length() > 0;
  }

  /**
   * Is has Next Pagination Token.
   * 
   * @param q {@link Map}
   * @return boolean
   */
  default boolean isPaginationNext(final Map<String, String> q) {
    return isNotBlank(q.getOrDefault("next", null));
  }

  /**
   * Is has Previous Pagination Token.
   * 
   * @param q {@link Map}
   * @return boolean
   */
  default boolean isPaginationPrevious(final Map<String, String> q) {
    return isNotBlank(q.getOrDefault("previous", null));
  }

  /**
   * Set Path Parameter.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param key {@link String}
   * @param value {@link String}
   */
  default void setPathParameter(final ApiGatewayRequestEvent event, final String key,
      final String value) {
    Map<String, String> q = new HashMap<>();

    if (event.getPathParameters() != null) {
      q.putAll(event.getPathParameters());
    }

    q.put(key, value);
    event.setPathParameters(q);
  }

  /**
   * Sub list to a max limit.
   * 
   * @param <T> Type
   * @param list {@link List}
   * @param limit int
   * 
   * @return {@link List}
   */
  default <T> List<T> subList(final List<T> list, final int limit) {
    return list.size() > limit ? list.subList(0, limit) : list;
  }

  /**
   * Convert {@link String} to {@link ApiPagination}.
   *
   * @param cacheService {@link CacheService}
   * @param key {@link String}
   * 
   * @return {@link ApiPagination}
   */
  default ApiPagination toPaginationToken(final CacheService cacheService, final String key) {

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
}
