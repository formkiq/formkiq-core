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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.MOVED_PERMANENTLY;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import static com.formkiq.stacks.dynamodb.ConfigService.DOCUMENT_TIME_TO_LIVE;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorizer;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRedirectResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.aws.services.lambda.exceptions.TooManyRequestsException;
import com.formkiq.aws.services.lambda.exceptions.UnauthorizedException;
import com.formkiq.aws.services.lambda.services.CacheService;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.api.CoreAwsServiceCache;
import com.formkiq.stacks.dynamodb.ConfigService;
import software.amazon.awssdk.utils.StringUtils;

/** {@link ApiGatewayRequestHandler} for "/public/webhooks". */
public class PublicWebhooksRequestHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_DOC_EXT = ".fkb64";
  /** To Milliseconds. */
  private static final long TO_MILLIS = 1000L;

  private static boolean isContentTypeJson(final String contentType) {
    return contentType != null && "application/json".equals(contentType);
  }

  private static boolean isJsonValid(final String json) {

    try {
      GSON.fromJson(json, Object.class);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private DynamicObject buildDynamicObject(final CoreAwsServiceCache awsservice,
      final String siteId, final String webhookId, final DynamicObject hook, final String body,
      final String contentType) {

    DynamicObject item = new DynamicObject(new HashMap<>());

    final String documentId = UUID.randomUUID().toString();

    if (contentType != null) {
      item.put("contentType", contentType);
    }

    item.put("content", body);
    item.put("documentId", documentId);
    item.put("userId", "webhook/" + hook.getOrDefault("path", "webhook"));
    item.put("path", "webhooks/" + webhookId);

    if (hook.containsKey("TimeToLive")) {
      item.put("TimeToLive", hook.get("TimeToLive"));
    } else {
      ConfigService configService = awsservice.getExtension(ConfigService.class);
      String ttl = configService.get(siteId).getString(DOCUMENT_TIME_TO_LIVE);
      if (ttl != null) {
        item.put("TimeToLive", ttl);
      }
    }

    if (siteId != null) {
      item.put("siteId", siteId);
    }
    return item;
  }

  private ApiRequestHandlerResponse buildRedirect(final ApiGatewayRequestEvent event,
      final String redirectUri, final String body) {
    ApiRequestHandlerResponse response;
    StringBuilder sb = new StringBuilder();
    sb.append(redirectUri);

    Map<String, String> queryMap = decodeQueryString(body);

    String responseFields = getParameter(event, "responseFields");
    if (StringUtils.isNotBlank(responseFields)) {
      String[] fields = responseFields.split(",");
      for (int i = 0; i < fields.length; i++) {
        String value = queryMap.get(fields[i]);
        sb.append(i == 0 && redirectUri.indexOf("?") == -1 ? "?" : "&");
        sb.append(fields[i] + "=" + value);
      }
    }

    response =
        new ApiRequestHandlerResponse(MOVED_PERMANENTLY, new ApiRedirectResponse(sb.toString()));
    return response;
  }

  private ApiRequestHandlerResponse buildResponse(final ApiGatewayRequestEvent event,
      final DynamicObject item) {

    String body = item.getString("content");
    String documentId = item.getString("documentId");
    String contentType = item.getString("contentType");

    ApiRequestHandlerResponse response =
        new ApiRequestHandlerResponse(SC_OK, new ApiMapResponse(Map.of("documentId", documentId)));

    String redirectUri = getParameter(event, "redirect_uri");

    if ("application/x-www-form-urlencoded".equals(contentType)
        && StringUtils.isNotBlank(redirectUri)) {

      response = buildRedirect(event, redirectUri, body);

    } else if (StringUtils.isNotBlank(redirectUri)) {
      response =
          new ApiRequestHandlerResponse(MOVED_PERMANENTLY, new ApiRedirectResponse(redirectUri));
    }

    return response;
  }

  private void checkIsWebhookValid(final DynamicObject hook)
      throws BadException, TooManyRequestsException, UnauthorizedException {
    if (hook == null || isExpired(hook)) {
      throw new BadException("invalid webhook url");
    }

    boolean isPrivate = isEnabled(hook, "private");
    boolean isEnabled = isEnabled(hook, "true");

    if (isPrivate && !isSupportPrivate()) {
      throw new UnauthorizedException("webhook is private");
    }

    if (!isPrivate && !isEnabled) {
      throw new TooManyRequestsException("webhook is disabled");
    }
  }

  private Map<String, String> decodeQueryString(final String query) {
    Map<String, String> params = new LinkedHashMap<>();
    for (String param : query.split("&")) {
      String[] keyValue = param.split("=", 2);
      String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
      String value =
          keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
      if (!key.isEmpty()) {
        params.put(key, value);
      }
    }

    return params;
  }

  private String getIdempotencyKey(final ApiGatewayRequestEvent event) {
    return event.getHeaders().get("Idempotency-Key");
  }

  @Override
  public String getRequestUrl() {
    return "/public/webhooks";
  }

  /**
   * Is Webhook Enabled.
   * 
   * @param obj {@link DynamicObject}
   * @param val {@link String}
   * @return boolean
   */
  private boolean isEnabled(final DynamicObject obj, final String val) {

    boolean enabled = false;

    if (obj.containsKey("enabled") && obj.getString("enabled").equals(val)) {
      enabled = true;
    }

    return enabled;
  }

  /**
   * Is object expired.
   * 
   * @param obj {@link DynamicObject}
   * @return boolean
   */
  private boolean isExpired(final DynamicObject obj) {

    boolean expired = false;

    if (obj.containsKey("TimeToLive")) {
      long epoch = Long.parseLong(obj.getString("TimeToLive"));

      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      ZonedDateTime date = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
      expired = now.isAfter(date);
    }

    return expired;
  }

  private boolean isIdempotencyCached(final CoreAwsServiceCache awsservice,
      final ApiGatewayRequestEvent event, final String siteId, final DynamicObject item) {

    boolean cached = false;
    String idempotencyKey = getIdempotencyKey(event);

    if (idempotencyKey != null) {

      CacheService cacheService = awsservice.getExtension(CacheService.class);

      String key = SiteIdKeyGenerator.createDatabaseKey(siteId, "idkey#" + idempotencyKey);
      String documentId = cacheService.read(key);

      if (documentId != null) {
        item.put("documentId", documentId);
        cached = true;
      } else {
        cacheService.write(key, item.getString("documentId"), 2);
      }
    }

    return cached;
  }

  protected boolean isSupportPrivate() {
    return false;
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorizer authorizer,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = getParameter(event, "siteId");
    String webhookId = getPathParameter(event, "webhooks");

    CoreAwsServiceCache cacheService = CoreAwsServiceCache.cast(awsservice);
    DynamicObject hook = cacheService.webhookService().findWebhook(siteId, webhookId);

    checkIsWebhookValid(hook);

    String body = ApiGatewayRequestEventUtil.getBodyAsString(event);

    String contentType = getContentType(event);

    if (isContentTypeJson(contentType) && !isJsonValid(body)) {
      throw new BadException("body isn't valid JSON");
    }

    DynamicObject item =
        buildDynamicObject(cacheService, siteId, webhookId, hook, body, contentType);

    if (!isIdempotencyCached(cacheService, event, siteId, item)) {
      putObjectToStaging(logger, awsservice, item, siteId);
    }

    return buildResponse(event, item);
  }

  /**
   * Put Object to Staging Bucket.
   * 
   * @param logger {@link LambdaLogger}
   * @param awsservice {@link AwsServiceCache}
   * @param item {@link DynamicObject}
   * @param siteId {@link String}
   */
  private void putObjectToStaging(final LambdaLogger logger, final AwsServiceCache awsservice,
      final DynamicObject item, final String siteId) {

    String s = GSON.toJson(item);

    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

    String stages3bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");
    String key = createDatabaseKey(siteId, item.getString("documentId") + FORMKIQ_DOC_EXT);
    logger.log("s3 putObject " + key + " into bucket " + stages3bucket);

    S3Service s3 = awsservice.getExtension(S3Service.class);
    s3.putObject(stages3bucket, key, bytes, item.getString("contentType"));
  }
}
