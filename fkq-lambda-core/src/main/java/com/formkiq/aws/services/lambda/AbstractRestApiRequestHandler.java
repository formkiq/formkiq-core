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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_ERROR;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_METHOD_CONFLICT;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_IMPLEMENTED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_TOO_MANY_REQUESTS;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.ConflictException;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotImplementedException;
import com.formkiq.aws.services.lambda.exceptions.TooManyRequestsException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * Rest API Request Handler for {@link RequestStreamHandler}.
 *
 */
public abstract class AbstractRestApiRequestHandler implements RequestStreamHandler {

  /** Define the size limit in bytes (6 MB = 6 * 1024 * 1024 bytes). */
  private static final long MAX_PAYLOAD_SIZE_MB = 6L * 1024 * 1024;

  /** {@link Gson}. */
  protected Gson gson = GsonUtil.getInstance();

  private void buildForbiddenException(final AwsServiceCache awsServices, final OutputStream output,
      final ForbiddenException e) throws IOException {

    awsServices.getLogger().debug(e.getDebug());

    buildResponse(awsServices, output, SC_UNAUTHORIZED, Collections.emptyMap(),
        new ApiResponseError(e.getMessage()));
  }

  /**
   * Handle Exception.
   *
   * @param awsServices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @param status {@link ApiResponseStatus}
   * @param headers {@link Map}
   * @param apiResponse {@link ApiResponse}
   * @throws IOException IOException
   */
  protected void buildResponse(final AwsServiceCache awsServices, final OutputStream output,
      final ApiResponseStatus status, final Map<String, String> headers,
      final ApiResponse apiResponse) throws IOException {

    Map<String, Object> response = new HashMap<>();
    Map<String, String> jsonheaders = createJsonHeaders();
    response.put("statusCode", status.getStatusCode());

    if (apiResponse instanceof ApiRedirectResponse a) {
      jsonheaders.put("Location", a.getRedirectUri());
    } else if (status.getStatusCode() == SC_FOUND.getStatusCode()
        && apiResponse instanceof ApiMessageResponse a) {
      jsonheaders.put("Location", a.getMessage());
    } else if (apiResponse instanceof ApiMapResponse a) {
      response.put("body", this.gson.toJson(a.getMap()));
      jsonheaders.putAll(headers);
    } else if (apiResponse instanceof ApiObjectResponse a) {
      response.put("body", this.gson.toJson(a.getObject()));
      jsonheaders.putAll(headers);
    } else {
      response.put("body", this.gson.toJson(apiResponse));
      jsonheaders.putAll(headers);
    }

    response.put("headers", jsonheaders);

    writeJson(awsServices, output, response);
  }

  /**
   * Call Handler Rest Method.
   *
   * @param method {@link String}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization ApiAuthorization
   * @param handler {@link ApiGatewayRequestHandler}
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse callHandlerMethod(final String method,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final ApiGatewayRequestHandler handler) throws Exception {

    ApiRequestHandlerResponse response = null;
    AwsServiceCache awsServices = getAwsServices();

    switch (method) {
      case "get":
        handler.beforeGet(event, authorization, awsServices);
        response = handler.get(event, authorization, awsServices);
        break;

      case "delete":
        handler.beforeDelete(event, authorization, awsServices);
        response = handler.delete(event, authorization, awsServices);
        break;

      case "head":
        handler.beforeHead(event, authorization, awsServices);
        response = handler.head(event, authorization, awsServices);
        break;

      case "options":
        response = handler.options(event, authorization, awsServices);
        break;

      case "patch":
        handler.beforePatch(event, authorization, awsServices);
        response = handler.patch(event, authorization, awsServices);
        break;

      case "post":
        handler.beforePost(event, authorization, awsServices);
        response = handler.post(event, authorization, awsServices);
        break;

      case "put":
        handler.beforePut(event, authorization, awsServices);
        response = handler.put(event, authorization, awsServices);
        break;
      default:
        break;
    }

    return response;
  }

  /**
   * Check For {@link ApiPermission}.
   * 
   * @param permission {@link ApiPermission}
   * @param permissions {@link Collection} {@link ApiPermission}
   * @return {@link Optional} {@link Boolean}
   */
  private Optional<Boolean> checkPermission(final ApiPermission permission,
      final Collection<ApiPermission> permissions) {
    return permissions.contains(permission) ? Optional.of(Boolean.TRUE) : Optional.empty();
  }

  /**
   * Create Response Headers.
   *
   * @return {@link Map} {@link String}
   */
  protected Map<String, String> createJsonHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key");
    headers.put("Access-Control-Allow-Methods", "*");
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Content-Type", "application/json");
    return headers;
  }

  /**
   * Execute Before Request Interceptor.
   * 
   * @param requestInterceptors {@link List} {@link ApiRequestHandlerInterceptor}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @throws Exception Exception
   */
  private void executeRequestInterceptors(
      final List<ApiRequestHandlerInterceptor> requestInterceptors,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization) throws Exception {

    for (ApiRequestHandlerInterceptor interceptor : requestInterceptors) {
      interceptor.beforeProcessRequest(event, authorization);
    }
  }

  /**
   * Execute {@link ApiRequestHandlerInterceptor}.
   * 
   * @param requestInterceptors {@link ApiRequestHandlerInterceptor}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param object {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private void executeResponseInterceptors(
      final List<ApiRequestHandlerInterceptor> requestInterceptors,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final ApiRequestHandlerResponse object) throws Exception {

    for (ApiRequestHandlerInterceptor interceptor : requestInterceptors) {
      interceptor.afterProcessRequest(event, authorization, object);
    }
  }

  /**
   * Find Request Handler.
   * 
   * @param urlMap {@link Map}
   * @param method {@link String}
   * @param resource {@link String}
   * @return {@link ApiGatewayRequestHandler}
   * @throws NotFoundException Handler not found
   */
  public ApiGatewayRequestHandler findRequestHandler(
      final Map<String, ApiGatewayRequestHandler> urlMap, final String method,
      final String resource) throws NotFoundException {
    String s = "options".equals(method) ? method : resource;
    ApiGatewayRequestHandler hander = urlMap.get(s);
    if (hander != null) {
      return hander;
    }

    throw new NotFoundException(resource + " request handler not found");
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   *
   * @param str {@link String}
   * @param awsservice {@link AwsServiceCache}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getApiGatewayEvent(final String str,
      final AwsServiceCache awsservice) {

    awsservice.getLogger().debug(str);

    return this.gson.fromJson(str, ApiGatewayRequestEvent.class);
  }

  private List<ApiRequestHandlerInterceptor> getApiRequestHandlerInterceptors(
      final AwsServiceCache awsServices) {
    return awsServices.getExtensions(ApiRequestHandlerInterceptor.class);
  }

  /**
   * Get {@link AwsServiceCache}.
   *
   * @return {@link AwsServiceCache}
   */
  public abstract AwsServiceCache getAwsServices();

  /**
   * Get URL Map.
   * 
   * @return {@link Map}
   */
  public abstract Map<String, ApiGatewayRequestHandler> getUrlMap();

  /**
   * Final Request Handler.
   * 
   * @param requestContext {@link Context}
   * @param input {@link String}
   */
  public void handleOtherRequest(final Context requestContext, final String input) {
    // empty
  }

  @Override
  public void handleRequest(final InputStream input, final OutputStream output,
      final Context context) throws IOException {

    AwsServiceCache awsServices = getAwsServices();

    String str = IoUtils.toUtf8String(input);

    ApiGatewayRequestEvent event = getApiGatewayEvent(str, awsServices);
    Logger logger = awsServices.getLogger();

    if (!isEmpty(event)) {

      processApiGatewayRequest(logger, event, awsServices, output);

    } else {

      if (str.contains("aws:sqs")) {
        LambdaInputRecords records = this.gson.fromJson(str, LambdaInputRecords.class);
        for (LambdaInputRecord record : records.getRecords()) {
          if ("aws:sqs".equals(record.getEventSource())) {
            handleSqsRequest(logger, awsServices, record);
          }
        }

      } else {

        handleOtherRequest(context, str);
      }
    }
  }

  /**
   * Handler for Sqs Requests.
   * 
   * @param logger {@link Logger}
   * @param awsServices {@link AwsServiceCache}
   * @param record {@link LambdaInputRecord}
   * @throws IOException IOException
   */
  public abstract void handleSqsRequest(Logger logger, AwsServiceCache awsServices,
      LambdaInputRecord record) throws IOException;

  /**
   * Whether {@link ApiGatewayRequestEvent} has access.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @param method {@link String}
   * @param authorization {@link ApiAuthorization}
   * @param handler {@link ApiGatewayRequestHandler}
   * @return boolean
   * @throws Exception Exception
   */
  private boolean isAuthorized(final ApiGatewayRequestEvent event, final String method,
      final ApiAuthorization authorization, final ApiGatewayRequestHandler handler)
      throws Exception {

    Collection<ApiPermission> permissions = authorization.getPermissions();

    Optional<Boolean> hasAccess =
        handler.isAuthorized(getAwsServices(), method, event, authorization);

    hasAccess = isAuthorizedHandler(event, authorization, hasAccess);

    if (hasAccess.isEmpty()) {
      hasAccess = switch (method) {
        case "head", "get" -> checkPermission(ApiPermission.READ, permissions);
        case "post", "patch", "put" -> checkPermission(ApiPermission.WRITE, permissions);
        case "delete" -> checkPermission(ApiPermission.DELETE, permissions);
        default -> Optional.empty();
      };
    }

    hasAccess = permissions.contains(ApiPermission.ADMIN) ? Optional.of(Boolean.TRUE) : hasAccess;

    return hasAccess.orElse(Boolean.FALSE);
  }

  /**
   * Is caller Authorized to continue.
   *
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param method {@link String}
   * @param handler {@link ApiGatewayRequestHandler}
   * @return boolean
   * @throws Exception Exception
   */
  private boolean isAuthorized(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final String method,
      final ApiGatewayRequestHandler handler) throws Exception {
    return "options".equals(method) || isAuthorized(event, method, authorization, handler);
  }

  private Optional<Boolean> isAuthorizedHandler(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final Optional<Boolean> hasAccess) {

    AuthorizationHandler authorizationHandler =
        getAwsServices().getExtensionOrNull(AuthorizationHandler.class);

    Optional<Boolean> isAuthorized = hasAccess;

    if (authorizationHandler != null) {
      Optional<Boolean> ah =
          authorizationHandler.isAuthorized(getAwsServices(), event, authorization);
      if (ah.isPresent()) {
        isAuthorized = ah;
      }
    }
    return isAuthorized;
  }

  /**
   * Is {@link ApiGatewayRequestEvent} empty.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return boolean
   */
  private boolean isEmpty(final ApiGatewayRequestEvent event) {
    return event != null && event.getHeaders() == null && event.getPath() == null;
  }

  private void log(final ApiGatewayRequestEvent event, final ApiAuthorization authorization) {

    Logger logger = getAwsServices().getLogger();

    if (event != null) {
      ApiGatewayRequestContext requestContext =
          event.getRequestContext() != null ? event.getRequestContext()
              : new ApiGatewayRequestContext();

      Map<String, Object> identity =
          requestContext.getIdentity() != null ? requestContext.getIdentity() : Map.of();

      String s = String.format(
          "{\"requestId\": \"%s\",\"ip\": \"%s\",\"requestTime\": \"%s\",\"httpMethod\": \"%s\","
              + "\"routeKey\": \"%s\",\"pathParameters\": %s,"
              + "\"protocol\": \"%s\",\"user\":\"%s\",\"queryParameters\":%s}",
          requestContext.getRequestId(), identity.get("sourceIp"), requestContext.getRequestTime(),
          event.getHttpMethod(), event.getHttpMethod() + " " + event.getResource(),
          "{" + toStringFromMap(event.getPathParameters()) + "}", requestContext.getProtocol(),
          authorization.getUsername(),
          "{" + toStringFromMap(event.getQueryStringParameters()) + "}");

      logger.info(s);

    } else {
      logger.error("{\"requestId\": \"invalid\"}");
    }
  }

  /**
   * Processes API Gateway Requests.
   * 
   * @param logger {@link Logger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsServices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @throws IOException IOException
   */
  private void processApiGatewayRequest(final Logger logger, final ApiGatewayRequestEvent event,
      final AwsServiceCache awsServices, final OutputStream output) throws IOException {

    try {

      List<ApiAuthorizationInterceptor> interceptors =
          setupApiAuthorizationInterceptor(awsServices);

      ApiAuthorization authorization = buildApiAuthorization(event, interceptors);

      List<ApiRequestHandlerInterceptor> requestInterceptors =
          getApiRequestHandlerInterceptors(awsServices);

      executeRequestInterceptors(requestInterceptors, event, authorization);

      ApiRequestHandlerResponse object = processRequest(getUrlMap(), event, authorization);

      executeResponseInterceptors(requestInterceptors, event, authorization, object);

      buildResponse(awsServices, output, object.getStatus(), object.getHeaders(),
          object.getResponse());

    } catch (NotFoundException e) {
      buildResponse(awsServices, output, SC_NOT_FOUND, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ConflictException e) {
      buildResponse(awsServices, output, SC_METHOD_CONFLICT, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (TooManyRequestsException e) {
      buildResponse(awsServices, output, SC_TOO_MANY_REQUESTS, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (BadException | IllegalArgumentException | DateTimeException e) {
      buildResponse(awsServices, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ForbiddenException e) {
      buildForbiddenException(awsServices, output, e);
    } catch (UnauthorizedException e) {
      buildResponse(awsServices, output, SC_UNAUTHORIZED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (NotImplementedException e) {
      buildResponse(awsServices, output, SC_NOT_IMPLEMENTED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ValidationException e) {
      buildResponse(awsServices, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.errors()));
    } catch (Exception e) {
      logger.error(e);

      buildResponse(awsServices, output, SC_ERROR, Collections.emptyMap(),
          new ApiResponseError("Internal Server Error"));

    } finally {
      ApiAuthorization.logout();
    }
  }

  private ApiAuthorization buildApiAuthorization(final ApiGatewayRequestEvent event,
      final List<ApiAuthorizationInterceptor> interceptors) throws Exception {

    ApiAuthorization.logout();

    ApiAuthorization authorization =
        new ApiAuthorizationBuilder().interceptors(interceptors).build(event);

    log(event, authorization);
    ApiAuthorization.login(authorization);

    return authorization;
  }

  /**
   * Process {@link ApiGatewayRequestEvent}.
   *
   * @param urlMap {@link Map}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse processRequest(
      final Map<String, ApiGatewayRequestHandler> urlMap, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws Exception {

    if (event == null || event.getHttpMethod() == null) {
      throw new NotFoundException("Invalid Request");
    }

    String method = event.getHttpMethod().toLowerCase();
    String resource = event.getResource();
    ApiGatewayRequestHandler handler = findRequestHandler(urlMap, method, resource);

    if (!isAuthorized(event, authorization, method, handler)) {
      String s = String.format("fkq access denied (%s)", authorization.getAccessSummary());
      throw new ForbiddenException(s);
    }

    return callHandlerMethod(method, event, authorization, handler);
  }

  private List<ApiAuthorizationInterceptor> setupApiAuthorizationInterceptor(
      final AwsServiceCache awsServices) {
    return awsServices.getExtensions(ApiAuthorizationInterceptor.class);
  }

  private String toStringFromMap(final Map<String, String> map) {
    return map != null
        ? map.entrySet().stream().map(e -> String.format("\"%s\":\"%s\"", e.getKey(), e.getValue()))
            .collect(Collectors.joining(","))
        : "";
  }

  /**
   * Write JSON Response {@link OutputStream}.
   *
   * @param awsservices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @param response {@link Map}
   * @throws IOException IOException
   */
  protected void writeJson(final AwsServiceCache awsservices, final OutputStream output,
      final Map<String, Object> response) throws IOException {

    String json = this.gson.toJson(response);

    Logger logger = awsservices.getLogger();

    if (logger.isLogged(LogLevel.DEBUG)) {
      logger.debug(this.gson.toJson(Map.of("response", response)));
    }

    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

    if (isResponseTooLarge(json)) {

      response.put("body", this.gson.toJson(Map.of("message", "Response exceeds allowed size")));
      response.put("statusCode", SC_BAD_REQUEST.getStatusCode());
      json = this.gson.toJson(response);
    }

    writer.write(json);

    writer.close();
  }

  /**
   * Determines if the size of the given string exceeds 6 MB.
   *
   * @param input The string to check.
   * @return true if the string size is greater than 6 MB, false otherwise.
   */
  private boolean isResponseTooLarge(final String input) {
    long sizeInBytes = input.getBytes(StandardCharsets.UTF_8).length;
    return sizeInBytes > MAX_PAYLOAD_SIZE_MB;
  }
}
