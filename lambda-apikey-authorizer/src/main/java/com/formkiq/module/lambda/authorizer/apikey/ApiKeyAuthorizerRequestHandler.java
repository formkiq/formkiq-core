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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.ApiKey;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.stacks.dynamodb.ApiKeysServiceExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.utils.IoUtils;

/** {@link RequestHandler} for handling DynamoDb to Tesseract OCR Processor. */
@Reflectable
public class ApiKeyAuthorizerRequestHandler implements RequestStreamHandler {

  /** Max Api Key Length. */
  private static final int MAX_APIKEY_LENGTH = 100;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache awsServices;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  static {
    if (System.getenv().containsKey("AWS_REGION")) {
      awsServices = new AwsServiceCacheBuilder(System.getenv(), Map.of(),
          EnvironmentVariableCredentialsProvider.create())
          .addService(new DynamoDbAwsServiceRegistry()).build();
    }
  }

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

  /**
   * Get {@link AwsServiceCache}.
   * 
   * @return {@link AwsServiceCache}
   */
  public AwsServiceCache getAwsServices() {
    return awsServices;
  }

  @SuppressWarnings("unchecked")
  private String getIdentitySource(final Map<String, Object> map) {
    List<String> identitySource = (List<String>) map.get("identitySource");
    return !identitySource.isEmpty() ? identitySource.get(0) : null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleRequest(final InputStream input, final OutputStream output,
      final Context context) throws IOException {

    LambdaLogger logger = context.getLogger();
    ApiKeysService apiKeys = awsServices.getExtension(ApiKeysService.class);

    String json = IoUtils.toUtf8String(input);

    if (awsServices.debug()) {
      logger.log(json);
    }

    Map<String, Object> map = this.gson.fromJson(json, Map.class);

    String apiKey = getIdentitySource(map);
    apiKey = apiKey != null && apiKey.length() < MAX_APIKEY_LENGTH ? apiKey : null;

    ApiKey api = apiKeys.get(apiKey, false);
    api = api != null ? api
        : new ApiKey().name("").apiKey("").siteId("").permissions(Collections.emptyList());

    String siteId = api.siteId();
    boolean isAuthorized = apiKey != null && apiKey.equals(api.apiKey());

    String apiKeyName = api.name();
    String group = isAuthorized ? "[" + siteId + "]" : "[]";
    String permissions =
        api.permissions().stream().map(p -> p.name()).sorted().collect(Collectors.joining(","));

    log(logger, map, isAuthorized, group);

    Map<String, Object> response = Map.of("isAuthorized", Boolean.valueOf(isAuthorized), "context",
        Map.of("apiKeyClaims", Map.of("permissions", permissions, "cognito:groups", group,
            "cognito:username", apiKeyName)));

    OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
    writer.write(this.gson.toJson(response));
    writer.close();
  }

  @SuppressWarnings("unchecked")
  private void log(final LambdaLogger logger, final Map<String, Object> map,
      final boolean isAuthorized, final String group) {

    Map<String, Object> requestContext =
        map.containsKey("requestContext") ? (Map<String, Object>) map.get("requestContext")
            : Collections.emptyMap();
    Map<String, String> http =
        map.containsKey("http") ? (Map<String, String>) map.get("http") : Collections.emptyMap();

    String s = String.format(
        "{\"requestId\": \"%s\",\"ip\": \"%s\",\"requestTime\": \"%s\",\"httpMethod\": \"%s\","
            + "\"routeKey\": \"%s\","
            + "\"protocol\": \"%s\",\"siteId\":\"%s\",\"isAuthorized\":\"%s\"}",
        map.get("requestId"), http.get("sourceIp"), requestContext.get("time"), http.get("method"),
        map.get("routeKey"), requestContext.get("protocol"), group, String.valueOf(isAuthorized));

    logger.log(s);
  }
}
