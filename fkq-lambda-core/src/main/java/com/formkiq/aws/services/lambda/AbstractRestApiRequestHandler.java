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
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_FORBIDDEN;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_NOT_IMPLEMENTED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_TOO_MANY_REQUESTS;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.ForbiddenException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.services.lambda.exceptions.NotImplementedException;
import com.formkiq.aws.services.lambda.exceptions.TooManyRequestsException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.sqs.SqsService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.validation.ValidationException;
import com.google.gson.Gson;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * Rest API Request Handler for {@link RequestStreamHandler}.
 *
 */
public abstract class AbstractRestApiRequestHandler implements RequestStreamHandler {

  /** {@link Gson}. */
  protected Gson gson = GsonUtil.getInstance();

  /**
   * Handle Exception.
   *
   * @param logger {@link LambdaLogger}
   * @param awsServices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @param status {@link ApiResponseStatus}
   * @param headers {@link Map}
   * @param apiResponse {@link ApiResponse}
   * @throws IOException IOException
   */
  protected void buildResponse(final LambdaLogger logger, final AwsServiceCache awsServices,
      final OutputStream output, final ApiResponseStatus status, final Map<String, String> headers,
      final ApiResponse apiResponse) throws IOException {

    Map<String, Object> response = new HashMap<>();
    Map<String, String> jsonheaders = createJsonHeaders();
    response.put("statusCode", Integer.valueOf(status.getStatusCode()));

    if (apiResponse instanceof ApiRedirectResponse) {
      jsonheaders.put("Location", ((ApiRedirectResponse) apiResponse).getRedirectUri());
    } else if (status.getStatusCode() == SC_FOUND.getStatusCode()
        && apiResponse instanceof ApiMessageResponse) {
      jsonheaders.put("Location", ((ApiMessageResponse) apiResponse).getMessage());
    } else if (apiResponse instanceof ApiMapResponse) {
      response.put("body", this.gson.toJson(((ApiMapResponse) apiResponse).getMap()));
      jsonheaders.putAll(headers);
    } else {
      response.put("body", this.gson.toJson(apiResponse));
      jsonheaders.putAll(headers);
    }

    response.put("headers", jsonheaders);

    writeJson(logger, awsServices, output, response);
  }

  /**
   * Call Handler Rest Method.
   * 
   * @param logger {@link LambdaLogger}
   * @param method {@link String}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization ApiAuthorization
   * @param handler {@link ApiGatewayRequestHandler}
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse callHandlerMethod(final LambdaLogger logger,
      final String method, final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final ApiGatewayRequestHandler handler) throws Exception {

    ApiRequestHandlerResponse response = null;
    AwsServiceCache awsServices = getAwsServices();

    switch (method) {
      case "get":
        handler.beforeGet(logger, event, authorization, awsServices);
        response = handler.get(logger, event, authorization, awsServices);
        break;

      case "delete":
        handler.beforeDelete(logger, event, authorization, awsServices);
        response = handler.delete(logger, event, authorization, awsServices);
        break;

      case "head":
        handler.beforeHead(logger, event, authorization, awsServices);
        response = handler.head(logger, event, authorization, awsServices);
        break;

      case "options":
        response = handler.options(logger, event, authorization, awsServices);
        break;

      case "patch":
        handler.beforePatch(logger, event, authorization, awsServices);
        response = handler.patch(logger, event, authorization, awsServices);
        break;

      case "post":
        handler.beforePost(logger, event, authorization, awsServices);
        response = handler.post(logger, event, authorization, awsServices);
        break;

      case "put":
        handler.beforePut(logger, event, authorization, awsServices);
        response = handler.put(logger, event, authorization, awsServices);
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
   * Execute {@link ApiRequestHandlerResponseInterceptor}.
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
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @return {@link ApiGatewayRequestEvent}
   * @throws IOException IOException
   */
  private ApiGatewayRequestEvent getApiGatewayEvent(final String str, final LambdaLogger logger,
      final AwsServiceCache awsservice) throws IOException {

    if (awsservice.debug()) {
      logger.log(str);
    }

    ApiGatewayRequestEvent event = this.gson.fromJson(str, ApiGatewayRequestEvent.class);
    return event;
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
   * Get {@link ApiGatewayRequestEvent} body as {@link String}.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return {@link String}
   * @throws BadException BadException
   */
  private String getBodyAsString(final ApiGatewayRequestEvent event) throws BadException {
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

    LambdaLogger logger = context.getLogger();

    AwsServiceCache awsServices = getAwsServices();

    String str = IoUtils.toUtf8String(input);

    ApiGatewayRequestEvent event = getApiGatewayEvent(str, logger, awsServices);

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
   * @param logger {@link LambdaLogger}
   * @param awsServices {@link AwsServiceCache}
   * @param record {@link LambdaInputRecord}
   * @throws IOException IOException
   */
  public abstract void handleSqsRequest(LambdaLogger logger, AwsServiceCache awsServices,
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

    hasAccess = isHandlerSiteIdRequired(authorization, handler, hasAccess);

    if (hasAccess.isEmpty()) {
      switch (method) {
        case "head":
        case "get":
          hasAccess = checkPermission(ApiPermission.READ, permissions);
          break;

        case "post":
        case "patch":
        case "put":
          hasAccess = checkPermission(ApiPermission.WRITE, permissions);
          break;

        case "delete":
          hasAccess = checkPermission(ApiPermission.DELETE, permissions);
          break;
        default:
          hasAccess = Optional.empty();
          break;
      }
    }

    hasAccess = permissions.contains(ApiPermission.ADMIN) ? Optional.of(Boolean.TRUE) : hasAccess;

    return hasAccess.orElse(Boolean.FALSE).booleanValue();
  }

  /**
   * Is caller Authorized to continue.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * @param method {@link String}
   * @param handler {@link ApiGatewayRequestHandler}
   * @return boolean
   * @throws Exception Exception
   */
  private boolean isAuthorized(final AwsServiceCache awsServiceCache,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization, final String method,
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
      if (!ah.isEmpty()) {
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
   * Does {@link ApiGatewayRequestHandler} requires a SiteId parameter.
   * 
   * @param authorization {@link ApiAuthorization}
   * @param handler {@link ApiGatewayRequestHandler}
   * @param hasAccess {@link Boolean}
   * @return {@link Optional}
   */
  private Optional<Boolean> isHandlerSiteIdRequired(final ApiAuthorization authorization,
      final ApiGatewayRequestHandler handler, final Optional<Boolean> hasAccess) {
    Optional<Boolean> result = hasAccess;
    if (hasAccess.isEmpty() && authorization.getSiteIds().size() > 1
        && !handler.isSiteIdRequired()) {
      result = Optional.of(Boolean.TRUE);
    }
    return result;
  }

  private void log(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) {

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

      logger.log(s);
    } else {
      logger.log("{\"requestId\": \"invalid\"");
    }
  }

  /**
   * Log Exception.
   * 
   * @param logger {@link LambdaLogger}
   * @param e {@link Exception}
   */
  private void logError(final LambdaLogger logger, final Exception e) {
    e.printStackTrace();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    logger.log(sw.toString());
  }

  /**
   * Processes API Gateway Requests.
   * 
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param awsServices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @throws IOException IOException
   */
  private void processApiGatewayRequest(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final AwsServiceCache awsServices,
      final OutputStream output) throws IOException {

    try {

      List<ApiAuthorizationInterceptor> interceptors =
          setupApiAuthorizationInterceptor(awsServices);

      ApiAuthorization authorization =
          new ApiAuthorizationBuilder().interceptors(interceptors).build(event);
      log(logger, event, authorization);

      List<ApiRequestHandlerInterceptor> requestInterceptors =
          getApiRequestHandlerInterceptors(awsServices);

      executeRequestInterceptors(requestInterceptors, event, authorization);

      ApiRequestHandlerResponse object = processRequest(logger, getUrlMap(), event, authorization);

      executeResponseInterceptors(requestInterceptors, event, authorization, object);

      sendWebNotify(authorization, event, object);

      buildResponse(logger, awsServices, output, object.getStatus(), object.getHeaders(),
          object.getResponse());

    } catch (NotFoundException e) {
      buildResponse(logger, awsServices, output, SC_NOT_FOUND, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (TooManyRequestsException e) {
      buildResponse(logger, awsServices, output, SC_TOO_MANY_REQUESTS, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (BadException | IllegalArgumentException | DateTimeException e) {
      buildResponse(logger, awsServices, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ForbiddenException e) {
      buildResponse(logger, awsServices, output, SC_FORBIDDEN, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (UnauthorizedException e) {
      buildResponse(logger, awsServices, output, SC_UNAUTHORIZED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (NotImplementedException e) {
      buildResponse(logger, awsServices, output, SC_NOT_IMPLEMENTED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ValidationException e) {
      buildResponse(logger, awsServices, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.errors()));
    } catch (Exception e) {
      logError(logger, e);

      buildResponse(logger, awsServices, output, SC_ERROR, Collections.emptyMap(),
          new ApiResponseError("Internal Server Error"));
    }
  }

  /**
   * Process {@link ApiGatewayRequestEvent}.
   *
   * @param logger {@link LambdaLogger}
   * @param urlMap {@link Map}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorization {@link ApiAuthorization}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse processRequest(final LambdaLogger logger,
      final Map<String, ApiGatewayRequestHandler> urlMap, final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization) throws Exception {

    if (event == null || event.getHttpMethod() == null) {
      throw new NotFoundException("Invalid Request");
    }

    String method = event.getHttpMethod().toLowerCase();
    String resource = event.getResource();
    ApiGatewayRequestHandler handler = findRequestHandler(urlMap, method, resource);

    if (!isAuthorized(getAwsServices(), event, authorization, method, handler)) {
      String s = String.format("fkq access denied (%s)", authorization.getAccessSummary());
      if (authorization.getSiteId() == null && authorization.getSiteIds().size() > 1) {
        s = String.format("'siteId' parameter required - multiple siteIds found (%s)",
            authorization.getAccessSummary());
      }

      throw new ForbiddenException(s);
    }

    return callHandlerMethod(logger, method, event, authorization, handler);
  }

  /**
   * Processes the Response.
   * 
   * @param authorization {@link ApiAuthorization}
   * @param event {@link ApiGatewayRequestEvent}
   * @param resp {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   */
  private void sendWebNotify(final ApiAuthorization authorization,
      final ApiGatewayRequestEvent event, final ApiRequestHandlerResponse resp)
      throws BadException {

    String websocket = event.getQueryStringParameter("ws");

    if ("true".equals(websocket)) {

      AwsServiceCache aws = getAwsServices();
      switch (resp.getStatus()) {
        case SC_OK:
        case SC_CREATED:
        case SC_ACCEPTED:
          String siteId = authorization.getSiteId();
          String body = getBodyAsString(event);
          String documentId = event.getPathParameters().get("documentId");

          if (documentId != null) {

            Map<String, String> m = new HashMap<>();
            m.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
            m.put("documentId", documentId);
            m.put("message", body);

            String json = this.gson.toJson(m);
            SqsService sqsService = aws.getExtension(SqsService.class);
            sqsService.sendMessage(aws.environment("WEBSOCKET_SQS_URL"), json);
          }
          break;

        default:
          break;
      }
    }
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
   * @param logger {@link LambdaLogger}
   * @param awsservices {@link AwsServiceCache}
   * @param output {@link OutputStream}
   * @param response {@link Object}
   * @throws IOException IOException
   */
  protected void writeJson(final LambdaLogger logger, final AwsServiceCache awsservices,
      final OutputStream output, final Object response) throws IOException {

    String json = this.gson.toJson(response);

    if (awsservices.debug()) {
      logger.log("response: " + json);
    }

    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
    writer.write(json);
    writer.close();
  }
}
