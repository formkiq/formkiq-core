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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import software.amazon.awssdk.services.s3.S3Client;

/** Unit Tests for request POST /public/webhooks. */
public class ApiPublicWebhooksRequestTest extends AbstractRequestHandler {

  /** Extension for FormKiQ config file. */
  private static final String FORMKIQ_DOC_EXT = ".fkb64";
  
  /**
   * Post /public/webhooks without authentication.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks01() throws Exception {
    // given    
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());

    newOutstream();
    String name = UUID.randomUUID().toString();

    String id = getAwsServices().webhookService().saveWebhook(null, name, "joe", null);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks01.json");
    setPathParameter(event, "webhooks", id);
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    
    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    String documentId = body.get("documentId").toString();
    assertNotNull(documentId);
    
    // verify s3 file
    try (S3Client s3 = getS3().buildClient()) {
      String json =
          getS3().getContentAsString(s3, getStages3bucket(), documentId + FORMKIQ_DOC_EXT, null);
      Map<String, Object> map = fromJson(json, Map.class);
      assertEquals(documentId, map.get("documentId"));
      assertEquals("webhook/" + name, map.get("userId"));
      assertEquals("webhooks/" + id, map.get("path"));
      assertEquals("{\"name\":\"john smith\"}", map.get("content"));
    }
  }

  /**
   * Post /public/webhooks without authentication BASE 64 ENCODED.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks02() throws Exception {
    // given    
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());

    newOutstream();
    
    String name = UUID.randomUUID().toString();
    String id = getAwsServices().webhookService().saveWebhook(null, name, "joe", null);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks02.json");
    setPathParameter(event, "webhooks", id);
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    
    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    String documentId = body.get("documentId").toString();
    assertNotNull(documentId);
    
    // verify s3 file
    try (S3Client s3 = getS3().buildClient()) {
      String json =
          getS3().getContentAsString(s3, getStages3bucket(), documentId + FORMKIQ_DOC_EXT, null);
      Map<String, Object> map = fromJson(json, Map.class);

      assertEquals(documentId, map.get("documentId"));
      assertEquals("webhook/" + name, map.get("userId"));
      assertEquals("webhooks/" + id, map.get("path"));
      assertEquals("application/json", map.get("contentType"));
      assertEquals("{\"name\":\"john smith\"}", map.get("content"));
    }
  }
  
  /**
   * Post /public/webhooks with NO BODY.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks03() throws Exception {
    // givens
    newOutstream();

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks03.json");
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("400.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }
  
  /**
   * Post /public/webhooks to expired webhook.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks04() throws Exception {
    // given    
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());

    newOutstream();
    String name = UUID.randomUUID().toString();

    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong("-1000"));
    Date ttl = Date.from(now.toInstant());
    String id = getAwsServices().webhookService().saveWebhook(null, name, "joe", ttl);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks01.json");
    setPathParameter(event, "webhooks", id);
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("400.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }
  
  /**
   * Post /public/webhooks to ttl webhook NOT expired.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks05() throws Exception {
    // given    
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());

    newOutstream();
    String name = UUID.randomUUID().toString();

    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(Long.parseLong("1000"));
    Date ttl = Date.from(now.toInstant());
    String id = getAwsServices().webhookService().saveWebhook(null, name, "joe", ttl);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-webhooks01.json");
    setPathParameter(event, "webhooks", id);
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    
    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    String documentId = body.get("documentId").toString();
    assertNotNull(documentId);
    
    DynamicObject obj = getAwsServices().webhookService().findWebhook(null, id);
    assertNotNull(obj.get("TimeToLive"));
    
    // verify s3 file
    try (S3Client s3 = getS3().buildClient()) {
      String json =
          getS3().getContentAsString(s3, getStages3bucket(), documentId + FORMKIQ_DOC_EXT, null);
      Map<String, Object> map = fromJson(json, Map.class);
      assertEquals(documentId, map.get("documentId"));
      assertEquals("webhook/" + name, map.get("userId"));
      assertEquals("webhooks/" + id, map.get("path"));
      assertEquals("{\"name\":\"john smith\"}", map.get("content"));
      assertEquals(obj.get("TimeToLive"), map.get("TimeToLive"));
    }
  }
}
