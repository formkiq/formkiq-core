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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ConfigsRequestTest extends AbstractRequestHandler {

  /**
   * Get /configs request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("get")
        .resource("/configs").path("/configs").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * Patch /configs request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent patchRequest(final String siteId, final String group,
      final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("patch")
        .resource("/configs").path("/configs").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
    return event;
  }


  /**
   * Get /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites01() throws Exception {
    // given
    String siteId = null;
    String group = "Admins";

    ConfigService config = getAwsServices().getExtension(ConfigService.class);
    config.save(siteId, new DynamicObject(Map.of("somekey", "somevalue")));

    String body =
        GsonUtil.getInstance().toJson(Map.of("key", "anotherkey", "value", "anothervalue"));
    ApiGatewayRequestEvent eventPatch = patchRequest(siteId, group, body);

    ApiGatewayRequestEvent eventGet = getRequest(siteId, group);

    // when
    String responsePatch = handleRequest(eventPatch);
    String responseGet = handleRequest(eventGet);

    // then
    Map<String, String> mpatch = GsonUtil.getInstance().fromJson(responsePatch, Map.class);
    verifyResponse(mpatch);

    Map<String, String> mget = GsonUtil.getInstance().fromJson(responseGet, Map.class);
    verifyResponse(mget);

    DynamicObject resp = new DynamicObject(fromJson(mget.get("body"), Map.class));
    assertEquals(2, resp.size());
    assertEquals("somevalue", resp.getString("somekey"));
    assertEquals("anothervalue", resp.getString("anotherkey"));
  }

  /**
   * Get /config default as User.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites02() throws Exception {
    // given
    String siteId = null;
    String group = "default";

    ApiGatewayRequestEvent event = getRequest(siteId, group);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"message\":\"user is unauthorized\"}", String.valueOf(m.get("body")));
  }

  /**
   * Get /config for siteId, Config in default.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites03() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();
    String group = "Admins";

    ConfigService config = getAwsServices().getExtension(ConfigService.class);
    config.save(null, new DynamicObject(Map.of("somekey", "somevalue")));

    ApiGatewayRequestEvent event = getRequest(siteId, group);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    verifyResponse(m);

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));
    assertEquals(1, resp.size());
    assertEquals("somevalue", resp.getString("somekey"));
  }

  /**
   * Get /config for siteId, Config in siteId.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetSites04() throws Exception {
    // given
    String siteId = UUID.randomUUID().toString();
    String group = "Admins";

    ConfigService config = getAwsServices().getExtension(ConfigService.class);
    config.save(null, new DynamicObject(Map.of("somekey", "somevalue")));
    config.save(siteId, new DynamicObject(Map.of("anotherkey", "anothervalue")));

    ApiGatewayRequestEvent event = getRequest(siteId, group);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    verifyResponse(m);

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));
    assertEquals(1, resp.size());
    assertEquals("anothervalue", resp.getString("anotherkey"));
  }

  /**
   * PUT /config default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePutSites01() throws Exception {
    // given
    String siteId = null;
    String group = "Admins";

    String body =
        GsonUtil.getInstance().toJson(Map.of("key", "anotherkey", "value", "anothervalue"));
    ApiGatewayRequestEvent eventPatch = patchRequest(siteId, group, body);

    ApiGatewayRequestEvent eventGet = getRequest(siteId, group);

    // when
    String responsePatch = handleRequest(eventPatch);
    String responseGet = handleRequest(eventGet);

    // then
    Map<String, String> mpatch = GsonUtil.getInstance().fromJson(responsePatch, Map.class);
    verifyResponse(mpatch);

    Map<String, String> mget = GsonUtil.getInstance().fromJson(responseGet, Map.class);
    verifyResponse(mget);

    DynamicObject resp = new DynamicObject(fromJson(mget.get("body"), Map.class));
    assertEquals(1, resp.size());
    assertEquals("anothervalue", resp.getString("anotherkey"));
  }

  /**
   * PUT /config default as user.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePutSites02() throws Exception {
    // given
    String siteId = null;
    String group = "default";

    String body =
        GsonUtil.getInstance().toJson(Map.of("key", "anotherkey", "value", "anothervalue"));
    ApiGatewayRequestEvent eventPatch = patchRequest(siteId, group, body);

    // when
    String responsePatch = handleRequest(eventPatch);

    // then
    final int mapsize = 3;
    Map<String, String> m = GsonUtil.getInstance().fromJson(responsePatch, Map.class);
    assertEquals(mapsize, m.size());
    assertEquals("401.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  private void verifyResponse(final Map<String, String> m) {
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }
}
