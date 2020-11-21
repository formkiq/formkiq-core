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
package com.formkiq.stacks.api.awstest;

import static com.formkiq.stacks.common.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Test;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.models.AddDocument;
import com.formkiq.stacks.client.models.AddDocumentResponse;
import com.formkiq.stacks.client.models.DocumentTag;
import com.formkiq.stacks.client.models.DocumentWithChildren;
import com.formkiq.stacks.client.models.UpdateDocument;
import com.formkiq.stacks.client.requests.AddDocumentRequest;
import com.formkiq.stacks.client.requests.DeleteDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentRequest;
import com.formkiq.stacks.common.formats.MimeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * GET, OPTIONS, POST /documents. Tests.
 *
 */
public class DocumentsRequestTest extends AbstractApiTest {

  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** 201 Created. */
  private static final int STATUS_CREATED = 201;
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** 403 Forbidden. */
  private static final int STATUS_FORBIDDEN = 403;
  /** 404 Not Found. */
  private static final int STATUS_NOT_FOUND = 404;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60000;
  /** 1 Second. */
  private static final int ONE_SECOND = 1000;
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();
  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * After Class.
   */
  @AfterClass
  public static void afterClass() {
    removeParameterStoreValue(getMaxDocumentsSsmKey());
  }

  /**
   * Get Max Documents Ssm Key.
   * 
   * @return {@link String}
   */
  private static String getMaxDocumentsSsmKey() {
    return "/formkiq/" + getAppenvironment() + "/siteid/" + SITEID1 + "/MaxDocuments";
  }

  /**
   * Fetch Document.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @return {@link Map}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  private Map<String, Object> fetchDocument(final FormKiqClientV1 client, final String documentId)
      throws IOException, InterruptedException, URISyntaxException {

    Map<String, Object> map = null;
    GetDocumentRequest request = new GetDocumentRequest().documentId(documentId);

    while (true) {

      HttpResponse<String> response = client.getDocumentAsHttpResponse(request);

      if (STATUS_OK == response.statusCode()) {
        assertEquals(STATUS_OK, response.statusCode());
        map = toMap(response);
        assertRequestCorsHeaders(response.headers());
        assertEquals(documentId, map.get("documentId"));
        assertEquals(DEFAULT_SITE_ID, map.get("siteId"));

        break;
      }

      Thread.sleep(ONE_SECOND);
    }

    return map;
  }

  /**
   * Get Document.
   * 
   * @param client {@link FormKiqClientV1}
   * @param documentId {@link String}
   * @param hasChildren boolean
   * @return {@link DocumentWithChildren}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  private DocumentWithChildren getDocument(final FormKiqClientV1 client, final String documentId,
      final boolean hasChildren) throws IOException, InterruptedException, URISyntaxException {

    GetDocumentRequest request = new GetDocumentRequest().documentId(documentId);

    while (true) {
      try {
        DocumentWithChildren document = client.getDocument(request);
        if (hasChildren && notNull(document.documents()).isEmpty()) {
          throw new IOException("documents not added yet");
        }
        return document;
      } catch (IOException e) {
        Thread.sleep(ONE_SECOND);
      }
    }
  }

  /**
   * Delete Not existing file.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testDelete01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      String documentId = UUID.randomUUID().toString();
      DeleteDocumentRequest request = new DeleteDocumentRequest().documentId(documentId);

      // when
      HttpResponse<String> response = client.deleteDocumentAsHttpResponse(request);

      // then
      assertEquals(STATUS_NOT_FOUND, response.statusCode());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Get Not existing file.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      GetDocumentsRequest request = new GetDocumentsRequest().date(new Date());
      // when
      HttpResponse<String> response = client.getDocumentsAsHttpResponse(request);

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());
      assertTrue(response.body().contains("\"documents\":["));
    }
  }

  /**
   * Get Not existing file. Test user with no roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    // given
    final String siteId = "finance";

    AuthenticationResultType token = login(READONLY_EMAIL, USER_PASSWORD);

    FormKiqClientConnection connection = new FormKiqClientConnection(getRootHttpUrl())
        .cognitoIdToken(token.idToken()).header("Origin", Arrays.asList("http://localhost"))
        .header("Access-Control-Request-Method", Arrays.asList("GET"));

    FormKiqClientV1 client = new FormKiqClientV1(connection);

    GetDocumentsRequest request = new GetDocumentsRequest().date(new Date());

    // when
    final HttpResponse<String> responseNoSiteId = client.getDocumentsAsHttpResponse(request);
    final HttpResponse<String> responseSiteId =
        client.getDocumentsAsHttpResponse(request.siteId(siteId));

    // then
    assertEquals(STATUS_OK, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().contains("\"documents\":["));

    assertEquals(STATUS_FORBIDDEN, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertEquals("{\"message\":\"Access Denied\"}", responseSiteId.body());
  }

  /**
   * Get Not existing file. Test user with 'USERS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet03() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = login(USER_EMAIL, USER_PASSWORD);

    FormKiqClientConnection connection = new FormKiqClientConnection(getRootHttpUrl())
        .cognitoIdToken(token.idToken()).header("Origin", Arrays.asList("http://localhost"))
        .header("Access-Control-Request-Method", Arrays.asList("GET"));

    FormKiqClientV1 client = new FormKiqClientV1(connection);
    GetDocumentsRequest request = new GetDocumentsRequest().date(new Date());

    // when
    final HttpResponse<String> responseNoSiteId = client.getDocumentsAsHttpResponse(request);
    final HttpResponse<String> responseSiteId =
        client.getDocumentsAsHttpResponse(request.siteId(siteId));

    // then
    assertEquals(STATUS_OK, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().contains("\"documents\":["));

    assertEquals(STATUS_FORBIDDEN, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertEquals("{\"message\":\"Access Denied\"}", responseSiteId.body());
  }

  /**
   * Get Not existing file. Test user with 'ADMINS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet04() throws Exception {
    // given
    final String siteId = "finance";
    FormKiqClientV1 client = createHttpClient(getAdminToken());
    GetDocumentsRequest request = new GetDocumentsRequest().date(new Date());

    // when
    final HttpResponse<String> responseNoSiteId = client.getDocumentsAsHttpResponse(request);
    final HttpResponse<String> responseSiteId =
        client.getDocumentsAsHttpResponse(request.siteId(siteId));

    // then
    assertEquals(STATUS_OK, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().contains("\"documents\":["));

    assertEquals(STATUS_OK, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertTrue(responseSiteId.body().contains("\"documents\":["));
  }

  /**
   * Get Not existing file. Test user with 'FINANCE' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = login(FINANCE_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    GetDocumentsRequest request = new GetDocumentsRequest().date(new Date());

    // when
    final HttpResponse<String> responseNoSiteId = client.getDocumentsAsHttpResponse(request);
    final HttpResponse<String> responseSiteId =
        client.getDocumentsAsHttpResponse(request.siteId(siteId));

    // then
    assertEquals(STATUS_OK, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().contains("\"documents\":["));

    assertEquals(STATUS_OK, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertTrue(responseSiteId.body().contains("\"documents\":["));
  }

  /**
   * Options.
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
      assertEquals(STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Options.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testOptions02() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      // when
      HttpResponse<String> response = client.optionsDocuments();

      // then
      assertEquals(STATUS_OK, response.statusCode());
      assertPreflightedCorsHeaders(response.headers());
    }
  }

  /**
   * Save new File.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost01() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      AddDocument post =
          new AddDocument().contentType("text/plain").content("test data", StandardCharsets.UTF_8);
      AddDocumentRequest req = new AddDocumentRequest().document(post);

      // when
      HttpResponse<String> response = client.addDocumentAsHttpResponse(req);
      assertEquals(STATUS_CREATED, response.statusCode());
      Map<String, Object> map = toMap(response);

      // given
      String documentId = map.get("documentId").toString();

      // when - fetch document
      while (true) {
        map = fetchDocument(client, documentId);
        if (map.containsKey("contentType")) {
          assertTrue(map.get("contentType").toString().startsWith("text/plain"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      // given
      UpdateDocument updateDocument = new UpdateDocument()
          .content("dummy data", StandardCharsets.UTF_8).contentType("application/pdf");
      UpdateDocumentRequest request = new UpdateDocumentRequest().document(updateDocument);
      request.documentId(documentId);

      // when - patch document
      response = client.updateDocumentAsHttpResponse(request);
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      // when - check content type changed
      while (true) {
        map = fetchDocument(client, documentId);

        if (map.containsKey("contentLength")
            && "application/pdf".equals(map.get("contentType").toString())) {
          assertEquals("application/pdf", map.get("contentType").toString());
          assertNotNull(map.get("contentLength"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      // given
      DeleteDocumentRequest delRequest = new DeleteDocumentRequest().documentId(documentId);
      GetDocumentRequest getRequest = new GetDocumentRequest().documentId(documentId);

      // when - delete document
      response = client.deleteDocumentAsHttpResponse(delRequest);
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      while (true) {
        // when - fetch document
        response = client.getDocumentAsHttpResponse(getRequest);
        // then
        if (STATUS_NOT_FOUND == response.statusCode()) {
          assertEquals(STATUS_NOT_FOUND, response.statusCode());
          assertRequestCorsHeaders(response.headers());
          break;
        }

        Thread.sleep(ONE_SECOND);
      }
    }
  }

  /**
   * Save new File as Readonly user.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost02() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = login(READONLY_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post);

    // when
    final HttpResponse<String> responseNoSiteId = client.addDocumentAsHttpResponse(req);
    final HttpResponse<String> responseSiteId =
        client.addDocumentAsHttpResponse(req.siteId(siteId));

    // then
    assertEquals(STATUS_FORBIDDEN, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertEquals("{\"message\":\"Access Denied\"}", responseNoSiteId.body());

    assertEquals(STATUS_FORBIDDEN, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertEquals("{\"message\":\"Access Denied\"}", responseSiteId.body());
  }

  /**
   * Save new File as 'USERS' group.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost03() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = login(USER_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post);

    // when
    final HttpResponse<String> responseNoSiteId = client.addDocumentAsHttpResponse(req);
    final HttpResponse<String> responseSiteId =
        client.addDocumentAsHttpResponse(req.siteId(siteId));

    // then
    assertEquals(STATUS_CREATED, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().startsWith("{\"documentId\":\""));

    assertEquals(STATUS_FORBIDDEN, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertEquals("{\"message\":\"Access Denied\"}", responseSiteId.body());
  }

  /**
   * Save new File as 'ADMINS' group.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost04() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = getAdminToken();
    FormKiqClientV1 client = createHttpClient(token);
    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post);

    // when
    final HttpResponse<String> responseNoSiteId = client.addDocumentAsHttpResponse(req);
    final HttpResponse<String> responseSiteId =
        client.addDocumentAsHttpResponse(req.siteId(siteId));

    // then
    assertEquals(STATUS_CREATED, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().startsWith("{\"documentId\":\""));

    assertEquals(STATUS_CREATED, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    Map<String, Object> map = toMap(responseSiteId.body());
    assertNotNull(map.get("documentId"));
    assertEquals(siteId, map.get("siteId"));
  }

  /**
   * Save new File as 'FINANCE' group.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost05() throws Exception {
    // given
    final String siteId = "finance";
    AuthenticationResultType token = login(FINANCE_EMAIL, USER_PASSWORD);
    FormKiqClientV1 client = createHttpClient(token);
    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post);

    // when
    final HttpResponse<String> responseNoSiteId = client.addDocumentAsHttpResponse(req);
    final HttpResponse<String> responseSiteId =
        client.addDocumentAsHttpResponse(req.siteId(siteId));

    // then
    assertEquals(STATUS_CREATED, responseNoSiteId.statusCode());
    assertRequestCorsHeaders(responseNoSiteId.headers());
    assertTrue(responseNoSiteId.body().startsWith("{\"documentId\":\""));

    assertEquals(STATUS_CREATED, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    Map<String, Object> map = toMap(responseSiteId.body());
    assertNotNull(map.get("documentId"));
    assertEquals(siteId, map.get("siteId"));
  }

  /**
   * Post /documents, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost06() throws Exception {
    // given
    putParameter(getMaxDocumentsSsmKey(), "1");

    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post).siteId(SITEID1);

    FormKiqClientV1 c = getFormKiqClients().get(0);

    HttpResponse<String> response = c.addDocumentAsHttpResponse(req);
    assertEquals(STATUS_CREATED, response.statusCode());

    for (FormKiqClientV1 client : getFormKiqClients()) {
      // when
      response = client.addDocumentAsHttpResponse(req);

      // then
      assertEquals(STATUS_BAD_REQUEST, response.statusCode());
      assertEquals("{\"message\":\"Max Number of Documents reached\"}", response.body());
      assertRequestCorsHeaders(response.headers());
    }
  }

  /**
   * Save document with subdocuments against private/public endpoints.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost07() throws Exception {

    for (boolean enablePublicEndpoint : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

      for (FormKiqClientV1 client : getFormKiqClients()) {
        // given
        AddDocument post = new AddDocument()
            .tags(Arrays.asList(new DocumentTag().key("formName").value("Job Application Form")))
            .documents(Arrays.asList(new AddDocument().contentType("application/json")
                .content("{\"firstName\": \"Jan\",\"lastName\": \"Doe\"")
                .tags(Arrays.asList(new DocumentTag().key("formData")))));

        AddDocumentRequest req =
            new AddDocumentRequest().document(post).enablePublicEndpoint(enablePublicEndpoint);

        // when
        AddDocumentResponse response = client.addDocument(req);

        // then
        assertNotNull(response.documentId());
        assertNotNull(response.uploadUrl());

        assertEquals(1, response.documents().size());
        assertNotNull(response.documents().get(0).documentId());
        assertNull(response.documents().get(0).uploadUrl());

        // given
        String content = "this is a test";

        // when
        HttpResponse<String> httpresponse =
            this.http.send(
                HttpRequest.newBuilder(new URI(response.uploadUrl()))
                    .header("Content-Type", MimeType.MIME_HTML.getContentType())
                    .method("PUT", BodyPublishers.ofString(content)).build(),
                BodyHandlers.ofString());

        // then
        assertEquals(STATUS_OK, httpresponse.statusCode());

        // given
        String documentId = response.documentId();

        // when - fetch document
        DocumentWithChildren document = getDocument(client, documentId, true);

        // then
        assertNotNull(document);
        assertEquals(1, document.documents().size());
        assertEquals(response.documents().get(0).documentId(),
            document.documents().get(0).documentId());

        // given
        documentId = response.documents().get(0).documentId();

        // when
        document = getDocument(client, documentId, false);

        // then
        assertNotNull(document);
        assertNull(document.documents());
        assertEquals(response.documentId(), document.belongsToDocumentId());
      }
    }
  }
}
