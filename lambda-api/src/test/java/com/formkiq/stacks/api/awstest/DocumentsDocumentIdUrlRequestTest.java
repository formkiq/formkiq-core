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
package com.formkiq.stacks.api.awstest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.DocumentVersions;
import com.formkiq.stacks.client.models.UpdateDocument;
import com.formkiq.stacks.client.requests.GetDocumentContentUrlRequest;
import com.formkiq.stacks.client.requests.GetDocumentVersionsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentUploadRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentRequest;

/**
 * GET, OPTIONS /documents/{documentId}/url tests.
 *
 */
public class DocumentsDocumentIdUrlRequestTest extends AbstractApiTest {

  /** 1/2 second sleep. */
  private static final int SLEEP = 500;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;
  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = addDocumentWithoutFile(client);
      Thread.sleep(SLEEP * 2);
      client.updateDocument(new UpdateDocumentRequest().documentId(documentId)
          .document(new UpdateDocument().content("new content", StandardCharsets.UTF_8)));

      GetDocumentVersionsRequest req = new GetDocumentVersionsRequest().documentId(documentId);
      DocumentVersions list = client.getDocumentVersions(req);

      while (list.versions().size() != 2) {
        Thread.sleep(SLEEP);
        list = client.getDocumentVersions(req);
      }

      assertEquals(2, list.versions().size());

      verifyDocumentContent(client, documentId, "new content", list.versions().get(0).versionId());
      verifyDocumentContent(client, documentId, "sample content",
          list.versions().get(1).versionId());
    }
  }

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = UUID.randomUUID().toString();
      OptionsDocumentUploadRequest req = new OptionsDocumentUploadRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.optionsDocumentUpload(req);
      // then
      final int status = 200;
      assertEquals(status, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Verify Document Content.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @param content {@link String}
   * @param versionId {@link String}
   * @throws Exception Exception
   */
  private void verifyDocumentContent(final FormKiqClientV1 client, final String documentId,
      final String content, final String versionId) throws Exception {
    final int status200 = 200;

    GetDocumentContentUrlRequest request =
        new GetDocumentContentUrlRequest().documentId(documentId).versionId(versionId);

    // when
    HttpResponse<String> response = client.getDocumentContentUrlAsHttpResponse(request);

    // then
    assertEquals(status200, response.statusCode());
    assertRequestCorsHeaders(response.headers());

    Map<String, Object> map = toMap(response);
    assertNotNull(map.get("url"));
    assertEquals(documentId, map.get("documentId"));

    // given
    String url = map.get("url").toString();

    // when
    response =
        this.http.send(HttpRequest.newBuilder().uri(new URI(url)).build(), BodyHandlers.ofString());

    // then
    assertEquals(status200, response.statusCode());
    assertEquals(content, response.body());
  }
}
