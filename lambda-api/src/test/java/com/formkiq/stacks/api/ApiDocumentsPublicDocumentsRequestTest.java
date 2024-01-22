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
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request POST /public/documents. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class ApiDocumentsPublicDocumentsRequestTest extends AbstractRequestHandler {

  /**
   * Post /public/documents without authentication.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostPublicDocuments01() throws Exception {
    // given
    Map<String, String> map = new HashMap<>(getMap());
    map.put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(map);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-documents03.json");
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

    Map<String, Object> body = fromJson(m.get("body"), Map.class);
    assertNotNull(body.get("documentId"));
    assertNotNull(body.get("uploadUrl"));
    List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
    assertEquals(mapsize, documents.size());

    assertNotNull(documents.get(0).get("documentId"));
    assertNull(documents.get(0).get("uploadUrl"));

    assertNotNull(documents.get(1).get("uploadUrl"));
    assertNotNull(documents.get(1).get("documentId"));

    assertNotNull(documents.get(2).get("uploadUrl"));
    assertNotNull(documents.get(2).get("documentId"));
  }

  /**
   * Post /public/documents with authentication.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostPublicForms02() throws Exception {
    // given
    Map<String, String> map = new HashMap<>(getMap());
    map.put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(map);

    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-documents03.json");
    setCognitoGroup(event, "admins");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("201.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }

  /**
   * Post /public/documents with disabled PUBLIC_URLS.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostPublicForms03() throws Exception {
    // givens
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-public-documents03.json");
    event.getRequestContext().setAuthorizer(new HashMap<>());
    event.getRequestContext().setIdentity(new HashMap<>());

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = fromJson(response, Map.class);
    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("404.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
  }
}
