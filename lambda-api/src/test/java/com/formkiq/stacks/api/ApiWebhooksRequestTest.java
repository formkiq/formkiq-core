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
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.common.objects.DynamicObject;
import com.formkiq.stacks.dynamodb.DocumentTag;
import com.formkiq.stacks.dynamodb.WebhooksService;

/** Unit Tests for request /webhooks. */
public class ApiWebhooksRequestTest extends AbstractRequestHandler {

  /** To Milliseconds. */
  private static final long TO_MILLIS = 1000L;
  
  /**
   * Delete /webhooks/{webhookId}.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteWebhooks01() throws Exception {
    // given
    String response = handleRequest(toRequestEvent("/request-post-webhooks01.json"));
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    Map<String, Object> result = GsonUtil.getInstance().fromJson(m.get("body"), Map.class);
    assertEquals("default", result.get("siteId"));
    assertNotNull(result.get("id"));
    final String id = result.get("id").toString();
    
    newOutstream();
    
    ApiGatewayRequestEvent event = toRequestEvent("/request-delete-webhooks-webhookid01.json");
    setPathParameter(event, "webhookId", id);

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
    putSsmParameter("/formkiq/" + getAppenvironment() + "/api/DocumentsHttpUrl", "http://localhost:8080");
    
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      
      newOutstream();
      
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
      
      verifyUrl(siteId, id, result);
      
      newOutstream();
      
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
  
      final int expectedCount = 6;
      assertEquals(expectedCount, o.get().size());
      assertNotNull(o.get().get("insertedDate"));
      assertNotNull(o.get().get("id"));
      assertEquals("john smith", o.get().get("name"));
      
      verifyUrl(siteId, id, o.get());
      
      assertEquals("test@formkiq.com", o.get().get("userId"));
    }
  }

  /**
   * POST /webhooks.
   * Test TTL.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostWebhooks01() throws Exception {
    // given
    putSsmParameter("/formkiq/" + getAppenvironment() + "/api/DocumentsHttpUrl", "http://localhost:8080");
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks01.json");
    String ttl = "87400";
    event.setBody("{\"name\":\"john smith\",\"ttl\":\"" + ttl + "\"}");
    
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
    assertEquals("http://localhost:8080/public/webhooks/" + id, result.get("url"));
    
    WebhooksService webhookService = getAwsServices().webhookService();
    DynamicObject obj = webhookService.findWebhook(null, id);
    
    long epoch = Long.parseLong(obj.getString("TimeToLive"));
    ZonedDateTime ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());

    // given
    DocumentTag tag = new DocumentTag(id, "category", "person", new Date(), "joe");
    
    // when
    webhookService.addTags(null, id, Arrays.asList(tag), null);
    
    // then
    assertNull(webhookService.findTag(null, id, "category").getString("TimeToLive"));
    
    // update ttl/name
    newOutstream();
    
    // given
    ttl = "174800";
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
    assertEquals("john smith2", obj.get("name"));
    
    epoch = Long.parseLong(obj.getString("TimeToLive"));
    ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());
    
    DynamicObject dtag = webhookService.findTag(null, id, "category");
    assertNotNull(dtag.getString("TimeToLive"));
    epoch = Long.parseLong(dtag.getString("TimeToLive"));
    ld = Instant.ofEpochMilli(epoch * TO_MILLIS).atZone(ZoneOffset.UTC);
    now = ZonedDateTime.now(ZoneOffset.UTC).plusDays(2);
    assertEquals(now.getDayOfMonth(), ld.getDayOfMonth());
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
