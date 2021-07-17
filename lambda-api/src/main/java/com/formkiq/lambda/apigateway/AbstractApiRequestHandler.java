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
package com.formkiq.lambda.apigateway;

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_ERROR;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_FORBIDDEN;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_FOUND;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_NOT_IMPLEMENTED;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_TOO_MANY_REQUESTS;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_UNAUTHORIZED;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.DateTimeException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.sqs.SqsConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.lambda.apigateway.exception.ForbiddenException;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.lambda.apigateway.exception.NotImplementedException;
import com.formkiq.lambda.apigateway.exception.TooManyRequestsException;
import com.formkiq.lambda.apigateway.exception.UnauthorizedException;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.InvalidConditionsException;
import com.google.gson.Gson;
import software.amazon.awssdk.utils.IoUtils;

/**
 * 
 * {@link RequestStreamHandler} for Api Gateway Requests.
 *
 */
public abstract class AbstractApiRequestHandler implements RequestStreamHandler {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sqs {@link SqsConnectionBuilder}
   */
  protected static void setAwsServiceCache(final Map<String, String> map,
      final DynamoDbConnectionBuilder builder, final S3ConnectionBuilder s3,
      final SsmConnectionBuilder ssm, final SqsConnectionBuilder sqs) {

    awsServices = new AwsServiceCache()
        .dbConnection(builder, map.get("DOCUMENTS_TABLE"), map.get("CACHE_TABLE")).s3Connection(s3)
        .sqsConnection(sqs).ssmConnection(ssm).stages3bucket(map.get("STAGE_DOCUMENTS_S3_BUCKET"))
        .debug("true".equals(map.get("DEBUG"))).appEnvironment(map.get("APP_ENVIRONMENT"))
        .formkiqType(map.get("FORMKIQ_TYPE")).documents3bucket(map.get("DOCUMENTS_S3_BUCKET"))
        .websocketSqsUrl(map.get("WEBSOCKET_SQS_URL"));

    awsServices.init();
  }

  /** {@link Gson}. */
  private Gson gson = GsonUtil.getInstance();

  /** construtor. */
  public AbstractApiRequestHandler() {}

  /**
   * Handle Exception.
   *
   * @param logger {@link LambdaLogger}
   * @param output {@link OutputStream}
   * @param status {@link ApiResponseStatus}
   * @param headers {@link Map}
   * @param apiResponse {@link ApiResponse}
   * @throws IOException IOException
   */
  private void buildResponse(final LambdaLogger logger, final OutputStream output,
      final ApiResponseStatus status, final Map<String, String> headers,
      final ApiResponse apiResponse) throws IOException {

    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", Integer.valueOf(status.getStatusCode()));

    if (apiResponse instanceof ApiRedirectResponse) {
      headers.clear();
      headers.put("Location", ((ApiRedirectResponse) apiResponse).getRedirectUri());
    } else if (status.getStatusCode() == SC_FOUND.getStatusCode()
        && apiResponse instanceof ApiMessageResponse) {
      headers.clear();
      headers.put("Location", ((ApiMessageResponse) apiResponse).getMessage());
    } else if (apiResponse instanceof ApiMapResponse) {
      response.put("body", this.gson.toJson(((ApiMapResponse) apiResponse).getMap()));
    } else {
      response.put("body", this.gson.toJson(apiResponse));
    }

    Map<String, String> jsonheaders = createJsonHeaders();
    jsonheaders.putAll(headers);
    response.put("headers", jsonheaders);

    writeJson(logger, awsServices, output, response);
  }

  /**
   * Create Response Headers.
   *
   * @return {@link Map} {@link String}
   */
  private Map<String, String> createJsonHeaders() {
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
   * @param method {@link String}
   * @param resource {@link String}
   * @return {@link ApiGatewayRequestHandler}
   * @throws NotFoundException Handler not found
   */
  public abstract ApiGatewayRequestHandler findRequestHandler(String method, String resource)
      throws NotFoundException;

  /**
   * Get {@link ApiGatewayRequestEvent}.
   *
   * @param input {@link InputStream}
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @return {@link ApiGatewayRequestEvent}
   * @throws IOException IOException
   */
  private ApiGatewayRequestEvent getApiGatewayEvent(final InputStream input,
      final LambdaLogger logger, final AwsServiceCache awsservice) throws IOException {

    String str = IoUtils.toUtf8String(input);

    if (awsservice.debug()) {
      logger.log(str);
    }

    ApiGatewayRequestEvent event = this.gson.fromJson(str, ApiGatewayRequestEvent.class);
    return event;
  }

  /**
   * Get {@link AwsServiceCache}.
   *
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {
    return awsServices;
  }

  @Override
  public void handleRequest(final InputStream input, final OutputStream output,
      final Context context) throws IOException {

    LambdaLogger logger = context.getLogger();

    ApiGatewayRequestEvent event = getApiGatewayEvent(input, logger, getAwsServices());
    ApiAuthorizer authorizer = new ApiAuthorizer(event);

    try {

      ApiRequestHandlerResponse object = processRequest(logger, event, authorizer);
      processResponse(authorizer, event, object);
      buildResponse(logger, output, object.getStatus(), object.getHeaders(), object.getResponse());

    } catch (NotFoundException e) {
      buildResponse(logger, output, SC_NOT_FOUND, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (TooManyRequestsException e) {
      buildResponse(logger, output, SC_TOO_MANY_REQUESTS, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (BadException | InvalidConditionsException | DateTimeException e) {
      buildResponse(logger, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ForbiddenException e) {
      buildResponse(logger, output, SC_FORBIDDEN, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (UnauthorizedException e) {
      buildResponse(logger, output, SC_UNAUTHORIZED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (NotImplementedException e) {
      buildResponse(logger, output, SC_NOT_IMPLEMENTED, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (Exception e) {
      logError(logger, e);

      buildResponse(logger, output, SC_ERROR, Collections.emptyMap(),
          new ApiResponseError("Internal Server Error"));
    }
  }

  /**
   * Processes the Response.
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
          String body = ApiGatewayRequestEventUtil.getBodyAsString(event);
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
          aws.sqsService().sendMessage(aws.websocketSqsUrl(), json);
          break;

        default:
          break;
      }
    }
  }

  /**
   * Whether {@link ApiGatewayRequestEvent} has access.
   * 
   * @param method {@link String}
   * @param path {@link String}
   * @param handler {@link ApiGatewayRequestHandler}
   * @param authorizer {@link ApiAuthorizer}
   * @return boolean
   */
  private boolean hasAccess(final String method, final String path,
      final ApiGatewayRequestHandler handler, final ApiAuthorizer authorizer) {

    boolean access = false;

    if (authorizer.isCallerAssumeRole() || authorizer.isCallerIamUser() || authorizer.isUserAdmin()
        || isPublicUrl(path)) {

      access = true;

    } else if ((handler.isReadonly(method) && authorizer.isUserReadAccess())
        || authorizer.isUserWriteAccess()) {

      access = true;
    }

    return access;
  }

  /**
   * Whether to Http Method requires access check.
   * 
   * @param method {@link String}
   * @return boolean
   */
  private boolean isCheckAccess(final String method) {
    return !"options".equals(method);
  }

  /**
   * Is Path /public/ and public urls are enabled.
   * 
   * @param path {@link String}
   * @return boolean
   */
  private boolean isPublicUrl(final String path) {
    return path.startsWith("/public/");
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
   * Process {@link ApiGatewayRequestEvent}.
   *
   * @param logger {@link LambdaLogger}
   * @param event {@link ApiGatewayRequestEvent}
   * @param authorizer {@link ApiAuthorizer}
   * 
   * @return {@link ApiRequestHandlerResponse}
   * @throws Exception Exception
   */
  private ApiRequestHandlerResponse processRequest(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer) throws Exception {

    if (event == null || event.getHttpMethod() == null) {
      throw new NotFoundException("Invalid Request");
    }

    String method = event.getHttpMethod().toLowerCase();
    String resource = event.getResource();
    ApiGatewayRequestHandler handler = findRequestHandler(method, resource);

    if (isCheckAccess(method) && !hasAccess(method, event.getPath(), handler, authorizer)) {
      throw new ForbiddenException("Access Denied");
    }

    ApiRequestHandlerResponse response = null;

    switch (method) {
      case "get":
        response = handler.get(logger, event, authorizer, awsServices);
        break;

      case "delete":
        response = handler.delete(logger, event, authorizer, awsServices);
        break;

      case "head":
        response = handler.head(logger, event, authorizer, awsServices);
        break;

      case "options":
        response = handler.options(logger, event, authorizer, awsServices);
        break;

      case "patch":
        response = handler.patch(logger, event, authorizer, awsServices);
        break;

      case "post":
        response = handler.post(logger, event, authorizer, awsServices);
        break;

      case "put":
        response = handler.put(logger, event, authorizer, awsServices);
        break;
      default:
        break;
    }

    return response;
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
  private void writeJson(final LambdaLogger logger, final AwsServiceCache awsservices,
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
