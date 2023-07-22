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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.WebhooksService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /webhooks/{webhookId}/tags. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiWebhooksTagsRequestTest extends AbstractRequestHandler {

  /**
   * Get /webhooks/{webhookId}/tags empty.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhooks01() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-webhooks-webhookid-tags01.json");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"tags\":[]}", m.get("body"));
  }

  /**
   * Get /webhooks/{webhookId}/tags empty.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testGetWebhooks02() throws Exception {
    // given
    String webhookId = getAwsServices().getExtension(WebhooksService.class).saveWebhook(null,
        "testwebhook", "joe", null, "true");
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-webhooks-webhookid-tags01.json");
    setPathParameter(event, "webhookId", webhookId);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"message\":\"Created Tag 'category'.\"}", m.get("body"));

    // given
    event = toRequestEvent("/request-get-webhooks-webhookid-tags01.json");
    setPathParameter(event, "webhookId", webhookId);

    // when
    response = handleRequest(event);

    // then
    m = GsonUtil.getInstance().fromJson(response, Map.class);

    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    DynamicObject obj = new DynamicObject(body);

    List<DynamicObject> list = obj.getList("tags");
    assertEquals(1, list.size());

    assertEquals(webhookId, list.get(0).getString("webhookId"));
    assertEquals("USERDEFINED", list.get(0).getString("type"));
    assertEquals("8a73dfef-26d3-43d8-87aa-b3ec358e43ba@formkiq.com",
        list.get(0).getString("userId"));
    assertEquals("category", list.get(0).getString("key"));
    assertEquals("job", list.get(0).getString("value"));
    assertNotNull(list.get(0).getString("insertedDate"));
  }
}
