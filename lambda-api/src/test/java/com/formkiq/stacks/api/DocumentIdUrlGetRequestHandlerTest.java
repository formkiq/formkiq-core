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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiResponseError;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.DocumentFormat;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.impl.bootstrap.HttpServer;

/** Unit Tests for request /documents/{documentId}/url. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class DocumentIdUrlGetRequestHandlerTest extends AbstractRequestHandler {

  /** {@link HttpServer}. */
  private HttpService http = new HttpServiceJdk11();

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

        String filename = "file_" + UUID.randomUUID() + ".pdf";
        DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
        item.setPath("/somepath/" + filename);
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

        URI uri = new URI(resp.getUrl());
        assertTrue(uri.getQuery().contains("filename=\"" + filename + "\""));

        assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
        assertTrue(resp.getUrl().contains("X-Amz-Expires=172800"));
        assertTrue(resp.getUrl().contains(AWS_REGION.toString()));
        assertNull(resp.getNext());
        assertNull(resp.getPrevious());

        if (siteId != null) {
          assertTrue(resp.getUrl().contains("/" + siteId + "/" + documentId));
        } else {
          assertTrue(resp.getUrl().contains("/" + documentId));
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
        assertTrue(resp.getUrl().contains("/" + siteId + "/" + documentId));
      } else {
        assertTrue(resp.getUrl().contains("/" + documentId));
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

  /**
   * /documents/{documentId}/url request deepLinkPath to another bucket.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent05() throws Exception {

    if (!getS3().exists("anotherbucket")) {
      getS3().createBucket("anotherbucket");
    }

    byte[] content = "Some data".getBytes(StandardCharsets.UTF_8);
    getS3().putObject("anotherbucket", "somefile.txt", content, "text/plain");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid-url02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String userId = "jsmith";
      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setDeepLinkPath("s3://anotherbucket/somefile.txt");
      doc.setContentType("text/plain");
      getDocumentService().saveDocument(siteId, doc, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiUrlResponse resp = fromJson(m.get("body"), ApiUrlResponse.class);

      assertTrue(resp.getUrl().contains("/anotherbucket/"));
      assertTrue(resp.getUrl().contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(resp.getUrl().contains("X-Amz-Expires=28800"));
      assertTrue(resp.getUrl().contains(AWS_REGION.toString()));
      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      if (siteId != null) {
        assertFalse(resp.getUrl().contains("/" + siteId + "/" + documentId));
      } else {
        assertFalse(resp.getUrl().contains("/" + documentId));
      }

      assertTrue(resp.getUrl().contains("somefile.txt"));
      assertEquals("Some data", getS3().getContentAsString("anotherbucket", "somefile.txt", null));
      assertEquals("Some data",
          this.http.get(resp.getUrl(), Optional.empty(), Optional.empty()).body());
    }
  }

  /**
   * /documents/{documentId}/url request deepLinkPath to http url.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent06() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-documentid-url02.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String userId = "jsmith";
      DocumentItemDynamoDb doc = new DocumentItemDynamoDb(documentId, new Date(), userId);
      doc.setDeepLinkPath("https://www.google.com/something/else.pdf");
      doc.setContentType("application/pdf");
      getDocumentService().saveDocument(siteId, doc, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
      ApiUrlResponse resp = fromJson(m.get("body"), ApiUrlResponse.class);

      assertEquals("https://www.google.com/something/else.pdf", resp.getUrl());
    }
  }
}
