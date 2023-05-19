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
package com.formkiq.stacks.api;

import static com.formkiq.stacks.dynamodb.ConfigService.MAX_WEBHOOKS;
import static com.formkiq.stacks.dynamodb.ConfigService.WEBHOOK_TIME_TO_LIVE;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /webhooks. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiWebhooksRequestTest extends AbstractRequestHandler {

  /** To Milliseconds. */
  private static final long TO_MILLIS = 1000L;

  /**
   * Get /webhooks empty.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhooks01() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-webhooks01.json");
    addParameter(event, "siteId", UUID.randomUUID().toString());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"webhooks\":[]}", m.get("body"));
  }

  /**
   * POST/Get /webhooks.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhooks02() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
      addParameter(event, "siteId", siteId);

      String response = handleRequest(event);

      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      if (siteId == null) {
        assertEquals("default", result.get("siteId"));
      } else {
        assertNotNull(result.get("siteId"));
        assertNotEquals("default", result.get("siteId"));
      }

      assertNotNull(result.get("id"));
      final String id = result.get("id").toString();

      assertNotNull(result.get("insertedDate"));
      assertEquals("john smith", result.get("name"));
      assertEquals("test@formkiq.com", result.get("userId"));
      assertEquals("true", result.get("enabled"));

      verifyUrl(siteId, id, result);

      event = toRequestEvent("/request-get-webhooks01.json");
      addParameter(event, "siteId", siteId);

      // when
      response = handleRequest(event);

      // then
      m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("webhooks");
      Optional<Map<String, Object>> o =
          list.stream().filter(l -> l.get("url").toString().contains(id)).findFirst();
      assertTrue(o.isPresent());

      final int expectedCount = 7;
      assertEquals(expectedCount, o.get().size());
      assertNotNull(o.get().get("insertedDate"));
      assertNotNull(o.get().get("id"));
      assertEquals("john smith", o.get().get("name"));
      assertEquals("true", o.get().get("enabled"));

      verifyUrl(siteId, id, o.get());

      assertEquals("test@formkiq.com", o.get().get("userId"));
    }
  }

  /**
   * POST /webhooks. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks01() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
    String ttl = "90000";
    event.setBody("{\"name\":\"john smith\",\"ttl\":\"" + ttl + "\"}");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("default", result.get("siteId"));
    final String id = result.get("id").toString();

    assertNotNull(result.get("insertedDate"));
    assertEquals("john smith", result.get("name"));
    assertEquals("test@formkiq.com", result.get("userId"));
    assertEquals("http://localhost:8080/public/webhooks/" + id, result.get("url"));
    assertNotNull(result.get("ttl"));

    WebhooksService webhookService = getAwsServices().webhookService();
    DynamicObject obj = webhookService.findWebhook(null, id);

    long epoch = Long.parseLong(obj.getString("TimeToLive"));
    ZonedDateTime ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1).plusHours(1);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

    // given
    DocumentTag tag = new DocumentTag(id, "category", "person", new Date(), "joe");

    // when
    webhookService.addTags(null, id, Arrays.asList(tag), null);

    // then
    assertNull(webhookService.findTag(null, id, "category").getString("TimeToLive"));

    // update ttl/name
    // given
    ttl = "180000";
    event = toRequestEvent("/request-patch-webhooks-webhookid01.json");
    event.setPathParameters(Map.of("webhookId", id));
    event.setBody("{\"name\":\"john smith2\",\"ttl\":\"" + ttl + "\"}");

    // when
    response = handleRequest(event);

    // then
    m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("'" + id + "' object updated", result.get("message"));

    obj = webhookService.findWebhook(null, id);
    assertEquals("john smith2", obj.get("path"));

    epoch = Long.parseLong(obj.getString("TimeToLive"));
    ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(2);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

    DynamicObject dtag = webhookService.findTag(null, id, "category");
    assertNotNull(dtag.getString("TimeToLive"));
    epoch = Long.parseLong(dtag.getString("TimeToLive"));
    ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2).plusHours(2);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());
  }

  /**
   * POST /webhooks with TAGS. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks02() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
      addParameter(event, "siteId", siteId);

      event.setBody(
          "{\"name\":\"joe smith\",\"tags\":" + "[{\"key\":\"category\",\"value\":\"person\"},"
              + "{\"key\":\"day\",\"value\":\"today\"}]}");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      if (siteId != null) {
        assertEquals(siteId, result.get("siteId"));
      } else {
        assertEquals("default", result.get("siteId"));
      }

      assertNotNull(result.get("id"));
      final String id = result.get("id").toString();

      assertNotNull(result.get("insertedDate"));
      assertEquals("joe smith", result.get("name"));
      assertEquals("test@formkiq.com", result.get("userId"));
      verifyUrl(siteId, id, result);

      WebhooksService webhookService = getAwsServices().webhookService();
      DynamicObject obj = webhookService.findWebhook(siteId, id);
      assertEquals("joe smith", obj.getString("path"));
      assertEquals("test@formkiq.com", obj.getString("userId"));

      PaginationResults<DynamicObject> tags = webhookService.findTags(siteId, id, null);
      assertEquals(2, tags.getResults().size());

      DynamicObject tag = tags.getResults().get(0);

      assertEquals(id, tag.getString("documentId"));
      assertEquals("USERDEFINED", tag.getString("type"));
      assertEquals("test@formkiq.com", tag.getString("userId"));
      assertEquals("category", tag.getString("tagKey"));
      assertEquals("person", tag.getString("tagValue"));
      assertNotNull(tag.getString("inserteddate"));

      tag = tags.getResults().get(1);
      assertEquals(id, tag.getString("documentId"));
      assertEquals("USERDEFINED", tag.getString("type"));
      assertEquals("test@formkiq.com", tag.getString("userId"));
      assertEquals("day", tag.getString("tagKey"));
      assertEquals("today", tag.getString("tagValue"));
      assertNotNull(tag.getString("inserteddate"));
    }
  }

  /**
   * POST /webhooks with NO SiteId Set.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks03() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    String siteId = UUID.randomUUID().toString();

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
    setCognitoGroup(event, siteId);

    event.setBody("{\"name\":\"joe smith\"}");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

    assertEquals(siteId, result.get("siteId"));

    assertNotNull(result.get("id"));
    final String id = result.get("id").toString();

    assertNotNull(result.get("insertedDate"));
    assertEquals("joe smith", result.get("name"));
    assertEquals("test@formkiq.com", result.get("userId"));
    verifyUrl(siteId, id, result);

    WebhooksService webhookService = getAwsServices().webhookService();
    DynamicObject obj = webhookService.findWebhook(siteId, id);
    assertEquals("joe smith", obj.getString("path"));
    assertEquals("test@formkiq.com", obj.getString("userId"));
  }

  /**
   * POST /webhooks too many webhooks.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks04() throws Exception {
    // given
    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    for (String maxWebHooks : Arrays.asList("2", "0")) {

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

        if (!"0".equals(maxWebHooks)) {
          configService.save(siteId, new DynamicObject(Map.of(MAX_WEBHOOKS, maxWebHooks)));
        } else {
          configService.save(null, new DynamicObject(Map.of(MAX_WEBHOOKS, maxWebHooks)));
        }

        String response = null;

        for (int i = 0; i <= 2; i++) {
          ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
          addParameter(event, "siteId", siteId);
          event.setBody("{\"name\":\"john smith\"}");

          // when
          response = handleRequest(event);
        }

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
        assertEquals("429.0", String.valueOf(m.get("statusCode")));
        assertEquals("{\"message\":\"Reached max number of webhooks\"}", m.get("body").toString());
      }
    }
  }

  /**
   * POST /webhooks with Config WEBHOOK_TIME_TO_LIVE.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks05() throws Exception {
    // given
    ConfigService configService = getAwsServices().getExtension(ConfigService.class);
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    ApiGatewayRequestEvent eventPost = toRequestEvent("/request-post-webhooks01.json");
    eventPost.setBody("{\"name\":\"john smith\"}");

    ApiGatewayRequestEvent event = toRequestEvent("/request-get-webhooks01.json");

    String siteId = null;
    String ttl = "87400";
    configService.save(siteId, new DynamicObject(Map.of(WEBHOOK_TIME_TO_LIVE, ttl)));

    // when
    String responsePost = handleRequest(eventPost);
    final String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(responsePost, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

    final String id = verifyPostWebhooks05(result);

    m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));

    result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("webhooks");
    assertEquals(1, list.size());
    verifyPostWebhooks05(list.get(0));

    WebhooksService webhookService = getAwsServices().webhookService();
    DynamicObject obj = webhookService.findWebhook(null, id);

    long epoch = Long.parseLong(obj.getString("TimeToLive"));
    ZonedDateTime ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

    // given
    ttl = "-87400";
    configService.save(siteId, new DynamicObject(Map.of(WEBHOOK_TIME_TO_LIVE, ttl)));

    // when
    responsePost = handleRequest(eventPost);

    // then
    m = GsonUtil.getInstance().fromJson(responsePost, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("false", result.get("enabled"));
    assertNotNull(result.get("ttl"));
  }

  /**
   * POST /webhooks. Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks06() throws Exception {
    // given
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
    event.setBody("{\"name\":\"john smith\",\"enabled\":\"private\"}");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("default", result.get("siteId"));
    assertNotNull(result.get("id"));
    final String id = result.get("id").toString();

    assertNotNull(result.get("insertedDate"));
    assertEquals("john smith", result.get("name"));
    assertEquals("test@formkiq.com", result.get("userId"));
    assertEquals("http://localhost:8080/private/webhooks/" + id, result.get("url"));
  }

  private String verifyPostWebhooks05(final Map<String, Object> result) {
    assertEquals("default", result.get("siteId"));
    assertNotNull(result.get("id"));
    final String id = result.get("id").toString();

    assertNotNull(result.get("insertedDate"));
    assertEquals("john smith", result.get("name"));
    assertEquals("test@formkiq.com", result.get("userId"));
    assertEquals("true", result.get("enabled"));
    assertNotNull(result.get("ttl"));
    assertEquals("http://localhost:8080/public/webhooks/" + id, result.get("url"));

    return id;
  }

  private void verifyUrl(final String siteId, final String id, final Map<String, Object> result) {
    if (siteId == null) {
      assertEquals("http://localhost:8080/public/webhooks/" + id, result.get("url"));
      assertEquals("default", result.get("siteId"));
    } else {
      assertNotNull(result.get("siteId"));
      assertNotEquals("default", result.get("siteId"));
      assertEquals("http://localhost:8080/public/webhooks/" + id + "?siteId=" + siteId,
          result.get("url"));
    }
  }
}
