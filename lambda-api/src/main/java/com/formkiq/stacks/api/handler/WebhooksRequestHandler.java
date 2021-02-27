/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.lambda.apigateway.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
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
import com.formkiq.lambda.apigateway.ApiRequestHandlerResponse;
import com.formkiq.lambda.apigateway.AwsServiceCache;
import com.formkiq.lambda.apigateway.exception.BadException;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentTag;

/** {@link ApiGatewayRequestHandler} for "/webhooks". */
public class WebhooksRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorizer.getSiteId();
    String url = awsServices.ssmService()
        .getParameterValue("/formkiq/" + awsServices.appEnvironment() + "/api/DocumentsHttpUrl");

    List<DynamicObject> list = awsServices.webhookService().findWebhooks(siteId);

    List<Map<String, Object>> webhooks = list.stream().map(m -> {

      Map<String, Object> map = new HashMap<>();
      String u = url + "/public/webhooks/" + m.getString("documentId");
      if (siteId != null && !DEFAULT_SITE_ID.equals(siteId)) {
        u += "?siteId=" + siteId;
      }

      map.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      map.put("id", m.getString("documentId"));
      map.put("name", m.getString("path"));
      map.put("url", u);
      map.put("insertedDate", m.getString("inserteddate"));
      map.put("userId", m.getString("userId"));

      return map;
    }).collect(Collectors.toList());

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("webhooks", webhooks)));
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks";
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorizer.getSiteId();
    DynamicObject o = fromBodyToDynamicObject(logger, event);

    if (o == null || o.get("name") == null) {
      throw new BadException("Invalid JSON body.");
    }

    Date ttlDate = null;
    String ttl = o.getString("ttl");
    if (ttl != null) {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(ttl));
      ttlDate = Date.from(now.toInstant());
    }

    String name = o.getString("name");
    String userId = getCallingCognitoUsername(event);
    String id = awsservice.webhookService().saveWebhook(siteId, name, userId, ttlDate);

    if (o.containsKey("tags")) {
      List<DynamicObject> dtags = o.getList("tags");

      Date date = new Date();
      Collection<DocumentTag> tags = dtags.stream()
          .map(d -> new DocumentTag(null, d.getString("key"), d.getString("value"), date, userId))
          .collect(Collectors.toList());
      awsservice.webhookService().addTags(siteId, id, tags, ttlDate);
    }

    setPathParameter(event, "webhookId", id);

    WebhooksIdRequestHandler h = new WebhooksIdRequestHandler();
    ApiRequestHandlerResponse response = h.get(logger, event, authorizer, awsservice);

    return response;
  }
}
