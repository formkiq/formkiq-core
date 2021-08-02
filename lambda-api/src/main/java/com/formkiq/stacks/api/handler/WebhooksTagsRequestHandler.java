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

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_CREATED;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.ApiResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.DocumentTagType;
import com.formkiq.stacks.dynamodb.PaginationResults;

/** {@link ApiGatewayRequestHandler} for "/webhooks/{webhookId}/tags". */
public class WebhooksTagsRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {
  
  /** Convert to Milliseconds. */
  private static final long TO_MILLIS = 1000L;
  
  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {
    
    String siteId = authorizer.getSiteId();
    String id = getPathParameter(event, "webhookId");
    PaginationResults<DynamicObject> list = awsServices.webhookService().findTags(siteId, id, null);
    
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
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {

    DocumentTag tag = fromBodyToObject(logger, event, DocumentTag.class);

    if (tag.getKey() == null || tag.getKey().length() == 0) {
      throw new BadException("invalid json body");
    }

    String siteId = authorizer.getSiteId();
    String id = getPathParameter(event, "webhookId");

    tag.setType(DocumentTagType.USERDEFINED);
    tag.setInsertedDate(new Date());
    tag.setUserId(getCallingCognitoUsername(event));
    
    DynamicObject webhook = awsServices.webhookService().findWebhook(siteId, id);
    if (webhook == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }
    
    Date ttl = null;
    String ttlString = webhook.getString("TimeToLive");
    if (ttlString != null) {
      long epoch = Long.parseLong(ttlString);
      ttl = new Date(epoch * TO_MILLIS);
    }
    
    awsServices.webhookService().addTags(siteId, id, Arrays.asList(tag), ttl);

    ApiResponse resp = new ApiMessageResponse("Created Tag '" + tag.getKey() + "'.");
    return new ApiRequestHandlerResponse(SC_CREATED, resp);
  }
}
