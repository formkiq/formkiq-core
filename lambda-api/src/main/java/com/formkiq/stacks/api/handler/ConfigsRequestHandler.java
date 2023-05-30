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
package com.formkiq.stacks.api.handler;

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.ConfigService.CHATGPT_API_KEY;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENT_SIZE_BYTES;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;

/** {@link ApiGatewayRequestHandler} for "/configuration". */
public class ConfigsRequestHandler implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /**
   * constructor.
   *
   */
  public ConfigsRequestHandler() {}

  @Override
  public void beforeGet(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorizer);
  }

  @Override
  public void beforePatch(final LambdaLogger logger, final ApiGatewayRequestEvent event,
      final ApiAuthorizer authorizer, final AwsServiceCache awsServices) throws Exception {
    checkPermissions(authorizer);
  }

  private void checkPermissions(final ApiAuthorizer authorizer) throws UnauthorizedException {
    if (!authorizer.isUserAdmin()) {
      throw new UnauthorizedException("user is unauthorized");
    }
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    ConfigService configService = awsservice.getExtension(ConfigService.class);

    DynamicObject obj = configService.get(siteId);
    Map<String, Object> map = new HashMap<>();
    map.put("chatGptApiKey", obj.getOrDefault(CHATGPT_API_KEY, ""));
    map.put("maxContentLengthBytes", obj.getOrDefault(MAX_DOCUMENT_SIZE_BYTES, ""));
    map.put("maxDocuments", obj.getOrDefault(MAX_DOCUMENTS, ""));
    map.put("maxWebhooks", obj.getOrDefault(MAX_WEBHOOKS, ""));

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/configuration";
  }

  @SuppressWarnings("unchecked")
  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();

    Map<String, String> body = fromBodyToObject(logger, event, Map.class);

    Map<String, Object> map = new HashMap<>();
    put(map, body, CHATGPT_API_KEY, "chatGptApiKey");
    put(map, body, MAX_DOCUMENT_SIZE_BYTES, "maxContentLengthBytes");
    put(map, body, MAX_DOCUMENTS, "maxDocuments");
    put(map, body, MAX_WEBHOOKS, "maxWebhooks");

    if (!map.isEmpty()) {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      configService.save(siteId, new DynamicObject(map));

      return new ApiRequestHandlerResponse(SC_OK,
          new ApiMapResponse(Map.of("message", "Config saved")));
    }

    throw new BadException("missing required body parameters");
  }

  private void put(final Map<String, Object> map, final Map<String, String> body,
      final String mapKey, final String bodyKey) {
    if (body.containsKey(bodyKey)) {
      map.put(mapKey, body.get(bodyKey));
    }
  }
}
