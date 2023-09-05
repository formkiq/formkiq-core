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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

/** Unit Tests for request /documents/{documentId}/content. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentIdContentGetRequestHandlerTest extends AbstractRequestHandler {

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests S3 URL is returned.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent01() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      String userId = "jsmith";

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> body = fromJson(m.get("body").toString(), Map.class);
      String url = body.get("contentUrl").toString();

      assertTrue(url.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
      assertTrue(url.contains("X-Amz-Expires="));
      assertTrue(url.contains(AWS_REGION.toString()));
      assertEquals("application/octet-stream", body.get("contentType"));

      if (siteId != null) {
        assertTrue(url.startsWith(TestServices.getEndpointOverride(Service.S3).toString()
            + "/testbucket/" + siteId + "/" + documentId));
      } else {
        assertTrue(url.startsWith(
            TestServices.getEndpointOverride(Service.S3).toString() + "/testbucket/" + documentId));
      }
    }
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Document not found.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentContent02() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("404.0", String.valueOf(m.get("statusCode")));
    }
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests Text Content is returned (content-type plain/text).
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent03() throws Exception {
    testReturnContent("text/plain");
  }

  /**
   * /documents/{documentId}/content request.
   * 
   * Tests Text Content is returned (content-type application/x-www-form-urlencoded).
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleGetDocumentContent04() throws Exception {
    testReturnContent("application/x-www-form-urlencoded");
  }

  /**
   * Test Content is returned.
   * 
   * @param contentType {@link String}
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  private void testReturnContent(final String contentType) throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();

      String content = "this is a test";
      String s3key = createS3Key(siteId, documentId);
      getS3().putObject(BUCKET_NAME, s3key, content.getBytes(StandardCharsets.UTF_8), null);

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-content01.json");
      addParameter(event, "siteId", siteId);
      setPathParameter(event, "documentId", documentId);

      String userId = "jsmith";
      DocumentItemDynamoDb item = new DocumentItemDynamoDb(documentId, new Date(), userId);
      item.setContentType(contentType);
      getDocumentService().saveDocument(siteId, item, new ArrayList<>());

      // when
      String response = handleRequest(event);

      // then
      Map<String, Object> m = fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));

      Map<String, Object> body = fromJson(m.get("body").toString(), Map.class);
      assertEquals(content, body.get("content"));
      assertEquals(contentType, body.get("contentType"));
      assertEquals("false", body.get("isBase64").toString());
    }
  }
}
