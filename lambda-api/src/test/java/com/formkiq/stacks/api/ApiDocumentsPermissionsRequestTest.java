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
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventBuilder;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /documents/{documentId}/permissions. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsPermissionsRequestTest extends AbstractRequestHandler {

  /**
   * Delete /documents/{documentId}/permissions/{versionKey} request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param permissionKey {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent deletePermissionsRequest(final String siteId,
      final String documentId, final String permissionKey) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("delete")
        .resource("/documents/{documentId}/permissions/{permissionKey}")
        .path("/documents/" + documentId + "/permissions/" + permissionKey).user("joesmith")
        .group(siteId != null ? siteId : "default")
        .pathParameters(Map.of("documentId", documentId, "permissionKey", permissionKey))
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * GET /documents/{documentId}/permissions request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getRequest(final String siteId, final String documentId,
      final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("GET")
        .resource("/documents/{documentId}/permissions")
        .path("/documents/" + documentId + "/permissions").group(group).user("joesmith")
        .pathParameters(Map.of("documentId", documentId))
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).build();
    return event;
  }

  /**
   * POST /documents/{documentId}/permissions request.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param group {@link String}
   * @param body {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent postRequest(final String siteId, final String documentId,
      final String group, final String body) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEventBuilder().method("POST")
        .resource("/documents/{documentId}/permissions")
        .path("/documents/" + documentId + "/permissions").group(group).user("joesmith")
        .pathParameters(Map.of("documentId", documentId))
        .queryParameters(siteId != null ? Map.of("siteId", siteId) : null).body(body).build();
    return event;
  }

  /**
   * Delete /documents/{documentId}/permissions/{permissionKey} request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleDeleteDocumentPermissions01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          deletePermissionsRequest(siteId, documentId, UUID.randomUUID().toString());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("402.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    }
  }

  /**
   * Get /documents/{documentId}/permissions request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentPermissions01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          getRequest(siteId, documentId, siteId != null ? siteId : DEFAULT_SITE_ID);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("402.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    }
  }

  /**
   * POST /documents/{documentId}/permissions request.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandlePostDocumentPermissions01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          postRequest(siteId, documentId, siteId != null ? siteId : DEFAULT_SITE_ID, "{}");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("402.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    }
  }
}
