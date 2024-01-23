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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.ApiResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** {@link ApiGatewayRequestHandler} for "/webhooks/{webhookId}/tags". */
public class WebhooksTagsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Convert to Milliseconds. */
  private static final long TO_MILLIS = 1000L;

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    PaginationResults<DynamicObject> list = webhooksService.findTags(siteId, id, null);

    List<Map<String, Object>> tags = list.getResults().stream().map(m -> {
      Map<String, Object> map = new HashMap<>();

      map.put("insertedDate", m.getString("inserteddate"));
      map.put("webhookId", id);
      map.put("type", m.getString("type"));
      map.put("userId", m.getString("userId"));
      map.put("value", m.getString("tagValue"));
      map.put("key", m.getString("tagKey"));

      return map;
    }).collect(Collectors.toList());

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("tags", tags)));
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks/{webhookId}/tags";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    DocumentTag tag = fromBodyToObject(event, DocumentTag.class);

    if (tag.getKey() == null || tag.getKey().length() == 0) {
      throw new BadException("invalid json body");
    }

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    tag.setType(DocumentTagType.USERDEFINED);
    tag.setInsertedDate(new Date());
    tag.setUserId(authorization.getUsername());

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    DynamicObject webhook = webhooksService.findWebhook(siteId, id);
    if (webhook == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }

    Date ttl = null;
    String ttlString = webhook.getString("TimeToLive");
    if (ttlString != null) {
      long epoch = Long.parseLong(ttlString);
      ttl = new Date(epoch * TO_MILLIS);
    }

    webhooksService.addTags(siteId, id, Arrays.asList(tag), ttl);

    ApiResponse resp = new ApiMessageResponse("Created Tag '" + tag.getKey() + "'.");
    return new ApiRequestHandlerResponse(SC_CREATED, resp);
  }
}
