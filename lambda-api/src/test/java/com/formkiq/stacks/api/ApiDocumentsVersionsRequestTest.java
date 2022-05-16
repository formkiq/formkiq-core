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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import software.amazon.awssdk.services.s3.S3Client;

/** Unit Tests for request /documents/{documentId}/versions. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class ApiDocumentsVersionsRequestTest extends AbstractRequestHandler {

  /**
   * Get /documents/{documentId}/versions request. With documents being updated twice.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentVersions01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId1 = UUID.randomUUID().toString();
      String documentId2 = UUID.randomUUID().toString();
      String s3key1 = createDatabaseKey(siteId, documentId1);
      String s3key2 = createDatabaseKey(siteId, documentId2);

      try (S3Client s3 = getS3().buildClient()) {

        getS3().putObject(s3, BUCKET_NAME, s3key1, "testdata1".getBytes(StandardCharsets.UTF_8),
            null);
        getS3().putObject(s3, BUCKET_NAME, s3key2, "testdata2".getBytes(StandardCharsets.UTF_8),
            null);

        getS3().putObject(s3, BUCKET_NAME, s3key1, "testdata3".getBytes(StandardCharsets.UTF_8),
            null);
      }

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-versions.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "tz", "-0600");
      setPathParameter(event, "documentId", documentId1);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      ApiDocumentVersionsResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentVersionsResponse.class);

      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      assertEquals(2, resp.getVersions().size());
      assertNotNull(resp.getVersions().get(0).getVersionId());
      assertNotNull(resp.getVersions().get(0).getLastModifiedDate());
      assertNotNull(resp.getVersions().get(1).getVersionId());
      assertNotNull(resp.getVersions().get(1).getLastModifiedDate());
    }
  }

  /**
   * Get /documents/{documentId}/versions request. No updated versions.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentVersions02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId1 = UUID.randomUUID().toString();
      String documentId2 = UUID.randomUUID().toString();
      String s3key1 = createDatabaseKey(siteId, documentId1);
      String s3key2 = createDatabaseKey(siteId, documentId2);

      try (S3Client s3 = getS3().buildClient()) {

        getS3().putObject(s3, BUCKET_NAME, s3key1, "testdata1".getBytes(StandardCharsets.UTF_8),
            null);
        getS3().putObject(s3, BUCKET_NAME, s3key2, "testdata2".getBytes(StandardCharsets.UTF_8),
            null);
      }

      ApiGatewayRequestEvent event =
          toRequestEvent("/request-get-documents-documentid-versions.json");
      addParameter(event, "siteId", siteId);
      addParameter(event, "tz", "-0600");
      setPathParameter(event, "documentId", documentId1);

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

      final int mapsize = 3;
      assertEquals(mapsize, m.size());
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));

      ApiDocumentVersionsResponse resp =
          GsonUtil.getInstance().fromJson(m.get("body"), ApiDocumentVersionsResponse.class);

      assertNull(resp.getNext());
      assertNull(resp.getPrevious());

      assertEquals(1, resp.getVersions().size());
      assertNotNull(resp.getVersions().get(0).getVersionId());
      assertNotNull(resp.getVersions().get(0).getLastModifiedDate());
    }
  }

  /**
   * Get /documents/{documentId}/versions invalid tz.
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleGetDocumentVersions03() throws Exception {
    // given
    String siteId = null;
    String documentId1 = UUID.randomUUID().toString();

    ApiGatewayRequestEvent event =
        toRequestEvent("/request-get-documents-documentid-versions.json");
    addParameter(event, "siteId", siteId);
    addParameter(event, "tz", "asdasda");
    setPathParameter(event, "documentId", documentId1);

    // when
    String response = handleRequest(event);

    // then
    Map<String, String> m = GsonUtil.getInstance().fromJson(response, Map.class);

    final int mapsize = 3;
    assertEquals(mapsize, m.size());
    assertEquals("400.0", String.valueOf(m.get("statusCode")));
    assertEquals(getHeaders(), "\"headers\":" + GsonUtil.getInstance().toJson(m.get("headers")));
    assertEquals("{\"message\":\"Invalid ID for ZoneOffset, invalid format: +asdasda\"}",
        m.get("body"));
  }
}
