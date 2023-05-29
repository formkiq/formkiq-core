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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /sites. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiKeysRequestTest extends AbstractRequestHandler {

  /**
   * DELETE /configs/apiKey request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param apiKey {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent deleteRequest(final String siteId, final String group,
      final String apiKey) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("delete")
        .resource("/configs/apiKey").path("/configs/apiKey").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null)
        .queryParameters(Map.of("apiKey", apiKey)).build();
    return event;
  }

  /**
   * Get /configs/apiKeys request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("get")
        .resource("/configs/apiKeys").path("/configs/apiKeys").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * POST /configs/apiKey request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent postRequest(final String siteId, final String group,
      final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("post")
        .resource("/configs/apiKey").path("/configs/apiKey").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
    return event;
  }

  /**
   * Delete /configs/apiKey default as User.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteApiKeys01() throws Exception {
    // given
    String siteId = null;
    String group = "default";

    ApiGatewayRequestEvent event = deleteRequest(siteId, group, "ABC");

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
   * POST /configs/apiKey default as User.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostApiKeys01() throws Exception {
    // given
    String siteId = null;
    String group = "default";

    String body = GsonUtil.getInstance().toJson(Map.of("name", "test key"));
    ApiGatewayRequestEvent event = postRequest(siteId, group, body);

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
   * Get/POST/DELETE /configs/apiKey default as Admin.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetApiKeys01() throws Exception {
    // given
    String siteId = null;
    String group = "Admins";

    String body = GsonUtil.getInstance().toJson(Map.of("name", "test key"));
    ApiGatewayRequestEvent eventPost = postRequest(siteId, group, body);

    // when
    String responsePost = handleRequest(eventPost);

    // then
    Map<String, String> mpost = GsonUtil.getInstance().fromJson(responsePost, Map.class);
    verifyResponse(mpost);

    DynamicObject resp = new DynamicObject(fromJson(mpost.get("body"), Map.class));
    assertEquals(2, resp.size());
    assertEquals("test key", resp.getString("name"));
    assertNotNull(resp.getString("apiKey"));

    // given
    ApiGatewayRequestEvent eventGet = getRequest(siteId, group);

    // when
    String responseGet = handleRequest(eventGet);

    // then
    Map<String, String> mget = GsonUtil.getInstance().fromJson(responseGet, Map.class);
    verifyResponse(mget);

    resp = new DynamicObject(fromJson(mget.get("body"), Map.class));
    List<DynamicObject> list = resp.getList("apiKeys");
    assertEquals(1, list.size());
    String apiKey = list.get(0).get("apiKey").toString();
    assertTrue(apiKey.contains("**************"));
    assertEquals("test key", list.get(0).getString("name"));
    assertNotNull(list.get(0).get("insertedDate"));
    assertNull(list.get(0).get("PK"));
    assertNull(list.get(0).get("SK"));

    // given
    ApiGatewayRequestEvent eventDelete = deleteRequest(siteId, group, apiKey);

    // when
    String responseDelete = handleRequest(eventDelete);

    // then
    Map<String, String> mdelete = GsonUtil.getInstance().fromJson(responseDelete, Map.class);
    verifyResponse(mdelete);

    responseGet = handleRequest(eventGet);
    mget = GsonUtil.getInstance().fromJson(responseGet, Map.class);
    verifyResponse(mget);

    resp = new DynamicObject(fromJson(mget.get("body"), Map.class));
    list = resp.getList("apiKeys");
    assertEquals(0, list.size());
  }

  /**
   * Get /configs/apiKey default as User.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetApiKeys02() throws Exception {
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

  private void verifyResponse(final Map<String, String> m) {
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }
}
