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

import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.api.util.GsonUtil;
import software.amazon.awssdk.services.s3.S3Client;

/** Unit Tests for request /documents/{documentId}/versions. */
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
      newOutstream();

      // given
      String documentId1 = UUID.randomUUID().toString();
      String documentId2 = UUID.randomUUID().toString();
      String s3key1 = createDatabaseKey(siteId, documentId1);
      String s3key2 = createDatabaseKey(siteId, documentId2);

      try (S3Client s3 = getS3().buildClient()) {

        getS3().putObject(s3, getBucketName(), s3key1, "testdata1".getBytes(StandardCharsets.UTF_8),
            null);
        getS3().putObject(s3, getBucketName(), s3key2, "testdata2".getBytes(StandardCharsets.UTF_8),
            null);

        getS3().putObject(s3, getBucketName(), s3key1, "testdata3".getBytes(StandardCharsets.UTF_8),
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
      newOutstream();

      // given
      String documentId1 = UUID.randomUUID().toString();
      String documentId2 = UUID.randomUUID().toString();
      String s3key1 = createDatabaseKey(siteId, documentId1);
      String s3key2 = createDatabaseKey(siteId, documentId2);

      try (S3Client s3 = getS3().buildClient()) {

        getS3().putObject(s3, getBucketName(), s3key1, "testdata1".getBytes(StandardCharsets.UTF_8),
            null);
        getS3().putObject(s3, getBucketName(), s3key2, "testdata2".getBytes(StandardCharsets.UTF_8),
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
    newOutstream();

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
