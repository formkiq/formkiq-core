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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.services.ConfigService;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

/** Unit Tests for uploading /documents. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsUploadRequestTest extends AbstractRequestHandler {

  /** {@link LocalStackContainer}. */
  private LocalStackContainer localstack = TestServices.getLocalStack();
  
  @Override
  @BeforeEach
  public void before() throws Exception {
    super.before();

    getAwsServices().configService().delete(null);
  }

  /**
   * Valid POST generate upload document signed url.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload01() throws Exception {
    // given
    for (String path : Arrays.asList(null, "/bleh/test.txt")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        ApiGatewayRequestEvent event =
            toRequestEvent("/request-get-documents-upload-documentid.json");
        addParameter(event, "siteId", siteId);
        addParameter(event, "path", path);

        // when
        String response = handleRequest(event);

        // then
        Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
        assertEquals("200.0", String.valueOf(m.get("statusCode")));
        ApiUrlResponse resp = expectResponse(response);
        assertFalse(resp.getUrl().contains("content-length"));

        if (siteId != null) {
          assertTrue(getLogger().containsString(
              "generated presign url: " + this.localstack.getEndpointOverride(Service.S3).toString()
                  + "/testbucket/" + siteId));
        } else {
          assertTrue(getLogger().containsString("generated presign url: "
              + this.localstack.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
        }

        assertTrue(getLogger().containsString("saving document: "));

        if (path != null) {
          assertTrue(getLogger().containsString(" on path /bleh/test.txt"));
        } else {
          assertTrue(getLogger().containsString(" on path " + null));
        }
      }
    }
  }

  /**
   * Valid PUT generate upload document signed url for new document.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid01.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * Valid PUT generate upload document signed url for existing document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date date = new Date();
      String documentId = UUID.randomUUID().toString();
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, date, "jsmith");

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      String response = handleRequest(event);

      // then
      ApiUrlResponse resp = expectResponse(response);

      assertTrue(getLogger().containsString("generated presign url: "
          + this.localstack.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
      assertTrue(getLogger().containsString("for document " + resp.getDocumentId()));

      assertEquals(documentId, resp.getDocumentId());
    }
  }

  /**
   * Valid PUT generate upload document signed url for NEW document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentsUpload04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid02.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      ApiUrlResponse resp = expectResponse(response);

      DocumentItem item = getDocumentService().findDocument(siteId, resp.getDocumentId());
      assertEquals(
          "AROAZB6IP7U6SDBIQTEUX:formkiq-docstack-unittest-api-ApiGatewayInvokeRole-IKJY8XKB0IUK",
          item.getUserId());
    }
  }

  /**
   * Valid POST generate upload document signed url with contentLength parameter.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload05() throws Exception {
    // given
    ApiGatewayRequestEvent event = toRequestEvent("/request-get-documents-upload-documentid.json");
    addParameter(event, "contentLength", "1000");

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    ApiUrlResponse resp = expectResponse(response);
    assertTrue(resp.getUrl().contains("content-length"));

    assertTrue(getLogger().containsString("generated presign url: "
        + this.localstack.getEndpointOverride(Service.S3).toString() + "/testbucket/"));
    assertTrue(getLogger().containsString("saving document: "));
    assertTrue(getLogger().containsString(" on path " + null));
  }

  /**
   * Content-Length > Max Content Length.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload06() throws Exception {

    String maxContentLengthBytes = "2783034";
    getAwsServices().configService().save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));
    
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      if (siteId != null) {
        getAwsServices().configService().save(siteId, new DynamicObject(
            Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));
      }

      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "contentLength", "2783035");

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"'contentLength' cannot exceed 2783034 bytes\"}", m.get("body"));
    }
  }

  /**
   * Content-Length is required.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentsUpload07() throws Exception {

    String maxContentLengthBytes = "2783034";
    getAwsServices().configService().save(null,
        new DynamicObject(Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      if (siteId != null) {
        getAwsServices().configService().save(siteId, new DynamicObject(
            Map.of(ConfigService.MAX_DOCUMENT_SIZE_BYTES, maxContentLengthBytes)));
      }

      // given
      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-upload-documentid.json");
      addParameter(event, "siteId", siteId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("400.0", String.valueOf(m.get("statusCode")));
      assertEquals("{\"message\":\"'contentLength' is required\"}", m.get("body"));
    }
  }

  /**
   * Verify Response.
   *
   * @param response {@link String}
   * @return {@link ApiUrlResponse}
   */
  private ApiUrlResponse expectResponse(final String response) {

    @SuppressWarnings("unchecked")
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("200.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    ApiUrlResponse resp = GsonUtil.getInstance().fromJson(m.get("body"), ApiUrlResponse.class);

    assertNull(resp.getNext());
    assertNull(resp.getPrevious());
    return resp;
  }
}
