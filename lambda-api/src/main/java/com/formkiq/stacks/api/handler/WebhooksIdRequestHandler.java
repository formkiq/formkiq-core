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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiMessageResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** {@link ApiGatewayRequestHandler} for "/webhooks/{webhookId}". */
public class WebhooksIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse delete(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    if (webhooksService.findWebhook(siteId, id) == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }
    webhooksService.deleteWebhook(siteId, id);

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMessageResponse("'" + id + "' object deleted"));
  }

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    DynamicObject m = webhooksService.findWebhook(siteId, id);
    if (m == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }

    SsmService ssmService = awsServices.getExtension(SsmService.class);

    String url = ssmService.getParameterValue(
        "/formkiq/" + awsServices.environment("APP_ENVIRONMENT") + "/api/DocumentsPublicHttpUrl");

    String path = "private".equals(m.getString("enabled")) ? "/private" : "/public";

    String u = url + path + "/webhooks/" + m.getString("documentId");
    if (siteId != null && !DEFAULT_SITE_ID.equals(siteId)) {
      u += "?siteId=" + siteId;
    }

    Map<String, Object> map = new HashMap<>();
    map.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
    map.put("id", m.getString("documentId"));
    map.put("name", m.getString("path"));
    map.put("url", u);
    map.put("insertedDate", m.getString("inserteddate"));
    map.put("userId", m.getString("userId"));
    map.put("enabled", m.getString("enabled"));
    map.put("ttl", m.getString("ttl"));

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(map));
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks/{webhookId}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    if (webhooksService.findWebhook(siteId, id) == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }

    DynamicObject obj = fromBodyToDynamicObject(event);

    Map<String, Object> map = new HashMap<>();

    if (obj.containsKey("name")) {
      map.put("name", obj.getString("name"));
    }

    if (obj.containsKey("enabled")) {
      map.put("enabled", obj.getBoolean("enabled"));
    }

    Date ttlDate = null;
    if (obj.containsKey("ttl")) {
      ZonedDateTime now =
          ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(obj.getString("ttl")));
      ttlDate = Date.from(now.toInstant());
      map.put("TimeToLive", ttlDate);
    }

    webhooksService.updateWebhook(siteId, id, new DynamicObject(map));

    if (ttlDate != null) {
      webhooksService.updateTimeToLive(siteId, id, ttlDate);
    }

    return new ApiRequestHandlerResponse(SC_OK,
        new ApiMessageResponse("'" + id + "' object updated"));
  }
}
