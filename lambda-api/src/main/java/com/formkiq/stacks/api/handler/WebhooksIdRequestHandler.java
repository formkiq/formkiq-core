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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.lambda.apigateway.ApiAuthorizer;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEventUtil;
import com.formkiq.lambda.apigateway.ApiGatewayRequestHandler;
import com.formkiq.lambda.apigateway.ApiMapResponse;
import com.formkiq.lambda.apigateway.ApiMessageResponse;
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.NotFoundException;
import com.formkiq.stacks.common.objects.DynamicObject;

/** {@link ApiGatewayRequestHandler} for "/webhooks/{webhookId}". */
public class WebhooksIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = getSiteId(event);
    String id = getPathParameter(event, "webhookId");
    
    if (awsServices.webhookService().findWebhook(siteId, id) == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }
    awsServices.webhookService().deleteWebhook(siteId, id);
    
    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMessageResponse("'" + id + "' object deleted"));
  }
  
  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {
    
    String siteId = getSiteId(event);
    String id = getPathParameter(event, "webhookId");
    DynamicObject m = awsServices.webhookService().findWebhook(siteId, id);
    
    String url = awsServices.ssmService()
        .getParameterValue("/formkiq/" + awsServices.appEnvironment() + "/api/DocumentsHttpUrl");

    Map<String, Object> map = new HashMap<>();
    map.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    map.put("id", m.getString("documentId"));
    map.put("name", m.getString("path"));
    map.put("url", url + "/public/webhooks/" + m.getString("documentId"));
    map.put("insertedDate", m.getString("inserteddate"));
    map.put("userId", m.getString("userId"));
        
    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks/{webhookId}";
  }
  
  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = getSiteId(event);
    String id = getPathParameter(event, "webhookId");
    
    if (awsServices.webhookService().findWebhook(siteId, id) == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }
    
    DynamicObject obj = fromBodyToDynamicObject(logger, event);
    
    Map<String, Object> map = new HashMap<>();
    
    if (obj.containsKey("name")) {
      map.put("name", obj.getString("name"));
    }
    
    if (obj.containsKey("ttl")) {
      ZonedDateTime now =
          ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(obj.getString("ttl")));
      Date ttlDate = Date.from(now.toInstant());
      map.put("TimeToLive", ttlDate);
    }
    
    awsServices.webhookService().updateWebhook(siteId, id, new DynamicObject(map));
    
    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMessageResponse("'" + id + "' object updated"));
  }
}
