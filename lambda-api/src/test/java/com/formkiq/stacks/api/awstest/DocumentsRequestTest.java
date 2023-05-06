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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.services.lambda.services.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import org.junit.AfterClass;
import org.junit.Test;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.stacks.client.FormKiqClient;
import com.formkiq.stacks.client.FormKiqClientConnection;
import com.formkiq.stacks.client.FormKiqClientV1;
import com.formkiq.stacks.client.HttpService;
import com.formkiq.stacks.client.HttpServiceJava;
import com.formkiq.stacks.client.models.AddDocument;
import com.formkiq.stacks.client.models.AddDocumentMetadata;
import com.formkiq.stacks.client.models.AddDocumentResponse;
import com.formkiq.stacks.client.models.AddDocumentTag;
import com.formkiq.stacks.client.models.Document;
import com.formkiq.stacks.client.models.DocumentMetadata;
import com.formkiq.stacks.client.models.DocumentSearchMetadata;
import com.formkiq.stacks.client.models.DocumentSearchQuery;
import com.formkiq.stacks.client.models.DocumentTags;
import com.formkiq.stacks.client.models.DocumentWithChildren;
import com.formkiq.stacks.client.models.Documents;
import com.formkiq.stacks.client.models.UpdateDocument;
import com.formkiq.stacks.client.requests.AddDocumentRequest;
import com.formkiq.stacks.client.requests.DeleteDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentRequest;
import com.formkiq.stacks.client.requests.GetDocumentTagsRequest;
import com.formkiq.stacks.client.requests.GetDocumentsRequest;
import com.formkiq.stacks.client.requests.OptionsDocumentRequest;
import com.formkiq.stacks.client.requests.SearchDocumentsRequest;
import com.formkiq.stacks.client.requests.UpdateDocumentRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

/**
 * GET, OPTIONS, POST /documents. Tests.
 *
 */
public class DocumentsRequestTest extends AbstractApiTest {

  /** 1 Second. */
  private static final int ONE_SECOND = 1000;
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** 201 Created. */
  private static final int STATUS_CREATED = 201;
  /** 403 Forbidden. */
  private static final int STATUS_FORBIDDEN = 403;
  /** 200 No Content. */
  private static final int STATUS_NO_CONTENT = 204;
  /** 404 Not Found. */
  private static final int STATUS_NOT_FOUND = 404;
  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60000;

  /**
   * After Class.
   */
  @AfterClass
  public static void afterClass() {
    getConfigService().delete(SITEID1);
  }

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

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
   * Get Document Tags.
   * 
   * @param client {@link FormKiqClient}
   * @param documentId {@link String}
   * @return {@link DocumentTags}
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  private DocumentTags getDocumentTags(final FormKiqClient client, final String documentId)
      throws IOException, InterruptedException {
    return client.getDocumentTags(new GetDocumentTagsRequest().documentId(documentId));
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
    assertEquals("{\"message\":\"fkq access denied (groups: default_read)\"}",
        responseSiteId.body());
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
    assertEquals("{\"message\":\"fkq access denied (groups: default)\"}", responseSiteId.body());
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
      assertEquals(STATUS_NO_CONTENT, response.statusCode());
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
      assertEquals(STATUS_NO_CONTENT, response.statusCode());
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
        if (map.containsKey("contentType") && map.get("contentLength") != null) {
          assertTrue(map.get("contentType").toString().startsWith("text/plain"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }
      assertEquals("9.0", map.get("contentLength").toString());

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
            && "application/pdf".equals(map.get("contentType").toString())
            && !"9.0".equals(map.get("contentLength").toString())) {
          assertEquals("application/pdf", map.get("contentType").toString());
          assertNotNull(map.get("contentLength"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      assertNotEquals("9.0", map.get("contentLength").toString());

      testPost01Delete(client, documentId);
    }
  }

  private void testPost01Delete(final FormKiqClientV1 client, final String documentId)
      throws IOException, InterruptedException {
    // given
    DeleteDocumentRequest delRequest = new DeleteDocumentRequest().documentId(documentId);
    GetDocumentRequest getRequest = new GetDocumentRequest().documentId(documentId);

    // when - delete document
    HttpResponse<String> response = client.deleteDocumentAsHttpResponse(delRequest);
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
    assertEquals("{\"message\":\"fkq access denied (groups: default_read)\"}",
        responseNoSiteId.body());

    assertEquals(STATUS_FORBIDDEN, responseSiteId.statusCode());
    assertRequestCorsHeaders(responseSiteId.headers());
    assertEquals("{\"message\":\"fkq access denied (groups: default_read)\"}",
        responseSiteId.body());
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
    assertEquals("{\"message\":\"fkq access denied (groups: default)\"}", responseSiteId.body());
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

    AuthenticationResultType utoken = login(USER_EMAIL, USER_PASSWORD);
    AuthenticationResultType ftoken = login(FINANCE_EMAIL, USER_PASSWORD);

    FormKiqClientV1 uclient = createHttpClient(utoken);
    FormKiqClientV1 fclient = createHttpClient(ftoken);

    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");
    AddDocumentRequest req = new AddDocumentRequest().document(post);

    // when
    final HttpResponse<String> responseNoSiteId = uclient.addDocumentAsHttpResponse(req);
    final HttpResponse<String> responseSiteId =
        fclient.addDocumentAsHttpResponse(req.siteId(siteId));

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
    getConfigService().save(SITEID1, new DynamicObject(Map.of(MAX_DOCUMENTS, "1")));

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
            .tags(Arrays.asList(new AddDocumentTag().key("formName").value("Job Application Form")))
            .documents(Arrays.asList(new AddDocument().contentType("application/json")
                .content("{\"firstName\": \"Jan\",\"lastName\": \"Doe\"}")
                .tags(Arrays.asList(new AddDocumentTag().key("formData")))));

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
        final DocumentWithChildren documentc = getDocument(client, documentId, true);
        DocumentTags tags = getDocumentTags(client, documentId);

        // then
        assertEquals(1, tags.tags().size());
        assertEquals("formName", tags.tags().get(0).key());
        assertEquals("Job Application Form", tags.tags().get(0).value());

        Documents search = client.search(new SearchDocumentsRequest()
            .query(new DocumentSearchQuery().meta(new DocumentSearchMetadata().path(documentId))));
        assertEquals(1, search.documents().size());
        assertEquals(documentId, search.documents().get(0).documentId());

        assertNotNull(documentc);
        assertEquals(1, documentc.documents().size());
        assertEquals(response.documents().get(0).documentId(),
            documentc.documents().get(0).documentId());

        // given
        documentId = response.documents().get(0).documentId();

        // when
        DocumentWithChildren document = getDocument(client, documentId, false);

        // then
        assertNotNull(document);
        assertNull(document.documents());
        assertEquals(response.documentId(), document.belongsToDocumentId());
      }
    }
  }

  /**
   * Save new File test content-type being set correctly from the Header.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost08() throws Exception {
    // given
    FormKiqClientV1 client = getFormKiqClients().get(0);
    String url = getRootHttpUrl() + "/documents";

    Map<String, List<String>> headers =
        Map.of("Authorization", Arrays.asList(getAdminToken().idToken()));
    Optional<HttpHeaders> o =
        Optional.of(HttpHeaders.of(headers, new BiPredicate<String, String>() {
          @Override
          public boolean test(final String t, final String u) {
            return true;
          }
        }));

    String content = "{\"path\": \"test.txt\",\"contentType\":\"text/plain\","
        + "\"content\":\"dGhpcyBpcyBhIHRlc3Q=\","
        + "\"tags\":[{\"key\":\"author\",\"value\":\"Pierre Loti\"}]}";

    // when
    HttpService hs = new HttpServiceJava();
    HttpResponse<String> response = hs.post(url, o, RequestBody.fromString(content));

    // then
    assertEquals(STATUS_CREATED, response.statusCode());

    // given
    Map<String, Object> map = toMap(response);
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
  }

  /**
   * Save new File and PATCH a new path and tags.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost09() throws Exception {
    for (FormKiqClientV1 client : getFormKiqClients()) {
      // given
      AddDocument post = new AddDocument().path("something.txt").contentType("text/plain")
          .content("test data", StandardCharsets.UTF_8)
          .tags(Arrays.asList(new AddDocumentTag().key("person").value("123")));
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

      Thread.sleep(ONE_SECOND * 2);

      // given
      String newpath = "newpath.txt";
      UpdateDocument updateDocument = new UpdateDocument().path(newpath)
          .tags(Arrays.asList(new AddDocumentTag().key("some").value("thing"),
              new AddDocumentTag().key("person").value("555")));
      UpdateDocumentRequest request =
          new UpdateDocumentRequest().document(updateDocument).documentId(documentId);

      // when - patch document
      response = client.updateDocumentAsHttpResponse(request);
      assertEquals(STATUS_OK, response.statusCode());
      assertRequestCorsHeaders(response.headers());

      // when - check path changed
      while (true) {
        map = fetchDocument(client, documentId);
        if (newpath.equals(map.get("path"))) {
          assertNotNull(map.get("contentLength"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      DocumentTags tags =
          client.getDocumentTags(new GetDocumentTagsRequest().documentId(documentId));
      assertEquals("555",
          tags.tags().stream().filter(t -> t.key().equals("person")).findAny().get().value());
      assertEquals("thing",
          tags.tags().stream().filter(t -> t.key().equals("some")).findAny().get().value());
    }
  }

  /**
   * Save Document with Metadata.
   * 
   * @throws Exception Exception
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testPost10() throws Exception {
    // given
    String content = "test data";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      for (FormKiqClientV1 client : getFormKiqClients()) {

        AddDocument post =
            new AddDocument().contentType("text/plain").content(content, StandardCharsets.UTF_8)
                .metadata(Arrays.asList(new AddDocumentMetadata().key("person").value("category"),
                    new AddDocumentMetadata().key("playerId").values(Arrays.asList("11", "22"))));
        AddDocumentRequest req = new AddDocumentRequest().siteId(siteId).document(post);

        // when
        AddDocumentResponse response = client.addDocument(req);

        // given
        String documentId = response.documentId();

        // when - fetch document
        waitForDocumentContent(client, siteId, documentId, content);

        // then
        GetDocumentRequest getReq = new GetDocumentRequest().siteId(siteId).documentId(documentId);

        Document document = client.getDocument(getReq);
        assertEquals(2, document.metadata().size());
        Iterator<DocumentMetadata> itr = document.metadata().iterator();
        DocumentMetadata md = itr.next();
        assertEquals("playerId", md.key());
        assertEquals("[11, 22]", md.values().toString());
        md = itr.next();
        assertEquals("person", md.key());
        assertEquals("category", md.value());
      }
    }
  }
}
