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
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;

/** Unit Tests for request POST /public/forms. */
public class ApiDocumentsPublicFormsRequestTest extends AbstractRequestHandler {

  /**
   * Post /public/documents without authentication.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testPostPublicForms01() throws Exception {
    // given
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());

    newOutstream();

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
    getMap().put("ENABLE_PUBLIC_URLS", "true");
    createApiRequestHandler(getMap());
    newOutstream();

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
    newOutstream();

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
