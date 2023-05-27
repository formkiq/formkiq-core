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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

  /** {@link ApiAuthorizerType}. */
  private static ApiAuthorizerType authorizerType = ApiAuthorizerType.COGNITO;

  /**
   * Set AuthorizerType.
   * 
   * @param type {@link ApiAuthorizerType}
   */
  protected static void setAuthorizerType(final ApiAuthorizerType type) {
    authorizerType = type;
  }

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
   * @param authorizer {@link ApiAuthorizer}
   * @param handler {@link ApiGatewayRequestHandler}
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse callHandlerMethod(final LambdaLogger logger,
      final String method, final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final ApiGatewayRequestHandler handler) throws Exception {

    ApiRequestHandlerResponse response = null;
    AwsServiceCache awsServices = getAwsServices();

    switch (method) {
      case "get":
        handler.beforeGet(logger, event, authorizer, awsServices);
        response = handler.get(logger, event, authorizer, awsServices);
        break;

      case "delete":
        handler.beforeDelete(logger, event, authorizer, awsServices);
        response = handler.delete(logger, event, authorizer, awsServices);
        break;

      case "head":
        handler.beforeHead(logger, event, authorizer, awsServices);
        response = handler.head(logger, event, authorizer, awsServices);
        break;

      case "options":
        response = handler.options(logger, event, authorizer, awsServices);
        break;

      case "patch":
        handler.beforePatch(logger, event, authorizer, awsServices);
        response = handler.patch(logger, event, authorizer, awsServices);
        break;

      case "post":
        handler.beforePost(logger, event, authorizer, awsServices);
        response = handler.post(logger, event, authorizer, awsServices);
        break;

      case "put":
        handler.beforePut(logger, event, authorizer, awsServices);
        response = handler.put(logger, event, authorizer, awsServices);
        break;
      default:
        break;
    }

    return response;
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
        ApiGatewayRequestEventUtil.getCallingCognitoUsername(event),
        "{" + toStringFromMap(event.getQueryStringParameters()) + "}");

    logger.log(s);

    return event;
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
      LambdaInputRecords records = this.gson.fromJson(str, LambdaInputRecords.class);
      for (LambdaInputRecord record : records.getRecords()) {
        if ("aws:sqs".equals(record.getEventSource())) {
          handleSqsRequest(logger, awsServices, record);
        }
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
   * Is {@link ApiGatewayRequestEvent} empty.
   * 
   * @param event {@link ApiGatewayRequestEvent}
   * @return boolean
   */
  private boolean isEmpty(final ApiGatewayRequestEvent event) {
    return event != null && event.getHeaders() == null && event.getPath() == null;
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

    ApiAuthorizer authorizer = new ApiAuthorizer(event, authorizerType);

    try {

      ApiRequestHandlerResponse object = processRequest(logger, getUrlMap(), event, authorizer);
      processResponse(authorizer, event, object);
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
   * @param authorizer {@link ApiAuthorizer}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse processRequest(final LambdaLogger logger,
      final Map<String, ApiGatewayRequestHandler> urlMap, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer) throws Exception {

    if (event == null || event.getHttpMethod() == null) {
      throw new NotFoundException("Invalid Request");
    }

    String method = event.getHttpMethod().toLowerCase();
    String resource = event.getResource();
    ApiGatewayRequestHandler handler = findRequestHandler(urlMap, method, resource);

    if (!handler.isAuthorized(getAwsServices(), event, authorizer, method)) {
      String s = String.format("fkq access denied (%s)", authorizer.accessSummary());
      throw new ForbiddenException(s);
    }

    return callHandlerMethod(logger, method, event, authorizer, handler);
  }

  /**
   * Processes the Response.
   * 
   * @param authorizer {@link ApiAuthorizer}
   * @param event {@link ApiGatewayRequestEvent}
   * @param resp {@link ApiRequestHandlerResponse}
   * @throws BadException BadException
   */
  private void processResponse(final ApiAuthorizer authorizer, final ApiGatewayRequestEvent event,
      final ApiRequestHandlerResponse resp) throws BadException {

    String webnotify = event.getQueryStringParameter("webnotify");

    if ("true".equals(webnotify)) {

      AwsServiceCache aws = getAwsServices();
      switch (resp.getStatus()) {
        case SC_OK:
        case SC_CREATED:
        case SC_ACCEPTED:
          String siteId = authorizer.getSiteId();
          String body = getBodyAsString(event);
          String documentId = event.getPathParameters().get("documentId");

          Map<String, String> m = new HashMap<>();
          if (siteId != null) {
            m.put("siteId", siteId);
          }

          if (documentId != null) {
            m.put("documentId", documentId);
          }

          m.put("message", body);

          String json = this.gson.toJson(m);
          SqsService sqsService = aws.getExtension(SqsService.class);
          sqsService.sendMessage(aws.environment("WEBSOCKET_SQS_URL"), json);
          break;

        default:
          break;
      }
    }
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
