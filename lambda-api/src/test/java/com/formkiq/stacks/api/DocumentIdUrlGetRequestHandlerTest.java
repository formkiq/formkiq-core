/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.lambda.apigateway.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;

/** Unit Tests for request /documents/{documentId}/url. */
public class DocumentIdUrlGetRequestHandlerTest extends AbstractRequestHandler {

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
        newOutstream();

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
        assertTrue(resp.getUrl().contains(getAwsRegion().toString()));
        assertNull(resp.getNext());
        assertNull(resp.getPrevious());

        if (siteId != null) {
          assertTrue(resp.getUrl()
              .startsWith("http://localhost:4566/testbucket/" + siteId + "/" + documentId));
        } else {
          assertTrue(resp.getUrl().startsWith("http://localhost:4566/testbucket/" + documentId));
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
      newOutstream();

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
      assertTrue(resp.getUrl().contains(getAwsRegion().toString()));
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      if (siteId != null) {
        assertTrue(resp.getUrl()
            .startsWith("http://localhost:4566/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(resp.getUrl().startsWith("http://localhost:4566/testbucket/" + documentId));
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
      newOutstream();
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
      newOutstream();

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
