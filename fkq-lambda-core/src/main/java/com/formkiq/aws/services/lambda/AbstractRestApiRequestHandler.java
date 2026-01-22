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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_ERROR;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.http.HttpAccessLog;
import com.formkiq.aws.services.lambda.http.HttpAccessLogBuilder;
import com.formkiq.aws.sqs.events.SqsEvent;
import com.formkiq.aws.sqs.events.SqsEventRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.plugins.useractivity.UserActivity;
import com.formkiq.plugins.useractivity.UserActivityContext;
import com.formkiq.plugins.useractivity.UserActivityPlugin;
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

  private static void resetThreadLocal() {
    ApiAuthorization.logout();
    UserActivityContext.clear();
  }

  /** {@link Gson}. */
  protected Gson gson = GsonUtil.getInstance();

  private ApiAuthorization buildApiAuthorization(final ApiGatewayRequestEvent event,
      final List<ApiAuthorizationInterceptor> interceptors) throws Exception {

    ApiAuthorization authorization =
        new ApiAuthorizationBuilder().interceptors(interceptors).build(event);

    ApiAuthorization.login(authorization);

    return authorization;
  }

  protected HttpAccessLog buildHttpAccessLog(final ApiAuthorization authorization,
      final ApiGatewayRequestEvent event, final ApiRequestHandlerResponse response,
      final ApiGatewayRequestContext rc, final Map<String, Object> identity,
      final String responseBody, final String userAgent) {

    return new HttpAccessLogBuilder().requestTime(rc.getRequestTime()).requestId(rc.getRequestId())
        .clientIp((String) identity.get("sourceIp"))
        .userId(authorization != null ? authorization.getUsername() : "Unknown")
        .http(event.getHttpMethod(), rc.getProtocol(), event.getResource(),
            event.getPathParameters(), event.getQueryStringParameters())
        .resp(response.statusCode(), responseBody).userAgent(userAgent).build();
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
   * @param apiResponse {@link ApiRequestHandlerResponse}
   * @return ApiRequestHandlerResponse
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse executeResponseInterceptors(
      final List<ApiRequestHandlerInterceptor> requestInterceptors,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final ApiRequestHandlerResponse apiResponse) throws Exception {

    ApiRequestHandlerResponse response = apiResponse;

    for (ApiRequestHandlerInterceptor interceptor : requestInterceptors) {
      response = interceptor.afterProcessRequest(event, authorization, response);
    }

    return response;
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

    Logger logger = awsservice.getLogger();
    if (logger.isLogged(LogLevel.DEBUG)) {
      ApiGatewayRequestEvent event = this.gson.fromJson(str, ApiGatewayRequestEvent.class);
      if (event != null && event.getHeaders() != null) {
        event.getHeaders().put("authorization", "****");
        logger.debug(gson.toJson(event));
      }
    }

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
        SqsEvent records = this.gson.fromJson(str, SqsEvent.class);
        for (SqsEventRecord record : records.records()) {
          if ("aws:sqs".equals(record.eventSource())) {
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
   * @param sqsEventRecord {@link SqsEventRecord}
   * @throws IOException IOException
   */
  public abstract void handleSqsRequest(Logger logger, AwsServiceCache awsServices,
      SqsEventRecord sqsEventRecord) throws IOException;

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

    Optional<Boolean> hasAccess =
        handler.isAuthorized(getAwsServices(), method, event, authorization);

    hasAccess = isAuthorizedHandler(event, authorization, hasAccess);

    String siteId = authorization.getSiteId();
    if (siteId != null) {
      Collection<ApiPermission> permissions = authorization.getPermissions(siteId);

      if (hasAccess.isEmpty()) {
        hasAccess = switch (method) {
          case "head", "get" -> checkPermission(ApiPermission.READ, permissions);
          case "post", "patch", "put" -> checkPermission(ApiPermission.WRITE, permissions);
          case "delete" -> checkPermission(ApiPermission.DELETE, permissions);
          default -> Optional.empty();
        };
      }
    }

    hasAccess =
        authorization.getAllPermissions().contains(ApiPermission.ADMIN) ? Optional.of(Boolean.TRUE)
            : hasAccess;
    return hasAccess.orElse(Boolean.FALSE);
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

  private void log(final ApiAuthorization authorization, final ApiGatewayRequestEvent event,
      final ApiRequestHandlerResponse response, final Exception e) {

    Logger logger = getAwsServices().getLogger();

    if (event != null) {
      ApiGatewayRequestContext rc = event.getRequestContext() != null ? event.getRequestContext()
          : new ApiGatewayRequestContext();

      String userAgent = event.getHeaderValue("User-Agent");
      Map<String, Object> identity = rc.getIdentity() != null ? rc.getIdentity() : Map.of();

      String responseBody = null;
      if (response.statusCode() >= ApiResponseStatus.SC_BAD_REQUEST.getStatusCode()
          && response.body() instanceof Map m) {
        if (m.containsKey("message")) {
          responseBody = (String) m.get("message");
        }
      }

      HttpAccessLog accessLog =
          buildHttpAccessLog(authorization, event, response, rc, identity, responseBody, userAgent);

      logger.info(gson.toJson(accessLog));

    } else {
      logger.error("{\"requestId\": \"invalid\"}");
    }

    if (e != null && SC_ERROR.getStatusCode() == response.statusCode()) {
      logger.error(e);
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

    Collection<UserActivity.Builder> ua = null;
    ApiAuthorization authorization = null;
    ApiRequestHandlerResponse response = null;
    Exception exception = null;

    try {

      resetThreadLocal();

      List<ApiAuthorizationInterceptor> interceptors =
          setupApiAuthorizationInterceptor(awsServices);

      authorization = buildApiAuthorization(event, interceptors);

      List<ApiRequestHandlerInterceptor> requestInterceptors =
          getApiRequestHandlerInterceptors(awsServices);

      executeRequestInterceptors(requestInterceptors, event, authorization);

      response = processRequest(getUrlMap(), event, authorization);

      response = executeResponseInterceptors(requestInterceptors, event, authorization, response);

      ua = new ApiGatewayRequestToUserActivityFunction().apply(authorization, event, response);
      writeJson(output, response.toMap());
      writeUserActivity(awsServices, authorization, ua);

    } catch (Exception e) {

      exception = e;
      response = ApiRequestHandlerResponse.builder().exception(logger, e).build();

      if (ua == null) {
        ua = new ApiGatewayRequestToUserActivityFunction().apply(authorization, event, null);
      }

      for (var a : ua) {
        a.status(response.statusCode()).message(e.getMessage());
      }

      writeJson(output, response.toMap());
      writeUserActivity(awsServices, authorization, ua);

    } finally {
      log(authorization, event, response, exception);
      resetThreadLocal();
    }
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

  /**
   * Write JSON Response {@link OutputStream}.
   *
   * @param output {@link OutputStream}
   * @param response {@link Map}
   * @throws IOException IOException
   */
  protected void writeJson(final OutputStream output, final Map<String, Object> response)
      throws IOException {

    String json = this.gson.toJson(response);

    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);

    if (isResponseTooLarge(json)) {

      response.put("body", this.gson.toJson(Map.of("message", "Response exceeds allowed size")));
      response.put("statusCode", SC_BAD_REQUEST.getStatusCode());
      json = this.gson.toJson(response);
    }

    writer.write(json);

    writer.close();
  }

  private void writeUserActivity(final AwsServiceCache awsServices,
      final ApiAuthorization authorization, final Collection<UserActivity.Builder> ua) {

    if (awsServices.containsExtension(UserActivityPlugin.class)) {
      String siteId = authorization != null ? authorization.getSiteId() : DEFAULT_SITE_ID;

      UserActivityPlugin plugin = awsServices.getExtension(UserActivityPlugin.class);
      plugin.addUserActivity(ua.stream().map(a -> a.build(siteId)).toList());
    }
  }
}
