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
package com.formkiq.stacks.api.handler.webhooks;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.JsonToObject;
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
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

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
    }).toList();

    return ApiRequestHandlerResponse.builder().ok().body("tags", tags).build();
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks/{webhookId}/tags";
  }

  @Override
  public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    DocumentTag tag = JsonToObject.fromJson(awsServices, event, DocumentTag.class);

    if (tag.getKey() == null || tag.getKey().isEmpty()) {
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

    webhooksService.addTags(siteId, id, List.of(tag), ttl);

    return ApiRequestHandlerResponse.builder().created()
        .body("message", "Created Tag '" + tag.getKey() + "'.").build();
  }
}
