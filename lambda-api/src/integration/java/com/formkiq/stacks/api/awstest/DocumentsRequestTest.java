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
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import static com.formkiq.testutils.aws.FkqDocumentService.waitForActionsComplete;
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
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.invoker.ApiResponse;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.ChecksumType;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.client.model.DocumentAttribute;
import com.formkiq.client.model.GetAttributeResponse;
import com.formkiq.module.http.HttpHeaders;
import com.formkiq.module.http.HttpService;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.stacks.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.stacks.dynamodb.config.SiteConfiguration;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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
import com.formkiq.stacks.dynamodb.config.ConfigService;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import com.google.gson.Gson;

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
  private static final String SITEID1 = ID.uuid();
  /** 400 Bad Request. */
  private static final int STATUS_BAD_REQUEST = 400;
  /** 201 Created. */
  private static final int STATUS_CREATED = 201;
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

    addAndLoginCognito(READONLY_EMAIL, List.of("default_read"));
    addAndLoginCognito(USER_EMAIL, List.of(DEFAULT_SITE_ID));
    addAndLoginCognito(FINANCE_EMAIL, List.of(GROUP_FINANCE));
  }

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

  /** {@link Gson}. */
  private final Gson gson = GsonUtil.getInstance();

  /** {@link HttpClient}. */
  private final HttpClient http = HttpClient.newHttpClient();

  /**
   * Fetch Document.
   * 
   * @param api {@link DocumentsApi}
   * @param documentId {@link String}
   * @return {@link GetDocumentResponse}
   * @throws InterruptedException InterruptedException
   */
  private GetDocumentResponse fetchDocument(final DocumentsApi api, final String documentId)
      throws InterruptedException {

    while (true) {

      try {
        return api.getDocument(documentId, null, null);
      } catch (ApiException e) {
        Thread.sleep(ONE_SECOND);
      }
    }
  }

  private AddDocumentRequest getChildDocumentRequest(final String content) {
    return new AddDocumentRequest()
        .addTagsItem(new AddDocumentTag().key("formName").value("Job Application Form"))
        .addDocumentsItem(new AddChildDocument().content(content).contentType("application/json")
            .addTagsItem(new AddDocumentTag().key("formData")));
  }

  /**
   * Delete Not existing file.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testDelete01() throws Exception {
    for (ApiClient client : getApiClients(null)) {
      // given
      DocumentsApi api = new DocumentsApi(client);
      String documentId = ID.uuid();

      // when
      try {
        api.deleteDocument(documentId, null, Boolean.FALSE);
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
  @Timeout(value = TEST_TIMEOUT)
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
  @Timeout(value = TEST_TIMEOUT)
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
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied to siteId (finance)\"}", e.getResponseBody());
    }
  }

  /**
   * Get Not existing file. Test user with 'USERS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
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
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied to siteId (finance)\"}", e.getResponseBody());
    }
  }

  /**
   * Get Not existing file. Test user with 'ADMINS' roles with/out siteid
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
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
  @Timeout(value = TEST_TIMEOUT)
  public void testGet05() throws Exception {
    // given
    ApiClient client = getApiClientForUser(FINANCE_EMAIL, USER_PASSWORD);
    DocumentsApi api = new DocumentsApi(client);

    String date = this.df.format(new Date());

    // when
    GetDocumentsResponse results0 =
        api.getDocuments(null, null, null, date, null, null, null, null);
    GetDocumentsResponse results1 =
        api.getDocuments(GROUP_FINANCE, null, null, date, null, null, null, null);

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
  @Timeout(value = TEST_TIMEOUT)
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
  @Timeout(value = TEST_TIMEOUT)
  public void testPatch01SiteIdClient() throws Exception {
    for (ApiClient client : getApiClients(SITE_ID)) {
      testPatch01(client, SITE_ID);
    }
  }

  /**
   * Wait For Document Content.
   *
   * @param client {@link ApiClient}
   * @param documentId {@link String}
   * @throws InterruptedException InterruptedException
   */
  private void waitForDocumentLength(final ApiClient client, final String documentId)
      throws InterruptedException {

    DocumentsApi api = new DocumentsApi(client);

    while (true) {

      try {
        GetDocumentResponse response = api.getDocument(documentId, null, null);
        if (response.getContentLength() != null) {
          return;
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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost01() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {
      DocumentsApi api = new DocumentsApi(client);
      AddDocumentRequest addReq =
          new AddDocumentRequest().content("test data").contentType("text/plain");

      // when
      AddDocumentResponse response = api.addDocument(addReq, null, null);

      // then
      String documentId = response.getDocumentId();
      waitForDocumentLength(client, documentId);

      GetDocumentResponse document = api.getDocument(documentId, null, null);
      assertEquals("9", java.util.Objects.requireNonNull(document.getContentLength()).toString());

      // given
      UpdateDocumentRequest updateReq =
          new UpdateDocumentRequest().content("dummy data").contentType("application/pdf");

      // when - patch document
      api.updateDocument(documentId, updateReq, null, null);

      // then - check content type changed
      while (true) {
        // map = fetchDocument(client, documentId);
        document = api.getDocument(documentId, null, null);

        if ("application/pdf".equals(document.getContentType())) {
          assertNotEquals("9.0",
              java.util.Objects.requireNonNull(document.getContentLength()).toString());
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      testPost01Delete(client, documentId);
    }
  }

  private void testPost01Delete(final ApiClient client, final String documentId)
      throws ApiException {

    DocumentsApi api = new DocumentsApi(client);
    api.deleteDocument(documentId, null, Boolean.FALSE);

    try {
      api.getDocument(documentId, null, null);
    } catch (ApiException e) {
      assertEquals(STATUS_NOT_FOUND, e.getCode());
    }
  }

  /**
   * Save new File as Readonly user.
   *
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost02() {
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
      assertEquals("{\"message\":\"fkq access denied to siteId (finance)\"}", e.getResponseBody());
    }
  }

  /**
   * Save new File as 'USERS' group.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
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
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals("{\"message\":\"fkq access denied to siteId (finance)\"}", e.getResponseBody());
    }
  }

  /**
   * Save new File as 'ADMINS' group.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost05() throws Exception {
    // given
    final String siteId = GROUP_FINANCE;

    ApiClient uclient = getApiClientForUser(USER_EMAIL, USER_PASSWORD);
    DocumentsApi uapi = new DocumentsApi(uclient);

    ApiClient fclient = getApiClientForUser(FINANCE_EMAIL, USER_PASSWORD);
    DocumentsApi fapi = new DocumentsApi(fclient);

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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost06() throws Exception {
    // given
    SiteConfiguration config = new SiteConfiguration().setMaxDocuments("1");
    configService.save(SITEID1, config);

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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost07() throws Exception {

    // given
    for (boolean enableEndpoint : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

      for (ApiClient client : getApiClients(null)) {

        DocumentsApi api = new DocumentsApi(client);
        PublicApi publicApi = new PublicApi(client);

        String content = "{\"firstName\": \"Jan\",\"lastName\": \"Doe\"}";

        AddDocumentRequest req = getChildDocumentRequest(content);

        // when
        AddDocumentResponse response = enableEndpoint ? publicApi.publicAddDocument(req, null)
            : api.addDocument(req, null, null);

        // then
        assertNotNull(response.getDocumentId());
        assertNotNull(response.getUploadUrl());

        assertEquals(1, notNull(response.getDocuments()).size());
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
        waitForDocumentTag(client, null, documentId, "formName");

        // given
        DocumentTagsApi tagApi = new DocumentTagsApi(client);

        // when - fetch document
        GetDocumentTagsResponse tags =
            tagApi.getDocumentTags(documentId, null, null, null, null, null);

        // then
        assertNotNull(tags.getTags());
        assertEquals(1, tags.getTags().size());
        assertEquals("formName", tags.getTags().get(0).getKey());
        assertEquals("Job Application Form", tags.getTags().get(0).getValue());

        DocumentSearchApi searchApi = new DocumentSearchApi(client);
        DocumentSearchRequest searchReq = new DocumentSearchRequest()
            .query(new DocumentSearch().meta(new DocumentSearchMeta().path(documentId)));
        DocumentSearchResponse s = searchApi.documentSearch(searchReq, null, null, null, null);

        assertEquals(1, notNull(s.getDocuments()).size());
        assertEquals(documentId, s.getDocuments().get(0).getDocumentId());

        GetDocumentResponse documentc = api.getDocument(documentId, null, null);
        assertNotNull(documentc.getDocuments());
        assertEquals(1, documentc.getDocuments().size());
        assertEquals(response.getDocuments().get(0).getDocumentId(),
            documentc.getDocuments().get(0).getDocumentId());

        // given
        documentId = response.getDocuments().get(0).getDocumentId();

        // when
        GetDocumentResponse document = api.getDocument(documentId, null, null);

        // then
        assertNotNull(document);
        assertTrue(notNull(document.getDocuments()).isEmpty());
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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost08() throws Exception {
    // given
    ApiClient client = getApiClients(null).get(0);
    String url = client.getBasePath() + "/documents";

    Optional<HttpHeaders> o =
        Optional.of(new HttpHeaders().add("Authorization", getAdminToken().idToken()));

    String content = "{\"path\": \"test.txt\",\"contentType\":\"text/plain\","
        + "\"content\":\"dGhpcyBpcyBhIHRlc3Q=\","
        + "\"tags\":[{\"key\":\"author\",\"value\":\"Pierre Loti\"}]}";

    // when
    HttpService hs = new HttpServiceJdk11();
    HttpResponse<String> response = hs.post(url, o, Optional.empty(), content);

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
  @Timeout(value = TEST_TIMEOUT)
  public void testPost09() throws Exception {
    // given
    for (ApiClient client : getApiClients(null)) {
      DocumentsApi api = new DocumentsApi(client);

      AddDocumentRequest req =
          new AddDocumentRequest().path("something.txt").contentType("text/plain")
              .content("test data").addTagsItem(new AddDocumentTag().key("person").value("123"));

      // when
      AddDocumentResponse response = api.addDocument(req, null, null);

      // then
      assertNotNull(response.getDocumentId());

      // given
      String documentId = response.getDocumentId();

      // when - fetch document
      while (true) {
        GetDocumentResponse map = fetchDocument(api, documentId);
        if (map.getContentType() != null) {
          assertTrue(map.getContentType().startsWith("text/plain"));
          break;
        }

        Thread.sleep(ONE_SECOND);
      }

      Thread.sleep(ONE_SECOND * 2);

      // given
      String newpath = "newpath_" + UUID.randomUUID() + ".txt";

      UpdateDocumentRequest updateDoc = new UpdateDocumentRequest().path(newpath)
          .addTagsItem(new AddDocumentTag().key("some").value("thing"))
          .addTagsItem(new AddDocumentTag().key("person").value("555"));

      // when - patch document
      api.updateDocument(documentId, updateDoc, null, null);

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
          tagApi.getDocumentTags(documentId, null, null, null, null, null);
      assertEquals("555", java.util.Objects.requireNonNull(tags.getTags()).stream()
          .filter(t -> java.util.Objects.equals(t.getKey(), "person")).findAny().get().getValue());
      assertEquals("thing", tags.getTags().stream().filter(t -> "some".equals(t.getKey())).findAny()
          .get().getValue());
    }
  }

  /**
   * Save Document with Metadata.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost10() throws Exception {
    // given
    String content = "test data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {
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
        assertNotNull(document.getMetadata());
        assertEquals(2, document.getMetadata().size());
        assertEquals("playerId", document.getMetadata().get(0).getKey());
        assertEquals("11,22", String.join(",",
            java.util.Objects.requireNonNull(document.getMetadata().get(0).getValues())));
        assertEquals("person", document.getMetadata().get(1).getKey());
        assertEquals("category", document.getMetadata().get(1).getValue());
      }
    }
  }

  /**
   * Save new File with valid SHA-256.
   *
   * @throws ApiException ApiException
   */
  @Test
  public void testPost11() throws ApiException {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (ApiClient client : getApiClients(siteId)) {

        DocumentsApi api = new DocumentsApi(client);

        String content = "dummy data";
        String checksum = DigestUtils.sha256Hex(content);

        AddDocumentRequest req = new AddDocumentRequest().content(content).contentType("text/plain")
            .checksum(checksum).checksumType(ChecksumType.SHA256);

        // when
        AddDocumentResponse response = api.addDocument(req, siteId, null);

        // then
        assertNotNull(response.getDocumentId());
        assertEquals(siteId, response.getSiteId());

        GetDocumentResponse site = api.getDocument(response.getDocumentId(), siteId, null);
        assertEquals("text/plain", site.getContentType());
        assertNotNull(site.getPath());
        assertNotNull(site.getDocumentId());
        assertEquals(content,
            api.getDocumentContent(response.getDocumentId(), siteId, null, null).getContent());
      }
    }
  }

  /**
   * Add Document with very large attributes.
   *
   * @throws Exception Exception
   */
  @Test
  public void testPost12() throws Exception {
    // given
    String content0 = "test data";
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      for (ApiClient client : getApiClients(siteId)) {

        String attributeKey = "category_" + UUID.randomUUID();

        DocumentsApi api = new DocumentsApi(client);
        AttributesApi attrApi = new AttributesApi(client);

        attrApi.addAttribute(
            new AddAttributeRequest().attribute(new AddAttribute().key(attributeKey)), siteId);

        final int len = 3000;
        String value = Strings.generateRandomString(len);
        AddDocumentRequest req =
            new AddDocumentRequest().content(content0).addAttributesItem(new AddDocumentAttribute(
                new AddDocumentAttributeStandard().key(attributeKey).stringValue(value)));

        // when
        String documentId = api.addDocument(req, siteId, null).getDocumentId();

        // then
        DocumentAttributesApi documentAttributesApi = new DocumentAttributesApi(client);
        List<DocumentAttribute> attributes = notNull(documentAttributesApi
            .getDocumentAttributes(documentId, siteId, null, null).getAttributes());

        final int expected = 1;
        assertEquals(expected, attributes.size());
        assertEquals(attributeKey, attributes.get(0).getKey());
        assertEquals(value, attributes.get(0).getStringValue());
      }
    }
  }

  /**
   * Save new File using API Key with non-matching SiteId.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testPost13() throws Exception {
    // given
    ApiClient client = getApiClients("mysite").get(2);
    DocumentsApi api = new DocumentsApi(client);
    AddDocumentRequest addReq =
        new AddDocumentRequest().content("test data").contentType("text/plain");

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      // when
      try {
        api.addDocument(addReq, siteId, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_UNAUTHORIZED.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"fkq access denied to siteId (" + siteId + ")\"}",
            e.getResponseBody());
      }
    }

    // when
    AddDocumentResponse response = api.addDocument(addReq, null, null);

    // then
    String documentId = response.getDocumentId();
    assertEquals("mysite", api.getDocument(documentId, "mysite", null).getSiteId());
    assertEquals("mysite", api.getDocument(documentId, null, null).getSiteId());
  }

  /**
   * Test Publish Document.
   *
   * @throws ApiException ApiException
   */
  @Test
  void testPublishDocument() throws ApiException, InterruptedException {
    // given
    String content = "test data";
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      ApiClient client = getApiClients(siteId).get(0);

      DocumentsApi api = new DocumentsApi(client);
      String path = UUID.randomUUID() + ".txt";

      // when
      AddDocumentRequest req = new AddDocumentRequest().contentType("text/plain").path(path)
          .content(content).addActionsItem(new AddAction().type(DocumentActionType.PUBLISH));
      String documentId = api.addDocument(req, siteId, null).getDocumentId();

      // then
      waitForDocumentContent(client, siteId, documentId, content);
      waitForActionsComplete(client, siteId, documentId);

      ApiResponse<Void> response = api.getPublishedDocumentContentWithHttpInfo(documentId, siteId);

      String location = notNull(response.getHeaders()).get("content-disposition").get(0);
      assertEquals("attachment; filename=\"" + path + "\"", location);

      String contentType = notNull(response.getHeaders()).get("content-type").get(0);
      assertEquals("text/plain", contentType);

      // check attribute exists
      AttributesApi attributesApi = new AttributesApi(client);
      GetAttributeResponse attribute =
          attributesApi.getAttribute(AttributeKeyReserved.PUBLICATION.getKey(), siteId);
      assertNotNull(attribute);
    }
  }
}
