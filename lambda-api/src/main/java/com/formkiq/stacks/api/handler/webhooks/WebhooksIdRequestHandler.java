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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.NotFoundException;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** {@link ApiGatewayRequestHandler} for "/webhooks/{webhookId}". */
public class WebhooksIdRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse delete(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    if (webhooksService.findWebhook(siteId, id) == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }
    webhooksService.deleteWebhook(siteId, id);

    return ApiRequestHandlerResponse.builder().ok().body("message", "'" + id + "' object deleted")
        .build();
  }

  @Override
  public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    String id = getPathParameter(event, "webhookId");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    DynamicObject m = webhooksService.findWebhook(siteId, id);
    if (m == null) {
      throw new NotFoundException("Webhook 'id' not found");
    }

    String url = getUrl(awsServices, siteId, m);
    Map<String, Object> map = new DynamicObjectToMap(siteId, url).apply(m);

    return ApiRequestHandlerResponse.builder().ok().body(map).build();
  }

  private String getUrl(final AwsServiceCache awsServices, final String siteId,
      final DynamicObject m) {
    SsmService ssmService = awsServices.getExtension(SsmService.class);

    String url = ssmService.getParameterValue(
        "/formkiq/" + awsServices.environment("APP_ENVIRONMENT") + "/api/DocumentsPublicHttpUrl");

    String path = "private".equals(m.getString("enabled")) ? "/private" : "/public";

    String u = url + path + "/webhooks/" + m.getString("documentId");
    if (siteId != null && !DEFAULT_SITE_ID.equals(siteId)) {
      u += "?siteId=" + siteId;
    }
    return u;
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks/{webhookId}";
  }

  @Override
  public ApiRequestHandlerResponse patch(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsServices) throws Exception {

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

    return ApiRequestHandlerResponse.builder().ok().body("message", "'" + id + "' object updated")
        .build();
  }
}
