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
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.isDefaultSiteId;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_CREATED;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.ConfigService.WEBHOOK_TIME_TO_LIVE;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.TooManyRequestsException;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** {@link ApiGatewayRequestHandler} for "/webhooks". */
public class WebhooksRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  @Override
  public ApiRequestHandlerResponse get(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsServices) throws Exception {

    String siteId = authorization.getSiteId();
    SsmService ssmService = awsServices.getExtension(SsmService.class);

    String url = ssmService.getParameterValue(
        "/formkiq/" + awsServices.environment("APP_ENVIRONMENT") + "/api/DocumentsPublicHttpUrl");

    WebhooksService webhooksService = awsServices.getExtension(WebhooksService.class);

    List<DynamicObject> list = webhooksService.findWebhooks(siteId);

    List<Map<String, Object>> webhooks = list.stream().map(m -> {

      String path = "private".equals(m.getString("enabled")) ? "/private" : "/public";

      Map<String, Object> map = new HashMap<>();
      String u = url + path + "/webhooks/" + m.getString("documentId");
      if (siteId != null && !DEFAULT_SITE_ID.equals(siteId)) {
        u += "?siteId=" + siteId;
      }

      map.put("siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      map.put("webhookId", m.getString("documentId"));
      map.put("name", m.getString("path"));
      map.put("url", u);
      map.put("insertedDate", m.getString("inserteddate"));
      map.put("userId", m.getString("userId"));
      map.put("enabled", m.getString("enabled"));
      map.put("ttl", m.getString("ttl"));

      return map;
    }).collect(Collectors.toList());

    return new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("webhooks", webhooks)));
  }

  @Override
  public String getRequestUrl() {
    return "/webhooks";
  }

  private Date getTtlDate(final AwsServiceCache awsservice, final String siteId,
      final DynamicObject o) {
    Date ttlDate = null;
    String ttl = o.getString("ttl");

    if (ttl == null) {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      DynamicObject config = configService.get(siteId);
      ttl = config.getString(WEBHOOK_TIME_TO_LIVE);
    }

    if (ttl != null) {
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong(ttl));
      ttlDate = Date.from(now.toInstant());
    }

    return ttlDate;
  }

  private boolean isOverMaxWebhooks(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String siteId) {

    boolean over = false;
    ConfigService configService = awsservice.getExtension(ConfigService.class);
    DynamicObject config = configService.get(siteId);

    String maxString = config.getString(MAX_WEBHOOKS);

    if (maxString != null) {

      try {

        int max = Integer.parseInt(maxString);

        WebhooksService webhooksService = awsservice.getExtension(WebhooksService.class);
        int numberOfWebhooks = webhooksService.findWebhooks(siteId).size();

        if (awsservice.debug()) {
          logger.log("found config for maximum webhooks " + maxString);
          logger.log("found " + numberOfWebhooks + " webhooks");
        }

        if (numberOfWebhooks >= max) {
          over = true;
        }

      } catch (NumberFormatException e) {
        over = false;
        e.printStackTrace();
      }
    }

    return over;
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    DynamicObject o = fromBodyToDynamicObject(event);

    validatePost(logger, awsservice, siteId, o);

    String id = saveWebhook(event, authorization, awsservice, siteId, o);

    ApiMapResponse resp = new ApiMapResponse(
        Map.of("webhookId", id, "siteId", isDefaultSiteId(siteId) ? DEFAULT_SITE_ID : siteId));
    return new ApiRequestHandlerResponse(SC_CREATED, resp);
  }

  private String saveWebhook(final ApiGatewayRequestEvent event,
      final ApiAuthorization authorization, final AwsServiceCache awsservice, final String siteId,
      final DynamicObject o) {

    WebhooksService webhooksService = awsservice.getExtension(WebhooksService.class);

    Date ttlDate = getTtlDate(awsservice, siteId, o);

    String name = o.getString("name");
    String userId = authorization.getUsername();
    String enabled = o.containsKey("enabled") ? o.getString("enabled") : "true";
    String id = webhooksService.saveWebhook(siteId, name, userId, ttlDate, enabled);

    if (o.containsKey("tags")) {
      List<DynamicObject> dtags = o.getList("tags");

      Date date = new Date();
      Collection<DocumentTag> tags = dtags.stream()
          .map(d -> new DocumentTag(null, d.getString("key"), d.getString("value"), date, userId))
          .collect(Collectors.toList());
      webhooksService.addTags(siteId, id, tags, ttlDate);
    }

    return id;
  }

  private void validatePost(final LambdaLogger logger, final AwsServiceCache awsservice,
      final String siteId, final DynamicObject o) throws BadException, TooManyRequestsException {

    if (o == null || o.get("name") == null) {
      throw new BadException("Invalid JSON body.");
    }

    if (isOverMaxWebhooks(logger, awsservice, siteId)) {
      throw new TooManyRequestsException("Reached max number of webhooks");
    }
  }
}
