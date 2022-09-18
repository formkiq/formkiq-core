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

import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

/** Unit Tests for request /documents/{documentId}/url. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class DocumentIdUrlGetRequestHandlerTest extends AbstractRequestHandler {

  /** {@link LocalStackContainer}. */
  private LocalStackContainer localstack = TestServices.getLocalStack();

  /**
   * /documents/{documentId}/url request.
   * 
   * Tests No Content-Type, Content-Type matches DocumentItem's Content Type and Document Format
   * exists.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent01() throws Exception {

    for (String contentType : Arrays.asList(null, "application/pdf", "text/plain")) {

      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        String documentId = UUID.randomUUID().toString();
        String userId = "jsmith";

        if (contentType != null) {
          DocumentFormat format = new DocumentFormat();
          format.setContentType(contentType);
          format.setDocumentId(documentId);
          format.setInsertedDate(new Date());
          format.setUserId(userId);
          getDocumentService().saveDocumentFormat(siteId, format);
        }

        ApiGatewayRequestEvent event =
            toRequestEvent("/request-get-documents-documentid-url01.json");
        addParameter(event, "siteId", siteId);
        setPathParameter(event, "documentId", documentId);
        addHeader(event, "Content-Type", contentType);

        DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
        if ("text/plain".equals(contentType)) {
          item.setContentType(contentType);
        }

        getDocumentService().saveDocument(siteId, item, new ArrayList<>());

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = fromJson(response, Map.class);

        final int mapsize = 3;
        assertEquals(mapsize, m.size());
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        assertEquals(getHeaders(),
            "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
        ApiUrlResponse resp = fromJson(m.get("body"), ApiUrlResponse.class);

        assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(resp.getUrl().contains("X-Amz-Expires=172800"));
        assertTrue(resp.getUrl().contains(AWS_REGION.toString()));
        assertNull(resp.getNext());
        assertNull(resp.getPrevious());

        if (siteId != null) {
          assertTrue(
              resp.getUrl().startsWith(this.localstack.getEndpointOverride(Service.S3).toString()
                  + "/testbucket/" + siteId + "/" + documentId));
        } else {
          assertTrue(
              resp.getUrl().startsWith(this.localstack.getEndpointOverride(Service.S3).toString()
                  + "/testbucket/" + documentId));
        }
      }
    }
  }

  /**
   * /documents/{documentId}/url request w/ duration.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid-url02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String userId = "jsmith";
      getDocumentService().saveDocument(siteId,
          new DocumentItemDynamoDb(documentId, new Date(), userId), new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiUrlResponse resp = fromJson(m.get("body"), ApiUrlResponse.class);

      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      if (siteId != null) {
        assertTrue(
            resp.getUrl().startsWith(this.localstack.getEndpointOverride(Service.S3).toString()
                + "/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(
            resp.getUrl().startsWith(this.localstack.getEndpointOverride(Service.S3).toString()
                + "/testbucket/" + documentId));
      }
    }
  }

  /**
   * /documents/{documentId}/url request, document not found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid-url01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiResponseError resp = fromJson(m.get("body"), ApiResponseError.class);

      assertEquals("Document 39da0052-3cb3-47a6-94c7-7b97cd43f1ee not found.", resp.getMessage());
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());
    }
  }

  /**
   * /documents/{documentId}/url request.
   * 
   * Tests No Content-Type, Content-Type matches DocumentItem's Content Type and Document Format
   * exists.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid-url01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);
      addHeader(event, "Content-Type", "application/pdf");

      String userId = "jsmith";
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

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
}
