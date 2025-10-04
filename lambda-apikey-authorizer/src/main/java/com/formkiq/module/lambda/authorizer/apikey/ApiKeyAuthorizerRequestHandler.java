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
package com.formkiq.module.lambda.authorizer.apikey;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.graalvm.annotations.ReflectableClass;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.dynamodb.ApiKey;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.stacks.dynamodb.ApiKeysServiceExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;

/** {@link RequestHandler} for handling DynamoDb to Tesseract OCR Processor. */
@Reflectable
@ReflectableClass(className = APIGatewayV2CustomAuthorizerEvent.class)
@ReflectableClass(className = APIGatewayV2CustomAuthorizerEvent.RequestContext.class)
@ReflectableClass(className = APIGatewayV2CustomAuthorizerEvent.Http.class)
public class ApiKeyAuthorizerRequestHandler
    implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, Map<String, Object>> {

  /** Max Api Key Length. */
  private static final int MAX_APIKEY_LENGTH = 100;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
  static {
    if (System.getenv().containsKey("AWS_REGION")) {
      awsServices = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry()).build();
    }
  }

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  /**
   * constructor.
   * 
   */
  public ApiKeyAuthorizerRequestHandler() {
    this(awsServices);
  }

  /**
   * constructor.
   * 
   * @param awsServiceCache {@link AwsServiceCache}
   *
   */
  public ApiKeyAuthorizerRequestHandler(final AwsServiceCache awsServiceCache) {
    awsServices = awsServiceCache;
    awsServices.register(ApiKeysService.class, new ApiKeysServiceExtension());
  }

  private String getIdentitySource(final APIGatewayV2CustomAuthorizerEvent input) {
    List<String> identitySource = input.getIdentitySource();
    return !identitySource.isEmpty() ? identitySource.get(0) : null;
  }

  @Override
  public Map<String, Object> handleRequest(final APIGatewayV2CustomAuthorizerEvent input,
      final Context context) {

    Logger logger = awsServices.getLogger();
    ApiKeysService apiKeys = awsServices.getExtension(ApiKeysService.class);

    if (logger.isLogged(LogLevel.DEBUG)) {
      logger.debug(gson.toJson(input));
    }

    String apiKey = getIdentitySource(input);
    apiKey = apiKey != null && apiKey.length() < MAX_APIKEY_LENGTH ? apiKey : null;

    ApiKey api = apiKeys.get(apiKey, false);
    api = api != null ? api
        : new ApiKey().name("").apiKey("").siteId("").permissions(Collections.emptyList());

    String siteId = api.siteId();
    boolean isAuthorized = apiKey != null && apiKey.equals(api.apiKey());

    String apiKeyName = api.name();
    String group = isAuthorized ? "[" + siteId + " API_KEY]" : "[API_KEY]";
    String permissions =
        api.permissions().stream().map(Enum::name).sorted().collect(Collectors.joining(","));

    log(logger, input, isAuthorized, group);

    return Map.of("isAuthorized", isAuthorized, "context", Map.of("apiKeyClaims", Map
        .of("permissions", permissions, "cognito:groups", group, "cognito:username", apiKeyName)));
  }

  private void log(final Logger logger, final APIGatewayV2CustomAuthorizerEvent input,
      final boolean isAuthorized, final String group) {

    String requestId = "";
    String sourceIp = "";
    String time = "";
    String method = "";
    String routeKey = "";
    String protocol = "";

    APIGatewayV2CustomAuthorizerEvent.RequestContext requestContent = input.getRequestContext();
    if (requestContent != null) {
      requestId = requestContent.getRequestId();
      time = requestContent.getTime().toString();
      routeKey = requestContent.getRouteKey();

      APIGatewayV2CustomAuthorizerEvent.Http http = requestContent.getHttp();
      if (http != null) {
        sourceIp = http.getSourceIp();
        method = http.getMethod();
        protocol = http.getProtocol();
      }
    }

    String s = String.format(
        "{\"requestId\": \"%s\",\"ip\": \"%s\",\"requestTime\": \"%s\",\"httpMethod\": \"%s\","
            + "\"routeKey\": \"%s\","
            + "\"protocol\": \"%s\",\"siteId\":\"%s\",\"isAuthorized\":\"%s\"}",
        requestId, sourceIp, time, method, routeKey, protocol, group, isAuthorized);

    logger.info(s);
  }
}
