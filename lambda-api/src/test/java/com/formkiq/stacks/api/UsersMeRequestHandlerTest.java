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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /users/me. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class UsersMeRequestHandlerTest extends AbstractRequestHandler {

  /**
   * Get /sites request.
   * 
   * @param siteId {@link String}
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("get")
        .resource("/users/me").path("/users/me").group(group).user("joesmith")
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * Get /users/me.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetUsersMe01() throws Exception {
    // given
    String siteId = "default Admins finance";
    ApiGatewayRequestEvent event = getRequest(null, siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));
    assertEquals("joesmith", resp.getString("username"));
    List<DynamicObject> sites = resp.getList("sites");
    assertEquals(2, sites.size());
    assertEquals("default", sites.get(0).getString("siteId"));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        sites.get(0).getStringList("permissions").stream().collect(Collectors.joining(",")));

    assertEquals("finance", sites.get(1).getString("siteId"));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        sites.get(1).getStringList("permissions").stream().collect(Collectors.joining(",")));
  }

  /**
   * Get /users/me - SAML.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetUsersMe02() throws Exception {
    // given
    createApiRequestHandler("saml");
    String siteId = "formkiq_default formkiq_admins formkiq_finance";
    ApiGatewayRequestEvent event = getRequest(null, siteId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));
    assertEquals("joesmith", resp.getString("username"));
    List<DynamicObject> sites = resp.getList("sites");
    assertEquals(2, sites.size());
    assertEquals("default", sites.get(0).getString("siteId"));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        sites.get(0).getStringList("permissions").stream().collect(Collectors.joining(",")));

    assertEquals("finance", sites.get(1).getString("siteId"));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        sites.get(1).getStringList("permissions").stream().collect(Collectors.joining(",")));
  }
}
