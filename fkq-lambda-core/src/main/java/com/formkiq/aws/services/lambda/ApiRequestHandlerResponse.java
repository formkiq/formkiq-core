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

import com.formkiq.aws.dynamodb.DynamoDbQueryException;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.ConflictException;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.formkiq.aws.services.lambda.exceptions.NotImplementedException;
import com.formkiq.aws.services.lambda.exceptions.TooManyRequestsException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.validation.UnAuthorizedValidationError;
import com.formkiq.validation.ValidationException;

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.MOVED_PERMANENTLY;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_ERROR;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_METHOD_CONFLICT;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_IMPLEMENTED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_TEMPORARY_REDIRECT;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_TOO_MANY_REQUESTS;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * Immutable HTTP‐style response holder.
 */
public record ApiRequestHandlerResponse(int statusCode, Map<String, String> headers, Object body) {

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Convert To {@link Map}.
   * 
   * @return Map
   */
  public Map<String, Object> toMap() {
    Map<String, Object> m = new HashMap<>();
    m.put("statusCode", statusCode);
    if (!headers.isEmpty()) {
      m.put("headers", headers);
    }

    if (body != null) {
      if (body instanceof Map map) {
        if (!map.isEmpty()) {
          m.put("body", GsonUtil.getInstance().toJson(body));
        }
      } else {
        m.put("body", GsonUtil.getInstance().toJson(body));
      }
    }

    return m;
  }

  public static final class Builder {
    /** Status Code. */
    private int statusCode = -1;
    /** Http Headers. */
    private final Map<String, String> headers = new HashMap<>();
    /** Http Body. */
    private final Map<String, Object> body = new HashMap<>();
    /** {@link Object}. */
    private Object object;

    /**
     * Set Status.
     * 
     * @param status {@link ApiResponseStatus}
     * @return Builder
     */
    public Builder status(final ApiResponseStatus status) {
      this.statusCode = status.getStatusCode();
      return this;
    }

    /**
     * Set Status.
     * 
     * @param status int
     * @return Builder
     */
    public Builder status(final int status) {
      this.statusCode = status;
      return this;
    }

    /**
     * Add Header.
     * 
     * @param name {@link String}
     * @param value {@link String}
     * @return Builder
     */
    public Builder header(final String name, final String value) {
      headers.put(name, value);
      return this;
    }

    /**
     * Add Headers.
     *
     * @param map {@link Map}
     * @return Builder
     */
    public Builder header(final Map<String, String> map) {
      headers.putAll(map);
      return this;
    }

    /**
     * HTTP 200 OK.
     * 
     * @return Builder
     */
    public Builder ok() {
      return status(SC_OK.getStatusCode());
    }

    /**
     * HTTP 201 Created.
     * 
     * @return Builder
     */
    public Builder created() {
      return status(SC_CREATED.getStatusCode());
    }

    /**
     * HTTP 400 Bad Request.
     * 
     * @return Builder
     */
    public Builder badRequest() {
      return status(SC_BAD_REQUEST.getStatusCode());
    }

    /**
     * Http Body.
     * 
     * @param key {@link String}
     * @param value {@link Object}
     * @return Builder
     */
    public Builder body(final String key, final Object value) {
      this.body.put(key, value);
      return this;
    }

    /**
     * Http Body.
     * 
     * @param map {@link Map}
     * @return Builder
     */
    public Builder body(final Map<String, Object> map) {
      this.body.putAll(map);
      return this;
    }

    /**
     * Http Body.
     * 
     * @param obj {@link Object}
     * @return Builder
     */
    public Builder body(final Object obj) {
      this.object = obj;
      return this;
    }

    /**
     * Next Token.
     * 
     * @param nextToken {@link String}
     * @return Builder
     */
    public Builder next(final String nextToken) {
      this.body.put("next", nextToken);
      return this;
    }

    /**
     * Build with {@link Exception}.
     *
     * @param logger {@link Logger}
     * @param exception {@link Exception}
     * @return Builder
     */
    public Builder exception(final Logger logger, final Exception exception) {

      this.body.put("message", exception.getMessage());

      if (exception instanceof ConflictException) {
        this.statusCode = SC_METHOD_CONFLICT.getStatusCode();
      } else if (exception instanceof TooManyRequestsException) {
        this.statusCode = SC_TOO_MANY_REQUESTS.getStatusCode();
      } else if (exception instanceof ValidationException e) {

        if (e.errors().stream().anyMatch(ee -> ee instanceof UnAuthorizedValidationError)) {
          this.statusCode = SC_UNAUTHORIZED.getStatusCode();
        } else {
          this.body.remove("message");
          this.statusCode = SC_BAD_REQUEST.getStatusCode();
          this.body.put("errors", e.errors());
        }

      } else if (isBadRequestException(exception)) {
        this.statusCode = SC_BAD_REQUEST.getStatusCode();
      } else if (exception instanceof ForbiddenException e) {
        this.statusCode = SC_UNAUTHORIZED.getStatusCode();
        if (!isEmpty(e.getDebug())) {
          logger.trace(e.getDebug());
        }
      } else if (exception instanceof UnauthorizedException) {
        this.statusCode = SC_UNAUTHORIZED.getStatusCode();
      } else if (exception instanceof NotImplementedException) {
        this.statusCode = SC_NOT_IMPLEMENTED.getStatusCode();
      } else {
        this.statusCode = buildStatus(exception).getStatusCode();
        this.body.put("message", buildErrorMessage(exception));
      }

      return this;
    }

    private String buildErrorMessage(final Exception e) {

      return switch (e.getClass().getName()) {
        case "com.formkiq.aws.dynamodb.DynamoDbQueryException" -> {
          if (e instanceof DynamoDbQueryException ee) {
            yield switch (ee.getError()) {
              case INVALID_START_KEY -> "Invalid Next token";
              default -> "Invalid query";
            };
          }
          yield "Unknown error";
        }
        case "com.formkiq.aws.services.lambda.exceptions.NotFoundException",
            "com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException" ->
          e.getMessage();
        default -> "Internal Server Error";
      };
    }

    private ApiResponseStatus buildStatus(final Exception e) {
      return switch (e.getClass().getName()) {
        case "com.formkiq.aws.dynamodb.DynamoDbQueryException" -> SC_BAD_REQUEST;
        case "com.formkiq.aws.services.lambda.exceptions.NotFoundException",
            "com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException" ->
          SC_NOT_FOUND;
        default -> SC_ERROR;
      };
    }

    /**
     * Is Bad Request Exception (Http Status 400).
     * 
     * @param exception {@link Exception}
     * @return boolean
     */
    private static boolean isBadRequestException(final Exception exception) {
      return exception instanceof BadException || exception instanceof IllegalArgumentException
          || exception instanceof DateTimeException;
    }

    private Map<String, String> createJsonHeaders() {
      return Map.of("Access-Control-Allow-Headers",
          "Content-Type,X-Amz-Date,Authorization,X-Api-Key", "Access-Control-Allow-Methods", "*",
          "Access-Control-Allow-Origin", "*", "Content-Type", "application/json");
    }

    /**
     * Builder {@link ApiRequestHandlerResponse}.
     * 
     * @return ApiRequestHandlerResponse
     */
    public ApiRequestHandlerResponse build() {
      Map<String, String> allHeaders = new HashMap<>(headers);

      if (statusCode != MOVED_PERMANENTLY.getStatusCode()
          && statusCode != SC_TEMPORARY_REDIRECT.getStatusCode()) {
        allHeaders.putAll(createJsonHeaders());
      }

      return new ApiRequestHandlerResponse(statusCode, allHeaders,
          this.object != null ? this.object : this.body);
    }
  }
}
