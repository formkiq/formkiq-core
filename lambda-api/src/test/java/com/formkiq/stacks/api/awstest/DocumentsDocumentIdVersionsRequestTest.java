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
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentVersionsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentVersionsRequest;

/**
 * GET, OPTIONS /documents/{documentId}/versions tests.
 *
 */
public class DocumentsDocumentIdVersionsRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Get Request Upload Document Versions.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    final int status200 = 200;

    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = addDocumentWithoutFile(client);
      GetDocumentVersionsRequest request = new GetDocumentVersionsRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.getDocumentVersionsAsHttpResponse(request);

      // then
      assertEquals(status200, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      Map<String, Object> map = toMap(response);
      List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("versions");
      assertEquals(1, list.size());

      map = list.get(0);
      assertNotNull(list.get(0).get("versionId"));
      assertNotNull(list.get(0).get("lastModifiedDate"));
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
      OptionsDocumentVersionsRequest req =
          new OptionsDocumentVersionsRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.optionsDocumentVersions(req);
      // then
      final int status = 200;
      assertEquals(status, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
