/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_ERROR;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_FORBIDDEN;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_FOUND;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_NOT_FOUND;
import static com.formkiq.stacks.api.handler.ApiResponseStatus.SC_NOT_IMPLEMENTED;
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
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableImport;
import com.formkiq.stacks.api.handler.ApiRequestHandlerResponse;
import com.formkiq.stacks.api.handler.ApiResponseStatus;
import com.formkiq.stacks.api.handler.AwsServiceCache;
import com.formkiq.stacks.api.handler.RequestHandler;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.stacks.dynamodb.InvalidConditionsException;
import com.formkiq.stacks.dynamodb.PaginationMapToken;
import com.formkiq.stacks.dynamodb.Preset;
import com.formkiq.stacks.dynamodb.PresetTag;
import com.formkiq.stacks.dynamodb.SearchQuery;
import com.formkiq.stacks.dynamodb.SearchTagCriteria;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;

/** {@link RequestStreamHandler} for handling API Gateway 'GET' requests. */
@Reflectable
@ReflectableImport(classes = {DocumentItemDynamoDb.class, DocumentTagType.class, DocumentTag.class,
    PaginationMapToken.class, SearchQuery.class, SearchTagCriteria.class, PresetTag.class,
    Preset.class})
public class ApiRequestHandler implements RequestStreamHandler {

  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;

  static {

    if (System.getenv("AWS_REGION") != null) {
      setUpHandler(System.getenv(),
          new DynamoDbConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new S3ConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new SsmConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()),
          new SqsConnectionBuilder().setRegion(Region.of(System.getenv("AWS_REGION")))
              .setCredentials(EnvironmentVariableCredentialsProvider.create()));
    }
  }

  /**
   * Setup Api Request Handlers.
   *
   * @param map {@link Map}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param s3 {@link S3ConnectionBuilder}
   * @param ssm {@link SsmConnectionBuilder}
   * @param sqs {@link SqsConnectionBuilder}
   */
  protected static void setUpHandler(final Map<String, String> map,
      final DynamoDbConnectionBuilder builder, final S3ConnectionBuilder s3,
      final SsmConnectionBuilder ssm, final SqsConnectionBuilder sqs) {

    awsServices = new AwsServiceCache()
        .dbConnection(builder, map.get("DOCUMENTS_TABLE"), map.get("CACHE_TABLE")).s3Connection(s3)
        .sqsConnection(sqs).ssmConnection(ssm).stages3bucket(map.get("STAGE_DOCUMENTS_S3_BUCKET"))
        .debug("true".equals(map.get("DEBUG"))).appEnvironment(map.get("APP_ENVIRONMENT"))
        .documents3bucket(map.get("DOCUMENTS_S3_BUCKET"));

    awsServices.init();

    ApiRequestMapper.init(map);
  }

  /** constructor. */
  public ApiRequestHandler() {}

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

    Map<String, String> jsonheaders = createJsonHeaders();
    jsonheaders.putAll(headers);
    response.put("headers", jsonheaders);

    if (status.getStatusCode() == SC_FOUND.getStatusCode()
        && apiResponse instanceof ApiMessageResponse) {
      headers.clear();
      headers.put("Location", ((ApiMessageResponse) apiResponse).getMessage());
    } else if (apiResponse instanceof ApiMapResponse) {
      response.put("body", RequestHandler.GSON.toJson(((ApiMapResponse) apiResponse).getMap()));
    } else {
      response.put("body", RequestHandler.GSON.toJson(apiResponse));
    }

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

    ApiGatewayRequestEvent event = RequestHandler.GSON.fromJson(str, ApiGatewayRequestEvent.class);
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
      buildResponse(logger, output, object.getStatus(), object.getHeaders(), object.getResponse());

    } catch (NotFoundException e) {
      buildResponse(logger, output, SC_NOT_FOUND, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (BadException | InvalidConditionsException | DateTimeException e) {
      buildResponse(logger, output, SC_BAD_REQUEST, Collections.emptyMap(),
          new ApiResponseError(e.getMessage()));
    } catch (ForbiddenException e) {
      buildResponse(logger, output, SC_FORBIDDEN, Collections.emptyMap(),
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
   * Whether {@link ApiGatewayRequestEvent} has access.
   * 
   * @param method {@link String}
   * @param path {@link String}
   * @param handler {@link RequestHandler}
   * @param authorizer {@link ApiAuthorizer}
   * @return boolean
   */
  private boolean hasAccess(final String method, final String path, final RequestHandler handler,
      final ApiAuthorizer authorizer) {

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

    RequestHandler handler = ApiRequestMapper.findHandler(event);

    if (handler == null) {
      String resource = event.getResource();
      throw new NotFoundException("Invalid resource " + resource);
    }

    String method = event.getHttpMethod().toLowerCase();
    if (isCheckAccess(method) && !hasAccess(method, event.getPath(), handler, authorizer)) {
      throw new ForbiddenException("Access Denied");
    }

    ApiRequestHandlerResponse object = handler.process(logger, event, authorizer, awsServices);
    return object;
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

    String json = RequestHandler.GSON.toJson(response);

    if (awsservices.debug()) {
      logger.log("response: " + json);
    }

    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
    writer.write(json);
    writer.close();
  }
}
