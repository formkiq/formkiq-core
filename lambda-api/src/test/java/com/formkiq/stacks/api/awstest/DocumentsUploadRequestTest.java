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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.DocumentContent;
import com.formkiq.stacks.client.requests.GetDocumentContentRequest;
import com.formkiq.stacks.client.requests.GetDocumentUploadRequest;
import com.formkiq.stacks.common.formats.MimeType;

/**
 * GET, OPTIONS /documents/upload tests.
 *
 */
public class DocumentsUploadRequestTest extends AbstractApiTest {

  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** Random Site ID. */
  private static final String SITEID0 = UUID.randomUUID().toString();
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 30000;
  /** Sleep Timeout. */
  private static final int SLEEP = 1000;
  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Get Max Documents Ssm Key.
   * 
   * @return {@link String}
   */
  private static String getMaxDocumentsSsmKey() {
    return "/formkiq/" + getAppenvironment() + "/siteid/" + SITEID1 + "/MaxDocuments";
  }

  /**
   * Get Max Contength Length Ssm Key.
   * 
   * @return {@link String}
   */
  private static String getMaxContentLengthSsmKey() {
    return "/formkiq/" + getAppenvironment() + "/siteid/" + SITEID0 + "/MaxContentLengthBytes";
  }

  /**
   * After Class.
   */
  @AfterClass
  public static void afterClass() {
    removeParameterStoreValue(getMaxContentLengthSsmKey());
    removeParameterStoreValue(getMaxDocumentsSsmKey());
  }

  /**
   * Get Request Upload Document Url.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String content = "<html><body>test content</body></html>";
      GetDocumentUploadRequest request =
          new GetDocumentUploadRequest().contentLength(content.length());

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      Map<String, Object> map = toMap(response);
      assertNotNull(map.get("url"));
      assertNotNull(map.get("documentId"));

      // given
      final String documentId = map.get("documentId").toString();
      String url = map.get("url").toString();

      // when
      response = this.http.send(HttpRequest.newBuilder(new URI(url))
          .header("Content-Type", MimeType.MIME_HTML.getContentType())
          .method("PUT", BodyPublishers.ofString(content)).build(), BodyHandlers.ofString());

      // then
      assertEquals(STATUS_OK, response.statusCode());

      DocumentContent documentContent =
          client.getDocumentContent(new GetDocumentContentRequest().documentId(documentId));
      while (!content.equals(documentContent.content())) {
        Thread.sleep(SLEEP);
        documentContent =
            client.getDocumentContent(new GetDocumentContentRequest().documentId(documentId));
      }

      assertEquals(content, documentContent.content());
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
      // when
      HttpResponse<String> response = client.optionsDocumentUpload();
      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Get Request Upload Document Url, Content Length missing.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    // given
    putParameter(getMaxContentLengthSsmKey(), "5");
    for (FormKiqClientV1 client : getFormKiqClients()) {

      GetDocumentUploadRequest request = new GetDocumentUploadRequest().siteId(SITEID0);

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertRequestCorsHeaders(response.headers());
      assertEquals("{\"message\":\"'contentLength' is required\"}", response.body());
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet03() throws Exception {
    // given
    final int contentLength = 100;
    putParameter(getMaxContentLengthSsmKey(), "5");
    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID0).contentLength(contentLength);

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertRequestCorsHeaders(response.headers());
      assertEquals("{\"message\":\"'contentLength' cannot exceed 5 bytes\"}", response.body());
    }
  }

  /**
   * Get Request Upload Document Url, Content Length Greater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet04() throws Exception {
    // given
    final int contentLength = 5;
    putParameter(getMaxContentLengthSsmKey(), "" + contentLength);

    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID0).contentLength(contentLength);

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      HttpResponse<String> response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Get Request Upload Document Url, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    putParameter(getMaxDocumentsSsmKey(), "1");

    GetDocumentUploadRequest request =
        new GetDocumentUploadRequest().siteId(SITEID1).contentLength(1);

    HttpResponse<String> response =
        getFormKiqClients().get(0).getDocumentUploadAsHttpResponse(request);
    assertEquals(STATUS_OK, response.statusCode());

    for (FormKiqClientV1 client : getFormKiqClients()) {

      // when
      response = client.getDocumentUploadAsHttpResponse(request);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertEquals("{\"message\":\"Max Number of Documents reached\"}", response.body());
      assertRequestCorsHeaders(response.headers());
    }
  }
}
