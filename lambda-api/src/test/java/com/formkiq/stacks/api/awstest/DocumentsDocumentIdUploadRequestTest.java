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
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentRequest;

/**
 * GET, OPTIONS /documents/{documentId}/upload tests.
 *
 */
public class DocumentsDocumentIdUploadRequestTest extends AbstractApiTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20000;

  /**
   * Get Request Document Not Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {

    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = UUID.randomUUID().toString();
      GetDocumentUploadRequest request =
          new GetDocumentUploadRequest().documentId(documentId).contentLength(1);

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);
      // then
      final int status = 404;
      assertEquals(status, response.statusCode());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Get Request Document Found.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = addDocumentWithoutFile(client);
      GetDocumentUploadRequest request =
          new GetDocumentUploadRequest().documentId(documentId).contentLength(1);

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);
      // then
      final int status = 200;
      assertEquals(status, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      Map<String, Object> map = toMap(response);
      assertNotNull(map.get("url"));
      assertEquals(documentId, map.get("documentId"));
    }
  }

  /**
   * Options Request.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = UUID.randomUUID().toString();
      OptionsDocumentRequest req = new OptionsDocumentRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.optionsDocument(req);
      // then
      final int status = 200;
      assertEquals(status, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }
}
