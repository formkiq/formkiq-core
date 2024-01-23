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
import static com.formkiq.stacks.dynamodb.ConfigService.MAX_DOCUMENTS;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentContent;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForDocumentTag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.objects.MimeType;
import com.formkiq.aws.services.lambda.GsonUtil;
import com.formkiq.client.api.DocumentSearchApi;
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.PublicApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddChildDocument;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.AddDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.GetDocumentResponse;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.stacks.client.HttpService;
import com.formkiq.stacks.client.HttpServiceJava;
import com.formkiq.stacks.client.models.AddDocument;
import com.formkiq.stacks.dynamodb.ConfigService;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.google.gson.Gson;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * GET, OPTIONS, POST /documents. Tests.
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocumentsRequestTest extends AbstractAwsIntegrationTest {

  /** {@link ConfigService}. */
  private static ConfigService configService;
  /** Cognito FINANCE User Email. */
  private static final String FINANCE_EMAIL = "testfinance56@formkiq.com";
  /** Finance Group. */
  private static final String GROUP_FINANCE = "finance56";
  /** 1 Second. */
  private static final int ONE_SECOND = 1000;
  /** Cognito User Email. */
  private static final String READONLY_EMAIL = "readonly56@formkiq.com";
  /** Random Site ID. */
  private static final String SITEID1 = UUID.randomUUID().toString();
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** 201 Created. */
  private static final int STATUS_CREATED = 201;
  /** 403 Forbidden. */
  private static final int STATUS_FORBIDDEN = 403;
  /** 404 Not Found. */
  private static final int STATUS_NOT_FOUND = 404;
  /** 200 OK. */
  private static final int STATUS_OK = 200;
  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 60;
  /** Cognito User Email. */
  private static final String USER_EMAIL = "testuser56@formkiq.com";

  /**
   * After Class.
   */
  @AfterAll
  public static void afterClass() {
    configService.delete(SITEID1);
  }

  /**
   * Before All.
   * 
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeAll
  public static void beforeAll() throws IOException, InterruptedException, URISyntaxException {
    AbstractAwsIntegrationTest.beforeClass();

    configService = new FkqConfigService(getAwsprofile(), getAwsregion(), getAppenvironment());

    addAndLoginCognito(READONLY_EMAIL, Arrays.asList("default_read"));
    addAndLoginCognito(USER_EMAIL, Arrays.asList(DEFAULT_SITE_ID));
    addAndLoginCognito(FINANCE_EMAIL, Arrays.asList(GROUP_FINANCE));
  }

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

  /** {@link Gson}. */
  private Gson gson = GsonUtil.getInstance();

  /** {@link HttpClient}. */
  private HttpClient http = HttpClient.newHttpClient();

  /**
   * Fetch Document.
   * 
   * @param api {@link DocumentsApi}
   * @param documentId {@link String}
   * @return {@link GetDocumentResponse}
   * @throws IOException IOException
   * @throws InterruptedException InterruptedException
   * @throws URISyntaxException URISyntaxException
   */
  private GetDocumentResponse fetchDocument(final DocumentsApi api, final String documentId)
      throws IOException, InterruptedException, URISyntaxException {

    String siteId = null;

    while (true) {

      try {
        return api.getDocument(documentId, siteId, null);
      } catch (ApiException e) {
        Thread.sleep(ONE_SECOND);
      }
    }
  }

  private AddDocumentRequest getChildDocumentRequest(final String content) {
    AddDocumentRequest req = new AddDocumentRequest()
        .addTagsItem(new AddDocumentTag().key("formName").value("Job Application Form"))
        .addDocumentsItem(new AddChildDocument().content(content).contentType("application/json")
            .addTagsItem(new AddDocumentTag().key("formData")));
    return req;
  }

  /**
   * Delete Not existing file.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testDelete01() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      String siteId = null;
      DocumentsApi api = new DocumentsApi(client);
      String documentId = UUID.randomUUID().toString();

      // when
      try {
        api.deleteDocument(documentId, siteId, Boolean.FALSE);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(STATUS_NOT_FOUND, e.getCode());
      }
    }
  }

  /**
   * Get Not existing file.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet01() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      DocumentsApi api = new DocumentsApi(client);
      String date = this.df.format(new Date());

      // when
      GetDocumentsResponse response =
          api.getDocuments(null, null, null, date, null, null, null, null);

      // then
      assertNotNull(response.getDocuments());
    }
  }

  /**
   * Get Not existing file. Test user with no roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet02() throws Exception {
    // given
    final String siteId = "finance";

    ApiClient client = getApiClientForUser(READONLY_EMAIL, USER_PASSWORD);

    DocumentsApi api = new DocumentsApi(client);

    // when
    GetDocumentsResponse responseNoSiteId =
        api.getDocuments(null, null, null, null, null, null, null, null);

    // then
    assertNotNull(responseNoSiteId.getDocuments());

    // when
    try {
      api.getDocuments(siteId, null, null, null, null, null, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(STATUS_FORBIDDEN, e.getCode());
      assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Get Not existing file. Test user with 'USERS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet03() throws Exception {
    // given
    String siteId = "finance";
    ApiClient client = getApiClientForUser(USER_EMAIL, USER_PASSWORD);

    DocumentsApi api = new DocumentsApi(client);

    String date = this.df.format(new Date());

    // when
    GetDocumentsResponse responseNoSiteId =
        api.getDocuments(null, null, null, date, null, null, null, null);

    // then
    assertNotNull(responseNoSiteId.getDocuments());

    // when
    try {
      api.getDocuments(siteId, null, null, date, null, null, null, null);
    } catch (ApiException e) {
      assertEquals(STATUS_FORBIDDEN, e.getCode());
      assertEquals("{\"message\":\"fkq access denied (groups: default (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Get Not existing file. Test user with 'ADMINS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet04() throws Exception {
    // given
    final String siteId = "finance";
    // getcli
    ApiClient client = getApiClients(siteId).get(0);
    DocumentsApi api = new DocumentsApi(client);
    String date = this.df.format(new Date());

    // when
    GetDocumentsResponse responseNoSiteId =
        api.getDocuments(null, null, null, date, null, null, null, null);
    GetDocumentsResponse responseSiteId =
        api.getDocuments(siteId, null, null, date, null, null, null, null);

    // then
    assertNotNull(responseNoSiteId.getDocuments());
    assertNotNull(responseSiteId.getDocuments());
  }

  /**
   * Get Not existing file. Test user with 'FINANCE' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    final String siteId = GROUP_FINANCE;
    ApiClient client = getApiClientForUser(FINANCE_EMAIL, USER_PASSWORD);
    DocumentsApi api = new DocumentsApi(client);

    String date = this.df.format(new Date());

    // when
    GetDocumentsResponse results0 =
        api.getDocuments(null, null, null, date, null, null, null, null);
    GetDocumentsResponse results1 =
        api.getDocuments(siteId, null, null, date, null, null, null, null);

    // then
    assertNotNull(results0.getDocuments());
    assertNotNull(results1.getDocuments());
  }

  /**
   * Save new File and update new tags with default site.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @throws ApiException ApiException
   * @throws InterruptedException InterruptedException
   */
  private void testPatch01(final ApiClient client, final String siteId)
      throws ApiException, InterruptedException {
    // given
    String content = "test data";
    DocumentsApi api = new DocumentsApi(client);

    AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content);

    // when
    AddDocumentResponse response = api.addDocument(req, siteId, null);

    // then
    String documentId = response.getDocumentId();
    waitForDocumentContent(client, siteId, documentId);

    // given
    String tagKey = "mykey";
    UpdateDocumentRequest updateReq =
        new UpdateDocumentRequest().addTagsItem(new AddDocumentTag().key(tagKey).value("myvalue"));

    // when - patch document
    api.updateDocument(documentId, updateReq, siteId, null);

    // then
    waitForDocumentTag(client, siteId, documentId, tagKey);

    waitForDocumentContent(client, siteId, documentId, content);
  }

  /**
   * Save new File and update new tags with default site.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPatch01DefaultClient() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      testPatch01(client, null);
    }
  }

  /**
   * Save new File and update new tags with default site.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPatch01SiteIdClient() throws Exception {
    for (ApiClient client : getApiClients(SITE_ID)) {
      testPatch01(client, SITE_ID);
    }
  }

  /**
   * Wait For Document Content.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link GetDocumentResponse}
   * @throws InterruptedException InterruptedException
   */
  private GetDocumentResponse waitForDocumentLength(final ApiClient client, final String siteId,
      final String documentId) throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentResponse response = api.getDocument(documentId, siteId, null);
        if (response.getContentLength() != null) {
          return response;
        }

      } catch (ApiException e) {
        // ignore error
      }

      TimeUnit.SECONDS.sleep(1);
    }
  }

  /**
   * Save new File.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost01() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(siteId)) {
      DocumentsApi api = new DocumentsApi(client);
      AddDocumentRequest addReq =
          new AddDocumentRequest().content("test data").contentType("text/plain");

      // when
      AddDocumentResponse response = api.addDocument(addReq, siteId, null);

      // then
      String documentId = response.getDocumentId();
      waitForDocumentLength(client, siteId, documentId);

      GetDocumentResponse document = api.getDocument(documentId, siteId, null);
      assertEquals("9", document.getContentLength().toString());

      // given
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().content("dummy data").contentType("application/pdf");

      // when - patch document
      api.updateDocument(documentId, updateReq, siteId, null);

      // then - check content type changed
      while (true) {
        // map = fetchDocument(client, documentId);
        document = api.getDocument(documentId, siteId, null);

        if (document.getContentType().equals("application/pdf")) {
          assertNotEquals("9.0", document.getContentLength().toString());
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      testPost01Delete(client, documentId);
    }
  }

  private void testPost01Delete(final ApiClient client, final String documentId)
      throws ApiException {

    String siteId = null;
    DocumentsApi api = new DocumentsApi(client);
    api.deleteDocument(documentId, siteId, Boolean.FALSE);

    try {
      api.getDocument(documentId, siteId, null);
    } catch (ApiException e) {
      assertEquals(STATUS_NOT_FOUND, e.getCode());
    }
  }

  /**
   * Save new File as Readonly user.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost02() throws Exception {
    // given
    final String siteId = "finance";
    ApiClient client = getApiClientForUser(READONLY_EMAIL, USER_PASSWORD);
    DocumentsApi api = new DocumentsApi(client);

    AddDocumentRequest req =
        new AddDocumentRequest().content("dummy data").contentType("application/pdf");

    // when
    try {
      api.addDocument(req, null, null);
    } catch (ApiException e) {
      // then
      assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}",
          e.getResponseBody());
    }

    // when
    try {
      api.addDocument(req, siteId, null);
    } catch (ApiException e) {
      // then
      assertEquals("{\"message\":\"fkq access denied (groups: default (READ))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Save new File as 'USERS' group.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost03() throws Exception {
    // given
    final String siteId = "finance";
    ApiClient client = getApiClientForUser(USER_EMAIL, USER_PASSWORD);
    DocumentsApi api = new DocumentsApi(client);

    AddDocumentRequest req =
        new AddDocumentRequest().content("dummy data").contentType("application/pdf");

    // when
    AddDocumentResponse responseNoSiteId = api.addDocument(req, null, null);

    // then
    assertNotNull(responseNoSiteId.getDocumentId());

    // when
    try {
      api.addDocument(req, siteId, null);
    } catch (ApiException e) {
      // then
      assertEquals(STATUS_FORBIDDEN, e.getCode());
      assertEquals("{\"message\":\"fkq access denied (groups: default (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * Save new File as 'ADMINS' group.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost04() throws Exception {
    // given
    String siteId = "finance";
    ApiClient client = getApiClients(siteId).get(0);
    DocumentsApi api = new DocumentsApi(client);

    AddDocumentRequest req =
        new AddDocumentRequest().content("dummy data").contentType("application/pdf");

    // when
    AddDocumentResponse responseNoSiteId = api.addDocument(req, null, null);
    AddDocumentResponse responseSiteId = api.addDocument(req, siteId, null);

    // then
    assertNotNull(responseNoSiteId.getDocumentId());
    assertNotNull(responseSiteId.getDocumentId());
    assertEquals(siteId, responseSiteId.getSiteId());
  }

  /**
   * Save new File as 'FINANCE' group.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost05() throws Exception {
    // given
    final String siteId = GROUP_FINANCE;

    ApiClient uclient = getApiClientForUser(USER_EMAIL, USER_PASSWORD);
    DocumentsApi uapi = new DocumentsApi(uclient);

    ApiClient fclient = getApiClientForUser(FINANCE_EMAIL, USER_PASSWORD);
    DocumentsApi fapi = new DocumentsApi(fclient);

    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");

    AddDocumentRequest req =
        new AddDocumentRequest().content("dummy data").contentType("application/pdf");

    // when
    AddDocumentResponse responseNoSiteId = uapi.addDocument(req, null, null);
    AddDocumentResponse responseSiteId = fapi.addDocument(req, siteId, null);

    // then
    assertNotNull(responseNoSiteId.getDocumentId());

    assertNotNull(responseSiteId.getDocumentId());
    assertEquals(siteId, responseSiteId.getSiteId());
  }

  /**
   * Post /documents, MAX DocumentGreater than allowed.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost06() throws Exception {
    // given
    configService.save(SITEID1, new DynamicObject(Map.of(MAX_DOCUMENTS, "1")));

    AddDocument post = new AddDocument();
    post.content("dummy data", StandardCharsets.UTF_8);
    post.contentType("application/pdf");

    ApiClient c = getApiClients(SITEID1).get(0);
    DocumentsApi api = new DocumentsApi(c);
    AddDocumentRequest req =
        new AddDocumentRequest().content("dummy data").contentType("application/pdf");

    api.addDocument(req, SITEID1, null);

    for (ApiClient client : getApiClients(SITEID1)) {
      api = new DocumentsApi(client);

      // when
      try {
        api.addDocument(req, SITEID1, null);
        fail();
      } catch (ApiException e) {

        // then
        assertEquals(STATUS_BAD_REQUEST, e.getCode());
        assertEquals("{\"message\":\"Max Number of Documents reached\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Save document with subdocuments against private/public endpoints.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost07() throws Exception {

    // given
    String siteId = null;

    for (boolean enableEndpoint : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);
        PublicApi publicApi = new PublicApi(client);

        String content = "{\"firstName\": \"Jan\",\"lastName\": \"Doe\"}";

        AddDocumentRequest req = getChildDocumentRequest(content);

        // when
        AddDocumentResponse response = enableEndpoint ? publicApi.publicAddDocument(req, siteId)
            : api.addDocument(req, siteId, null);

        // then
        assertNotNull(response.getDocumentId());
        assertNotNull(response.getUploadUrl());

        assertEquals(1, response.getDocuments().size());
        assertNotNull(response.getDocuments().get(0).getDocumentId());
        assertNull(response.getDocuments().get(0).getUploadUrl());

        // given
        content = "this is a test";

        // when
        HttpResponse<String> httpresp =
            this.http.send(
                HttpRequest.newBuilder(new URI(response.getUploadUrl()))
                    .header("Content-Type", MimeType.MIME_HTML.getContentType())
                    .method("PUT", BodyPublishers.ofString(content)).build(),
                BodyHandlers.ofString());

        // then
        String documentId = response.getDocumentId();
        assertEquals(STATUS_OK, httpresp.statusCode());
        waitForDocumentTag(client, siteId, documentId, "formName");

        // given
        DocumentTagsApi tagApi = new DocumentTagsApi(client);

        // when - fetch document
        GetDocumentTagsResponse tags =
            tagApi.getDocumentTags(documentId, siteId, null, null, null, null);

        // then
        assertEquals(1, tags.getTags().size());
        assertEquals("formName", tags.getTags().get(0).getKey());
        assertEquals("Job Application Form", tags.getTags().get(0).getValue());

        DocumentSearchApi searchApi = new DocumentSearchApi(client);
        DocumentSearchRequest searchReq = new DocumentSearchRequest()
            .query(new DocumentSearch().meta(new DocumentSearchMeta().path(documentId)));
        DocumentSearchResponse s = searchApi.documentSearch(searchReq, siteId, null, null, null);

        assertEquals(1, s.getDocuments().size());
        assertEquals(documentId, s.getDocuments().get(0).getDocumentId());

        GetDocumentResponse documentc = api.getDocument(documentId, siteId, null);
        assertEquals(1, documentc.getDocuments().size());
        assertEquals(response.getDocuments().get(0).getDocumentId(),
            documentc.getDocuments().get(0).getDocumentId());

        // given
        documentId = response.getDocuments().get(0).getDocumentId();

        // when
        GetDocumentResponse document = api.getDocument(documentId, null, null);

        // then
        assertNotNull(document);
        assertNull(document.getDocuments());
        assertEquals(response.getDocumentId(), document.getBelongsToDocumentId());
      }
    }
  }

  /**
   * Save new File test content-type being set correctly from the Header.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost08() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);
    String url = client.getBasePath() + "/documents";

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
    GetDocumentResponse map = this.gson.fromJson(response.body(), GetDocumentResponse.class);
    String documentId = map.getDocumentId();

    DocumentsApi api = new DocumentsApi(client);

    // when - fetch document
    while (true) {
      map = fetchDocument(api, documentId);
      if (map.getContentType() != null) {
        assertTrue(map.getContentType().startsWith("text/plain"));
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
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost09() throws Exception {
    // given
    String siteId = null;
    for (ApiClient client : getApiClients(null)) {
      DocumentsApi api = new DocumentsApi(client);

      AddDocumentRequest req =
          new AddDocumentRequest().path("something.txt").contentType("text/plain")
              .content("test data").addTagsItem(new AddDocumentTag().key("person").value("123"));

      // when
      AddDocumentResponse response = api.addDocument(req, siteId, null);

      // then
      assertNotNull(response.getDocumentId());

      // given
      String documentId = response.getDocumentId();

      // when - fetch document
      while (true) {
        GetDocumentResponse map = fetchDocument(api, documentId);
        if (map.getContentType() != null) {
          assertTrue(map.getContentType().toString().startsWith("text/plain"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      Thread.sleep(ONE_SECOND * 2);

      // given
      String newpath = "newpath_" + UUID.randomUUID().toString() + ".txt";

      UpdateDocumentRequest updateDoc = new UpdateDocumentRequest().path(newpath)
          .addTagsItem(new AddDocumentTag().key("some").value("thing"))
          .addTagsItem(new AddDocumentTag().key("person").value("555"));

      // when - patch document
      api.updateDocument(documentId, updateDoc, siteId, null);

      // then - check path changed
      while (true) {
        GetDocumentResponse map = fetchDocument(api, documentId);
        if (newpath.equals(map.getPath())) {
          assertNotNull(map.getContentLength());
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      DocumentTagsApi tagApi = new DocumentTagsApi(client);
      GetDocumentTagsResponse tags =
          tagApi.getDocumentTags(documentId, siteId, null, null, null, null);
      assertEquals("555", tags.getTags().stream().filter(t -> t.getKey().equals("person")).findAny()
          .get().getValue());
      assertEquals("thing", tags.getTags().stream().filter(t -> t.getKey().equals("some")).findAny()
          .get().getValue());
    }
  }

  /**
   * Save Document with Metadata.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testPost10() throws Exception {
    // given
    String content = "test data";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);

        AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").content(content)
            .addMetadataItem(new AddDocumentMetadata().key("person").value("category"))
            .addMetadataItem(
                new AddDocumentMetadata().key("playerId").values(Arrays.asList("11", "22")));

        // when
        AddDocumentResponse response = api.addDocument(req, siteId, null);

        // given
        String documentId = response.getDocumentId();

        // when - fetch document
        waitForDocumentContent(client, siteId, documentId, content);

        // then
        GetDocumentResponse document = api.getDocument(documentId, siteId, null);
        assertNotNull(document);
        assertEquals(2, document.getMetadata().size());
        assertEquals("playerId", document.getMetadata().get(0).getKey());
        assertEquals("11,22",
            document.getMetadata().get(0).getValues().stream().collect(Collectors.joining(",")));
        assertEquals("person", document.getMetadata().get(1).getKey());
        assertEquals("category", document.getMetadata().get(1).getValue());
      }
    }
  }
}
