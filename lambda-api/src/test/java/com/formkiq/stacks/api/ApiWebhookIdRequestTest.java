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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /webhooks/{webhookId}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ApiWebhookIdRequestTest extends AbstractRequestHandler {

  /**
   * Delete /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteWebhooks01() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
    event.setQueryStringParameters(Map.of("siteId", "default"));

    String response = handleRequest(event);
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("default", result.get("siteId"));
    assertNotNull(result.get("webhookId"));

    String webhookId = result.get("webhookId").toString();

    event = toRequestEvent("/request-delete-webhooks-webhookid01.json");
    setPathParameter(event, "webhookId", webhookId);
    event.setQueryStringParameters(Map.of("siteId", "default"));

    // when
    response = handleRequest(event);

    // then
    m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  /**
   * Delete /webhooks/{webhookId} not found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteWebhooks02() throws Exception {
    // given
    String id = UUID.randomUUID().toString();

    ApiGatewayRequestEvent event = toRequestEvent("/request-delete-webhooks-webhookid01.json");
    setPathParameter(event, "webhookId", id);

    // when
    String response = handleRequest(event);

    // then
    Map<String, Object> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("404.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  /**
   * Delete /webhooks/{webhookId} and webhook tags.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteWebhooks03() throws Exception {
    // given
    ApiGatewayRequestEvent req = toRequestEvent("/request-post-webhooks01.json");
    req.setQueryStringParameters(Map.of("siteId", "default"));
    req.setBody("{\"name\":\"john smith\",tags:[{key:\"dynamodb\"}]}");

    String response = handleRequest(req);
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("default", result.get("siteId"));
    assertNotNull(result.get("webhookId"));
    String webhookId = result.get("webhookId").toString();

    ApiGatewayRequestEvent event = toRequestEvent("/request-delete-webhooks-webhookid01.json");
    event.setQueryStringParameters(Map.of("siteId", "default"));
    setPathParameter(event, "webhookId", webhookId);

    // when
    response = handleRequest(event);

    // then
    m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    event = toRequestEvent("/request-get-webhooks-webhookid-tags01.json");
    setPathParameter(event, "webhookId", webhookId);

    // when
    response = handleRequest(event);

    // then
    m = GsonUtil.getInstance().fromJson(response, Map.class);
    result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals(0, ((List<Object>) result.get("tags")).size());
  }

  /**
   * GET /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhook01() throws Exception {
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

      String response = handleRequest(event);
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("201.0", String.valueOf(m.get("statusCode")));
      Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
      assertNotNull(result.get("webhookId"));
      String webhookId = result.get("webhookId").toString();

      event = toRequestEvent("/request-get-webhooks-webhookid01.json");
      setPathParameter(event, "webhookId", webhookId);
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

      // when
      response = handleRequest(event);

      // then
      m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      if (siteId == null) {
        assertEquals("default", result.get("siteId"));
      } else {
        assertNotNull(result.get("siteId"));
        assertNotEquals("default", result.get("siteId"));
      }

      assertNotNull(result.get("id"));
      webhookId = result.get("id").toString();

      assertNotNull(result.get("insertedDate"));
      assertEquals("john smith", result.get("name"));
      assertEquals("test@formkiq.com", result.get("userId"));

      verifyUrl(siteId, webhookId, result, true);
    }
  }

  /**
   * GET /webhooks/{webhookId} with PRIVATE.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhook02() throws Exception {
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      event.setBody("{\"name\":\"john smith\",\"enabled\":\"private\"}");

      String response = handleRequest(event);
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
      assertEquals("201.0", String.valueOf(m.get("statusCode")));
      Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
      assertNotNull(result.get("webhookId"));
      String webhookId = result.get("webhookId").toString();

      event = toRequestEvent("/request-get-webhooks-webhookid01.json");
      setPathParameter(event, "webhookId", webhookId);
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

      // when
      response = handleRequest(event);

      // then
      m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);

      if (siteId == null) {
        assertEquals("default", result.get("siteId"));
      } else {
        assertNotNull(result.get("siteId"));
        assertNotEquals("default", result.get("siteId"));
      }

      assertNotNull(result.get("id"));
      webhookId = result.get("id").toString();

      assertNotNull(result.get("insertedDate"));
      assertEquals("john smith", result.get("name"));
      assertEquals("test@formkiq.com", result.get("userId"));

      verifyUrl(siteId, webhookId, result, false);
    }
  }

  private void verifyUrl(final String siteId, final String id, final Map<String, Object> result,
      final boolean publicUrl) {
    String path = publicUrl ? "/public" : "/private";
    if (siteId == null) {
      assertEquals("http://localhost:8080" + path + "/webhooks/" + id, result.get("url"));
      assertEquals("default", result.get("siteId"));
    } else {
      assertNotNull(result.get("siteId"));
      assertNotEquals("default", result.get("siteId"));
      assertEquals("http://localhost:8080" + path + "/webhooks/" + id + "?siteId=" + siteId,
          result.get("url"));
    }
  }

  /**
   * PATCH /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPatchWebhook01() throws Exception {
    Date date = new Date();
    putSsmParameter("/formkiq/" + FORMKIQ_APP_ENVIRONMENT + "/api/DocumentsPublicHttpUrl",
        "http://localhost:8080");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String id = getAwsServices().getExtension(WebhooksService.class).saveWebhook(siteId, "test",
          "joe", date, "true");

      ApiGatewayRequestEvent event = toRequestEvent("/request-patch-webhooks-webhookid01.json");
      setPathParameter(event, "webhookId", id);
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);
      event.setBody("{\"name\":\"john smith2\",\"enabled\":false}");

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      assertEquals("{\"message\":\"'" + id + "' object updated\"}", m.get("body"));

      // given
      event = toRequestEvent("/request-get-webhooks-webhookid01.json");
      setPathParameter(event, "webhookId", id);
      addParameter(event, "siteId", siteId != null ? siteId : DEFAULT_SITE_ID);

      // when
      response = handleRequest(event);

      // then
      m = GsonUtil.getInstance().fromJson(response, Map.class);

      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      Map<String, Object> result =
          GsonUtil.getInstance().fromJson(m.get("body").toString(), Map.class);

      if (siteId == null) {
        assertEquals("default", result.get("siteId"));
      } else {
        assertNotNull(result.get("siteId"));
        assertNotEquals("default", result.get("siteId"));
      }

      assertNotNull(result.get("id"));
      id = result.get("id").toString();

      assertNotNull(result.get("insertedDate"));
      assertEquals("john smith2", result.get("name"));
      assertEquals("joe", result.get("userId"));
      assertEquals("false", result.get("enabled"));

      verifyUrl(siteId, id, result, true);
    }
  }
}
