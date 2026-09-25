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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AttributeValueType;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchFilename;
import com.formkiq.client.model.DocumentSearchFolder;
import com.formkiq.client.model.DocumentSearchMatchTag;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchMeta.IndexTypeEnum;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.DocumentSearchTag;
import com.formkiq.client.model.DocumentSearchTags;
import com.formkiq.client.model.DocumentSyncService;
import com.formkiq.client.model.DocumentSyncStatus;
import com.formkiq.client.model.GetDocumentFulltextResponse;
import com.formkiq.client.model.GetDocumentSyncResponse;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchRangeDataType;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.client.model.SearchResultDocumentAttribute;
import com.formkiq.client.model.Watermark;
import com.formkiq.module.lambda.typesense.TypesenseProcessor;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorImpl;
import com.formkiq.aws.dynamodb.folders.FolderIndexRecord;
import com.formkiq.testutils.api.attributes.AddAttributeRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentUploadRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentSyncsRequestBuilder;
import com.formkiq.testutils.api.opensearch.GetFulltextDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.SearchDocumentRequestBuilder;
import com.formkiq.testutils.api.schemas.SetSchemaDocumentRequestBuilder;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.testutils.TestWait.until;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static com.formkiq.testutils.api.ApiAsserts.assertNotTruncated;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for request /search. */
public class DocumentsSearchRequestTest extends AbstractApiClientRequestTest {

  /** {@link DynamoDbService}. */
  private static DynamoDbService db;
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor indexProcessor;

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 10;

  /**
   * Before All.
   */
  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dbConnection = DynamoDbTestServices.getDynamoDbConnection();
    db = new DynamoDbServiceImpl(dbConnection, DOCUMENTS_TABLE);
    indexProcessor = new FolderIndexProcessorImpl(db,
        FolderIndexProcessorExtension.DEFAULT_PARENT_LAST_MODIFIED_UPDATE_INTERVAL_IN_MS);
  }

  private void addAttribute(final String siteId) throws ApiException {
    addAttribute(siteId, "category");
    addAttribute(siteId, "other");
  }

  private void addAttribute(final String siteId, final String attributeKey) throws ApiException {
    new AddAttributeRequestBuilder().keyAsString(attributeKey).submitOk(client, siteId);
  }

  private void addDocument(final String siteId, final List<AddDocumentTag> tags)
      throws ApiException {
    AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder().tags(tags);
    uploadReq.submitOk(client, siteId);
  }

  private String addDocument(final String siteId, final String tagKey, final String tagValue,
      final List<String> tagValues) throws ApiException {

    AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder();

    if (tagKey != null) {
      AddDocumentTag tag = new AddDocumentTag().key(tagKey).value(tagValue).values(tagValues);
      uploadReq.addTag(tag);
    }

    return uploadReq.submitOk(client, siteId).response().getDocumentId();
  }

  private String addDocumentWithAttributes(final String siteId, final String attributeValue)
      throws ApiException {

    AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder();

    AddDocumentAttribute attr = new AddDocumentAttribute(new AddDocumentAttributeStandard()
        .key("category").stringValue(attributeValue).stringValues(null));
    uploadReq.addAttribute(attr);

    return uploadReq.submitOk(client, siteId).response().getDocumentId();
  }

  /**
   * Compare document IDs without depending on result order, preserving duplicate detection.
   *
   * @param expected expected document IDs
   * @param documents search results
   */
  private void assertDocumentIds(final List<String> expected,
      final List<SearchResultDocument> documents) {
    assertEquals(expected.stream().sorted().toList(),
        documents.stream().map(SearchResultDocument::getDocumentId).sorted().toList());
  }

  private String createIndexedDocument(final String siteId, final String path) throws Exception {

    AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder().path(path);
    String documentId = uploadReq.submitOk(client, siteId).response().getDocumentId();
    assertNotNull(documentId);

    Map<String, Object> data =
        Map.of("documentId", Map.of("S", documentId), "path", Map.of("S", path));
    Map<String, Object> document = new DocumentMapToDocument().apply(data);

    AwsServiceCache awsServices = getAwsServices();

    TypesenseProcessor processor = new TypesenseProcessor(awsServices);

    HttpResponse<String> response = processor.addOrUpdate(siteId, documentId, document, false);
    if (response.statusCode() != ApiResponseStatus.SC_CREATED.getStatusCode()) {
      throw new IOException("status: " + response.statusCode() + " body: " + response.body());
    }

    return documentId;
  }

  private DocumentSearchResponse searchByTag(final String siteId, final String key, final String eq,
      final List<String> eqOr, final List<String> documentIds) throws ApiException {
    SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder().query(new DocumentSearch()
        .tag(new DocumentSearchTag().key(key).eq(eq).eqOr(eqOr)).documentIds(documentIds));

    return dsq.submitOk(client, siteId).response();
  }

  /**
   * Search for filename.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testFilenameSearch() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      new AddDocumentRequestBuilder().content().path("myfile").submitOk(client, siteId);
      new AddDocumentRequestBuilder().content().path("dir1/dir2/mysomefile").submitOk(client,
          siteId);
      new AddDocumentRequestBuilder().content().path("someotherfile").submitOk(client, siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().filename(new DocumentSearchFilename().beginsWith("my")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(2, documents.size());
      assertEquals("myfile", documents.get(0).getPath());
      assertEquals("dir1/dir2/mysomefile", documents.get(1).getPath());

      // when
      response = dsq.projection("COUNT").submitOk(client, siteId).response();

      // then
      assertEquals(2, response.getCount());
      assertNotTruncated(response);
      assertTrue(notNull(response.getDocuments()).isEmpty());
    }
  }

  /**
   * Filename prefixes are case insensitive for document and count searches.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testFilenameSearchMixedCasePrefix() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String suffix = ID.uuid();
      String path = "mixedcaseprefix-" + suffix + ".txt";
      new AddDocumentRequestBuilder().content().path(path).submitOk(client, siteId);

      SearchDocumentRequestBuilder request =
          new SearchDocumentRequestBuilder().query(new DocumentSearch()
              .filename(new DocumentSearchFilename().beginsWith("MixedCasePrefix-" + suffix)));

      // when
      DocumentSearchResponse documentResponse = request.submitOk(client, siteId).response();
      DocumentSearchResponse countResponse =
          request.projection("COUNT").submitOk(client, siteId).response();

      // then
      List<String> paths = notNull(documentResponse.getDocuments()).stream()
          .map(SearchResultDocument::getPath).toList();
      assertAll(() -> assertEquals(List.of(path), paths),
          () -> assertEquals(1, countResponse.getCount()), () -> assertNotTruncated(countResponse));
    }
  }

  /**
   * Filename search cannot be combined with document IDs.
   */
  @Test
  public void testFilenameSearchWithDocumentIdsValidation() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      SearchDocumentRequestBuilder request = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().filename(new DocumentSearchFilename().beginsWith("my"))
              .documentIds(List.of(ID.uuid())));

      // when
      var response = request.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"error\":\"'filename' cannot be combined with 'documentIds'\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Search for filename with paging.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testFilenameSearchWithPaging() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      for (int i = 0; i < 15; i++) {
        String path = "mypath_" + String.format("%02d", i);
        new AddDocumentRequestBuilder().content().path(path).submitOk(client, siteId);
      }

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().filename(new DocumentSearchFilename().beginsWith("my")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(10, documents.size());
      assertEquals(
          "mypath_00,mypath_01,mypath_02,mypath_03,mypath_04,mypath_05,"
              + "mypath_06,mypath_07,mypath_08,mypath_09",
          documents.stream().map(SearchResultDocument::getPath).collect(Collectors.joining(",")));
      assertNotNull(response.getNext());

      // when
      response = dsq.next(response.getNext()).submitOk(client, siteId).response();

      // then
      documents = notNull(response.getDocuments());
      assertEquals(5, documents.size());
      assertEquals("mypath_10,mypath_11,mypath_12,mypath_13,mypath_14",
          documents.stream().map(SearchResultDocument::getPath).collect(Collectors.joining(",")));
      assertNull(response.getNext());
    }
  }

  /**
   * Search for folder with paging.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testFolderSearchWithPaging() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      for (int i = 0; i < 15; i++) {
        String path = "myfold_" + String.format("%02d", i) + "/test.txt";
        new AddDocumentRequestBuilder().content().path(path).submitOk(client, siteId);
      }

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().folder(new DocumentSearchFolder().beginsWith("my")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(10, documents.size());
      assertEquals(
          "myfold_00,myfold_01,myfold_02,myfold_03,myfold_04,myfold_05,myfold_06,"
              + "myfold_07,myfold_08,myfold_09",
          documents.stream().map(SearchResultDocument::getPath).collect(Collectors.joining(",")));
      assertNotNull(response.getNext());

      // when
      response = dsq.next(response.getNext()).submitOk(client, siteId).response();

      // then
      documents = notNull(response.getDocuments());
      assertEquals(5, documents.size());
      assertEquals("myfold_10,myfold_11,myfold_12,myfold_13,myfold_14",
          documents.stream().map(SearchResultDocument::getPath).collect(Collectors.joining(",")));
      assertNull(response.getNext());
    }
  }

  /**
   * Search for folder.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testFoldernameSearch() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      new AddDocumentRequestBuilder().content().path("myfile").submitOk(client, siteId);
      new AddDocumentRequestBuilder().content().path("dir1/dir2/mysomefile").submitOk(client,
          siteId);
      new AddDocumentRequestBuilder().content().path("test/dir.txt").submitOk(client, siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().folder(new DocumentSearchFolder().beginsWith("dir")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(2, documents.size());

      assertEquals("dir1", documents.get(0).getPath());
      assertEquals(Boolean.TRUE, documents.get(0).getFolder());

      assertEquals("dir2", documents.get(1).getPath());
      assertEquals(Boolean.TRUE, documents.get(1).getFolder());

      // when
      response = dsq.projection("COUNT").submitOk(client, siteId).response();

      // then
      assertEquals(2, response.getCount());
      assertNotTruncated(response);
      assertTrue(notNull(response.getDocuments()).isEmpty());
    }
  }

  /**
   * Post search by attribute "eqOr".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchAttributeEqOr() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId);

      String documentId0 = addDocumentWithAttributes(siteId, "person");
      String documentId1 = addDocumentWithAttributes(siteId, "other");
      addDocumentWithAttributes(siteId, "another");

      DocumentSearchAttribute attributes =
          new DocumentSearchAttribute().key("category").eqOr(List.of("person", "other"));

      SearchDocumentRequestBuilder dsq0 =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().attribute(attributes));
      SearchDocumentRequestBuilder dsq1 = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().attributes(List.of(attributes)));

      // when
      DocumentSearchResponse response0 = dsq0.submitOk(client, siteId).response();
      DocumentSearchResponse response1 = dsq1.submitOk(client, siteId).response();

      // then
      for (DocumentSearchResponse response : List.of(response0, response1)) {
        List<SearchResultDocument> documents = notNull(response.getDocuments());
        assertEquals(2, documents.size());
        assertEquals(documentId0, documents.get(0).getDocumentId());
        assertEquals(documentId1, documents.get(1).getDocumentId());
      }

      // given - documentId filter
      dsq0.query(new DocumentSearch().attribute(attributes).documentIds(List.of(documentId1)));

      // when
      response0 = dsq0.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response0.getDocuments());
      assertEquals(1, documents.size());
      assertEquals(documentId1, documents.getFirst().getDocumentId());
    }
  }

  /**
   * Post search by attribute "eq".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchAttributeEquality() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId);

      String documentId0 = addDocumentWithAttributes(siteId, "person");
      addDocumentWithAttributes(siteId, "other");

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch()
              .attribute(new DocumentSearchAttribute().key("category").eq("person")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      assertEquals(documentId0, documents.getFirst().getDocumentId());

      // when
      response = dsq.projection("COUNT").submitOk(client, siteId).response();

      // then
      assertEquals(1, response.getCount());
      assertNotTruncated(response);
      assertTrue(notNull(response.getDocuments()).isEmpty());

      // given
      dsq = new SearchDocumentRequestBuilder().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      // when
      response = dsq.submitOk(client, siteId).response();

      // then
      documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      assertEquals(documentId0, documents.getFirst().getDocumentId());
    }
  }

  /**
   * Post search by attribute, responsefields.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchAttributeResponseFields() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId);
      addAttribute(siteId, "playerId");

      AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder();

      AddDocumentAttribute attr0 = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("category").stringValue("person"));
      AddDocumentAttribute attr1 = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("playerId").stringValue("12345"));
      uploadReq.addAttribute(attr0).addAttribute(attr1);

      String documentId0 = uploadReq.submitOk(client, siteId).response().getDocumentId();
      addDocumentWithAttributes(siteId, "other");

      DocumentSearchAttribute attributes =
          new DocumentSearchAttribute().key("category").eq("person");

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().attribute(attributes))
              .responseFields(new SearchResponseFields().addAttributesItem("playerId"));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      assertEquals(documentId0, documents.getFirst().getDocumentId());
      Map<String, SearchResultDocumentAttribute> map = documents.getFirst().getAttributes();
      assertNotNull(map);
      assertEquals(1, map.size());
      assertEquals("12345", String.join(",", notNull(map.get("playerId").getStringValues())));
    }
  }

  /**
   * Post search by composite attributes including DATE range.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDateAttributeCompositeRange() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    new AddAttributeRequestBuilder().keyAsString("category").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsDate("dueDate").submitOk(client, siteId);

    new SetSchemaDocumentRequestBuilder("joe").addRequiredAttribute("category")
        .addRequiredAttribute("dueDate").addCompositeKey("category", "dueDate")
        .submitOk(client, siteId);

    String documentId = new AddDocumentRequestBuilder().content()
        .addAttribute("category", "invoice").addDateAttribute("dueDate", "2026-08-04")
        .submitOk(client, siteId).response().getDocumentId();
    String otherCategoryDocumentId = new AddDocumentRequestBuilder().content()
        .addAttribute("category", "receipt").addDateAttribute("dueDate", "2026-08-04")
        .submitOk(client, siteId).response().getDocumentId();
    String otherDateDocumentId = new AddDocumentRequestBuilder().content()
        .addAttribute("category", "invoice").addDateAttribute("dueDate", "2026-09-01")
        .submitOk(client, siteId).response().getDocumentId();

    DocumentSearchRange range = new DocumentSearchRange().start("2026-08-01").end("2026-08-31")
        .type(SearchRangeDataType.DATE);

    // when
    DocumentSearchResponse response = new SearchDocumentRequestBuilder()
        .query(new DocumentSearch()
            .attributes(List.of(new DocumentSearchAttribute().key("category").eq("invoice"),
                new DocumentSearchAttribute().key("dueDate").range(range))))
        .submitOk(client, siteId).response();

    // then
    List<String> documentIds =
        notNull(response.getDocuments()).stream().map(SearchResultDocument::getDocumentId).toList();
    assertTrue(documentIds.contains(documentId));
    assertFalse(documentIds.contains(otherCategoryDocumentId));
    assertFalse(documentIds.contains(otherDateDocumentId));
  }

  /**
   * Post search by DATE attribute "eq".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDateAttributeEq() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    String attributeKey = "dueDate";
    new AddAttributeRequestBuilder().keyAsDate(attributeKey).submitOk(client, siteId);

    String documentId =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-04")
            .submitOk(client, siteId).response().getDocumentId();
    String otherDocumentId =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-06")
            .submitOk(client, siteId).response().getDocumentId();

    // when
    DocumentSearchResponse response = new SearchDocumentRequestBuilder()
        .attribute(new DocumentSearchAttribute().key(attributeKey).eq("2026-08-04"))
        .submitOk(client, siteId).response();

    // then
    List<String> documentIds =
        notNull(response.getDocuments()).stream().map(SearchResultDocument::getDocumentId).toList();
    assertTrue(documentIds.contains(documentId));
    assertFalse(documentIds.contains(otherDocumentId));

    // when - a singleton attributes array uses the same normalization and execution
    response = new SearchDocumentRequestBuilder()
        .queryAttributes(List.of(new DocumentSearchAttribute().key(attributeKey).eq("2026-08-04")))
        .submitOk(client, siteId).response();

    // then
    assertDocumentIds(List.of(documentId), notNull(response.getDocuments()));

    // when
    DocumentSearchResponse count = new SearchDocumentRequestBuilder()
        .queryAttributes(List.of(new DocumentSearchAttribute().key(attributeKey).eq("2026-08-04")))
        .projection("COUNT").submitOk(client, siteId).response();

    // then
    assertEquals(1, count.getCount());
    assertNotTruncated(count);
  }

  /**
   * Post search by DATE attribute "eq" with invalid date.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDateAttributeEqInvalid() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    String attributeKey = "dueDate";
    new AddAttributeRequestBuilder().keyAsDate(attributeKey).submitOk(client, siteId);

    // when
    var response = new SearchDocumentRequestBuilder()
        .attribute(new DocumentSearchAttribute().key(attributeKey).eq("bad-date"))
        .submit(client, siteId);

    // then
    assertNotNull(response.exception());
    assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
    assertEquals("{\"errors\":[{\"key\":\"dueDate\",\"error\":\"invalid date value\"}]}",
        response.exception().getResponseBody());
  }

  /**
   * Post search by DATE attribute "eqOr".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDateAttributeEqOr() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    String attributeKey = "dueDate";
    new AddAttributeRequestBuilder().keyAsDate(attributeKey).submitOk(client, siteId);

    String documentId0 =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-04")
            .submitOk(client, siteId).response().getDocumentId();
    String documentId1 =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-05")
            .submitOk(client, siteId).response().getDocumentId();
    String otherDocumentId =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-06")
            .submitOk(client, siteId).response().getDocumentId();

    // when
    DocumentSearchResponse response = new SearchDocumentRequestBuilder().attribute(
        new DocumentSearchAttribute().key(attributeKey).eqOr(List.of("2026-08-04", "2026-08-05")))
        .submitOk(client, siteId).response();

    // then
    List<String> documentIds =
        notNull(response.getDocuments()).stream().map(SearchResultDocument::getDocumentId).toList();
    assertTrue(documentIds.contains(documentId0));
    assertTrue(documentIds.contains(documentId1));
    assertFalse(documentIds.contains(otherDocumentId));
  }

  /**
   * Post search by DATE attribute range.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDateAttributeRange() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);

    String attributeKey = "dueDate";
    new AddAttributeRequestBuilder().keyAsDate(attributeKey).submitOk(client, siteId);

    String documentId0 =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-04")
            .submitOk(client, siteId).response().getDocumentId();
    String documentId1 =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-08-31")
            .submitOk(client, siteId).response().getDocumentId();
    String otherDocumentId =
        new AddDocumentRequestBuilder().content().addDateAttribute(attributeKey, "2026-09-01")
            .submitOk(client, siteId).response().getDocumentId();

    DocumentSearchRange range = new DocumentSearchRange().start("2026-08-01").end("2026-08-31")
        .type(SearchRangeDataType.DATE);

    // when
    DocumentSearchResponse response = new SearchDocumentRequestBuilder()
        .attribute(new DocumentSearchAttribute().key(attributeKey).range(range))
        .submitOk(client, siteId).response();

    // then
    List<String> documentIds =
        notNull(response.getDocuments()).stream().map(SearchResultDocument::getDocumentId).toList();
    assertTrue(documentIds.contains(documentId0));
    assertTrue(documentIds.contains(documentId1));
    assertFalse(documentIds.contains(otherDocumentId));
  }

  /**
   * Post search by documentIds only.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchDocumentIdsWithResponseFields() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId);
      addAttribute(siteId, "playerId");

      AddDocumentAttribute attr0 = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("category").stringValue("person"));
      AddDocumentAttribute attr1 = new AddDocumentAttribute(
          new AddDocumentAttributeStandard().key("playerId").stringValue("12345"));
      AddDocumentUploadRequestBuilder uploadReq =
          new AddDocumentUploadRequestBuilder().addAttribute(attr0).addAttribute(attr1)
              .deepLinkPath("https://www.example.com").width("100").height("200");

      String documentId0 = uploadReq.submitOk(client, siteId).response().getDocumentId();
      assertNotNull(documentId0);
      addDocumentWithAttributes(siteId, "other");

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .responseFields(new SearchResponseFields().addAttributesItem("playerId"))
          .query(new DocumentSearch().documentIds(List.of(documentId0)));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      SearchResultDocument doc = documents.getFirst();
      assertEquals(documentId0, doc.getDocumentId());
      assertEquals("https://www.example.com", doc.getDeepLinkPath());
      assertEquals("100", doc.getWidth());
      assertEquals("200", doc.getHeight());
      Map<String, SearchResultDocumentAttribute> map = doc.getAttributes();
      assertNotNull(map);
      assertEquals(1, map.size());
      assertEquals("12345", String.join(",", notNull(map.get("playerId").getStringValues())));
    }
  }

  /**
   * /search meta path.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchExactPath() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String path = "something/path.txt";

      final String documentId = createIndexedDocument(siteId, path);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().path(path)));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
      assertNotNull(documents.getFirst().getInsertedDate());
      assertNotNull(documents.getFirst().getLastModifiedDate());
      assertEquals(documentId, documents.getFirst().getDocumentId());

      // given - meta.path points to a folder rather than a document
      dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().path("something/")));

      // when
      response = dsq.submitOk(client, siteId).response();

      // then
      assertTrue(notNull(response.getDocuments()).isEmpty());
    }
  }

  /**
   * /search meta 'folder' data.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchFolderContents() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequestBuilder uploadReq =
          new AddDocumentUploadRequestBuilder().path("something/path.txt");
      uploadReq.submitOk(client, siteId);

      for (String folder : Arrays.asList("something", "something/", "")) {

        SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
            .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(folder)));

        // when
        DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

        // then
        List<SearchResultDocument> documents = notNull(response.getDocuments());
        assertEquals(1, documents.size());
        assertNotNull(documents.getFirst().getInsertedDate());
        assertNotNull(documents.getFirst().getLastModifiedDate());

        if (folder.isEmpty()) {
          assertEquals("something", documents.getFirst().getPath());
          assertEquals(Boolean.TRUE, documents.getFirst().getFolder());
          assertNotNull(documents.getFirst().getDocumentId());
        } else {
          assertEquals("something/path.txt", documents.getFirst().getPath());
          assertNull(documents.getFirst().getFolder());
          assertNotNull(documents.getFirst().getDocumentId());
        }
      }
    }
  }

  /**
   * /search meta 'folder' data & folders only.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchFolderEntryTypeFilter() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      for (String path : Arrays.asList("a/b/test.txt", "a/c/test.txt", "a/test.txt")) {
        AddDocumentUploadRequestBuilder uploadReq =
            new AddDocumentUploadRequestBuilder().path(path);
        uploadReq.submitOk(client, siteId);
      }

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().indexType(IndexTypeEnum.FOLDER)
              .eq("a").indexFilterBeginsWith("ff#")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(2, documents.size());
      assertNotNull(documents.getFirst().getInsertedDate());
      assertNotNull(documents.getFirst().getLastModifiedDate());

      assertEquals("b", documents.getFirst().getPath());
      assertEquals(Boolean.TRUE, documents.get(0).getFolder());
      assertNotNull(documents.get(0).getDocumentId());

      assertEquals("c", documents.get(1).getPath());
      assertEquals(Boolean.TRUE, documents.get(1).getFolder());
      assertNotNull(documents.get(1).getDocumentId());

      // given
      dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().indexType(IndexTypeEnum.FOLDER)
              .eq("a/").indexFilterBeginsWith("fi#")));

      // when
      response = dsq.submitOk(client, siteId).response();

      // then
      documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());

      assertEquals("a/test.txt", documents.getFirst().getPath());
      assertNull(documents.getFirst().getFolder());
      assertNotNull(documents.getFirst().getDocumentId());
    }
  }

  /**
   * /search meta 'folder' tags / attributes.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchFolderResponseFields() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId);
      String path = "test.txt";
      AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder().path(path)
          .addTag(new AddDocumentTag().key("documentType").value("invoice"))
          .addAttribute(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("category").stringValue("document")))
          .addAttribute(new AddDocumentAttribute(
              new AddDocumentAttributeStandard().key("other").stringValue("thing")));
      uploadReq.submitOk(client, siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch()
              .meta(new DocumentSearchMeta().indexType(IndexTypeEnum.FOLDER).eq("")))
          .responseFields(
              new SearchResponseFields().addTagsItem("documentType").addAttributesItem("category"));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());

      SearchResultDocument document = documents.getFirst();

      Map<String, Object> tags = notNull(document.getTags());
      assertEquals("{documentType=invoice}", tags.toString());

      Map<String, SearchResultDocumentAttribute> attributes = notNull(document.getAttributes());
      assertEquals(1, attributes.size());
      assertEquals("category", String.join(",", attributes.keySet()));

      SearchResultDocumentAttribute category = attributes.get("category");
      assertEquals("document", String.join(",", notNull(category.getStringValues())));
      assertEquals(AttributeValueType.STRING, category.getValueType());
    }
  }

  /**
   * Test POST /search on a folder with a lock key that wasn't removed.
   *
   * @throws Exception Exception
   */
  @Test
  void testSearchFolderWithStaleLock() throws Exception {
    // given
    final String path = "/a/b/test2.pdf";

    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {

      setBearerToken(siteId);

      AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder().path(path);
      uploadReq.submitOk(client, siteId);

      SearchDocumentRequestBuilder req = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta()
              .indexType(DocumentSearchMeta.IndexTypeEnum.FOLDER).eq("/a/")));
      req.responseFields(
          new SearchResponseFields().tags(List.of("test1", "test2")).addAttributesItem("bleh"));

      List<FolderIndexRecord> folders = indexProcessor.createFolders(siteId, path);
      FolderIndexRecord a = folders.get(1);
      final int timeout = 10000;
      DynamoDbKey key = new DynamoDbKey(a.pk(siteId), a.sk(), null, null, null, null);
      assertTrue(db.acquireLock(key, timeout, timeout));

      // when
      List<SearchResultDocument> documents =
          notNull(req.submitOk(client, siteId).response().getDocuments());

      // then
      assertEquals(1, documents.size());
      assertEquals("b", documents.getFirst().getPath());
    }
  }

  /**
   * Text Fulltext search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testSearchFulltext() throws Exception {
    // given
    setBearerToken((String) null);

    final String text = "My Document.docx";
    final String path = "something/My Document.docx";

    String documentId = createIndexedDocument(null, path);

    // when
    GetDocumentSyncResponse syncResponse =
        new GetDocumentSyncsRequestBuilder(documentId).submitOk(client, null).response();

    // then
    assertNotNull(syncResponse.getSyncs());
    assertEquals(1, syncResponse.getSyncs().size());
    assertEquals(DocumentSyncStatus.COMPLETE, syncResponse.getSyncs().getFirst().getStatus());
    assertEquals(DocumentSyncService.TYPESENSE, syncResponse.getSyncs().getFirst().getService());

    // when
    GetDocumentFulltextResponse getResponse = until("fulltext for document '" + documentId + "'",
        () -> new GetFulltextDocumentRequestBuilder(documentId).submitOk(client, null).response(),
        Objects::nonNull);

    // then
    assertEquals(path, getResponse.getPath());

    // given
    SearchDocumentRequestBuilder dsq =
        new SearchDocumentRequestBuilder().query(new DocumentSearch().text(text));

    // when
    List<SearchResultDocument> documents = until("fulltext search result",
        () -> dsq.getDocuments(client, null), results -> !results.isEmpty());

    // then
    assertEquals(1, documents.size());
    assertEquals(documentId, documents.getFirst().getDocumentId());
    assertEquals(path, documents.getFirst().getPath());
  }

  /**
   * Text Fulltext search no data.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchFulltextWithoutMatches() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      String text = ID.uuid();

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().text(text));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      assertEquals(0, notNull(response.getDocuments()).size());
    }
  }

  /** Prefer the largest usable composite and fall back when its leading operator cannot match. */
  @Test
  public void testSearchMultipleAttributesCompositeSelection() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    for (String key : List.of("customer", "status", "region", "category")) {
      new AddAttributeRequestBuilder().keyAsString(key).submitOk(client, siteId);
    }

    new SetSchemaDocumentRequestBuilder("composite-selection").addCompositeKey("customer", "status")
        .addCompositeKey("customer", "status", "region").submitOk(client, siteId);

    var document0 = new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
        .addAttribute("status", "approved").addAttribute("region", "us")
        .addAttribute("category", "invoice").getDocument(client, siteId);

    // dummy document that should be filtered
    new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
        .addAttribute("status", "approved").addAttribute("region", "us")
        .addAttribute("category", "invoice2").getDocument(client, siteId);

    for (boolean equality : List.of(true, false)) {
      // given
      DocumentSearchAttribute customer = new DocumentSearchAttribute().key("customer");
      if (equality) {
        customer.eq("123");
      } else {
        customer.beginsWith("12");
      }
      List<DocumentSearchAttribute> attributes =
          List.of(new DocumentSearchAttribute().key("category").eq("invoice"), customer,
              new DocumentSearchAttribute().key("region").eq("us"),
              new DocumentSearchAttribute().key("status").eq("approved"));

      // when
      DocumentSearchResponse response = new SearchDocumentRequestBuilder()
          .queryAttributes(attributes).submitOk(client, siteId).response();

      // then
      assertDocumentIds(List.of(document0.documentId()), notNull(response.getDocuments()));
      assertEquals(equality ? "customer::status::region" : "category", Objects
          .requireNonNull(response.getDocuments().getFirst().getMatchedAttribute()).getKey());
    }
  }

  /** Explicit document IDs return all matching documents even when limit is smaller. */
  @Test
  public void testSearchMultipleAttributesDocumentIdsIgnoresLimit() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsString("category").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsString("status").submitOk(client, siteId);
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      expected.add(new AddDocumentRequestBuilder().content().addAttribute("category", "invoice")
          .addAttribute("status", "approved").submitOk(client, siteId).response().getDocumentId());
    }
    List<String> ids = new ArrayList<>(expected);
    ids.add(new AddDocumentRequestBuilder().content().addAttribute("category", "invoice")
        .addAttribute("status", "pending").submitOk(client, siteId).response().getDocumentId());
    ids.add(expected.getFirst());
    ids.add(ID.uuid());
    List<DocumentSearchAttribute> attributes =
        List.of(new DocumentSearchAttribute().key("category").eq("invoice"),
            new DocumentSearchAttribute().key("status").eq("approved"));

    // when
    DocumentSearchResponse response = new SearchDocumentRequestBuilder()
        .query(new DocumentSearch().attributes(attributes).documentIds(ids)).limit("1")
        .submitOk(client, siteId).response();

    // then
    assertEquals(expected, notNull(response.getDocuments()).stream()
        .map(SearchResultDocument::getDocumentId).toList());
    assertNull(response.getNext());
    assertNull(response.getPrevious());
  }

  /** Both attributes use EQ OR, including missing values, with AND matching and no duplicates. */
  @Test
  public void testSearchMultipleAttributesEqOr() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsString("customer").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsString("status").submitOk(client, siteId);

    List<DocumentArtifact> expected = new ArrayList<>();
    for (String customer : List.of("123", "456")) {
      for (String status : List.of("approved", "pending")) {
        expected.add(new AddDocumentRequestBuilder().content().addAttribute("customer", customer)
            .addAttribute("status", status).getDocument(client, siteId));
      }
    }

    expected.add(
        new AddDocumentRequestBuilder().content().addAttribute("customer", List.of("123", "456"))
            .addAttribute("status", List.of("approved", "pending")).getDocument(client, siteId));
    new AddDocumentRequestBuilder().content().addAttribute("customer", "789")
        .addAttribute("status", "approved").submitOk(client, siteId);
    new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
        .addAttribute("status", "rejected").submitOk(client, siteId);
    new AddDocumentRequestBuilder().content().addAttribute("customer", "123").submitOk(client,
        siteId);

    List<DocumentSearchAttribute> attributes = List.of(
        new DocumentSearchAttribute().key("customer").eqOr(List.of("missing", "456", "123")),
        new DocumentSearchAttribute().key("status")
            .eqOr(List.of("missing", "pending", "approved")));

    // when
    List<SearchResultDocument> documents =
        new SearchDocumentRequestBuilder().queryAttributes(attributes).getDocuments(client, siteId);

    // then
    assertDocumentIds(expected.stream().map(DocumentArtifact::documentId).toList(), documents);

    // when
    DocumentSearchResponse count = new SearchDocumentRequestBuilder().queryAttributes(attributes)
        .projection("COUNT").submitOk(client, siteId).response();

    // then
    assertEquals(expected.size(), count.getCount());
    assertNotTruncated(count);
  }

  /** Multi-attribute pagination returns every match once, with and without a partial composite. */
  @Test
  public void testSearchMultipleAttributesPagination() throws Exception {
    for (boolean composite : List.of(false, true)) {
      // given
      String siteId = ID.uuid();
      setBearerToken(siteId);
      for (String key : List.of("customer", "status", "region")) {
        new AddAttributeRequestBuilder().keyAsString(key).submitOk(client, siteId);
      }
      if (composite) {
        new SetSchemaDocumentRequestBuilder("pagination").addCompositeKey("customer", "status")
            .submitOk(client, siteId);
      }
      List<String> expected = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        expected.add(new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
            .addAttribute("status", List.of("approved", "review")).addAttribute("region", "us")
            .submitOk(client, siteId).response().getDocumentId());
      }
      new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
          .addAttribute("status", "approved").addAttribute("region", "eu").submitOk(client, siteId);
      List<DocumentSearchAttribute> attributes =
          List.of(new DocumentSearchAttribute().key("customer").eq("123"),
              new DocumentSearchAttribute().key("status").eqOr(List.of("approved", "review")),
              new DocumentSearchAttribute().key("region").eq("us"));
      List<SearchResultDocument> documents = new ArrayList<>();
      String next = null;
      int pages = 0;
      do {
        // when
        DocumentSearchResponse response = new SearchDocumentRequestBuilder()
            .queryAttributes(attributes).limit("1").next(next).submitOk(client, siteId).response();

        // then
        List<SearchResultDocument> page = notNull(response.getDocuments());
        assertTrue(page.size() <= 1, "Each page must respect the limit");
        assertNotTruncated(response);
        documents.addAll(page);
        next = response.getNext();
        assertTrue(++pages < 10, "Pagination must terminate");
      } while (next != null);
      assertTrue(pages > 1, "Matching documents must span multiple pages");
      assertDocumentIds(expected, documents);
    }
  }

  /** A composite covering two of three criteria drives matching and count. */
  @Test
  public void testSearchMultipleAttributesPartialCompositeKey() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    for (String key : List.of("customer", "status", "region")) {
      new AddAttributeRequestBuilder().keyAsString(key).submitOk(client, siteId);
    }
    new SetSchemaDocumentRequestBuilder("partial-composite").addCompositeKey("customer", "status")
        .submitOk(client, siteId);
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      expected.add(new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
          .addAttribute("status", List.of("approved", "review")).addAttribute("region", "us")
          .submitOk(client, siteId).response().getDocumentId());
    }
    new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
        .addAttribute("status", "approved").addAttribute("region", "eu").submitOk(client, siteId);
    new AddDocumentRequestBuilder().content().addAttribute("customer", "123")
        .addAttribute("status", "approved").submitOk(client, siteId);
    new AddDocumentRequestBuilder().content().addAttribute("customer", "456")
        .addAttribute("status", "approved").addAttribute("region", "us").submitOk(client, siteId);
    for (DocumentSearchAttribute status : List.of(
        new DocumentSearchAttribute().key("status").eq("approved"),
        new DocumentSearchAttribute().key("status").eqOr(List.of("review", "approved")),
        new DocumentSearchAttribute().key("status").beginsWith("approv"),
        new DocumentSearchAttribute().key("status")
            .range(new DocumentSearchRange().start("approved").end("review")))) {
      // given - neither the first criterion nor the request order identifies the composite
      List<DocumentSearchAttribute> attributes =
          List.of(new DocumentSearchAttribute().key("region").eq("us"), status,
              new DocumentSearchAttribute().key("customer").eq("123"));

      // when
      List<SearchResultDocument> documents = new SearchDocumentRequestBuilder()
          .queryAttributes(attributes).getDocuments(client, siteId);

      // then
      assertDocumentIds(expected, documents);
      for (SearchResultDocument document : documents) {
        assertEquals("customer::status",
            Objects.requireNonNull(document.getMatchedAttribute()).getKey());
      }

      // when
      DocumentSearchResponse count = new SearchDocumentRequestBuilder().queryAttributes(attributes)
          .projection("COUNT").submitOk(client, siteId).response();

      // then
      assertEquals(2, count.getCount());
      assertNotTruncated(count);
    }
  }

  /**
   * Multi-attribute fallback supports typed criteria and count without a schema.
   */
  @Test
  public void testSearchMultipleAttributesWithoutCompositeKey() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    new AddAttributeRequestBuilder().keyAsString("category").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsDate("dueDate").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsBoolean("approved").submitOk(client, siteId);
    new AddAttributeRequestBuilder().keyAsNumber("amount").submitOk(client, siteId);
    List<String> expected = new ArrayList<>();
    for (String date : List.of("2026-08-04", "2026-08-06")) {
      expected.add(new AddDocumentRequestBuilder().content().addAttribute("category", "invoice")
          .addDateAttribute("dueDate", date).addAttribute("approved", true)
          .addAttribute("amount", new java.math.BigDecimal("123.5")).submitOk(client, siteId)
          .response().getDocumentId());
    }
    new AddDocumentRequestBuilder().content().addAttribute("category", "invoice")
        .addDateAttribute("dueDate", "2026-08-05").addAttribute("approved", false)
        .addAttribute("amount", new java.math.BigDecimal("123.5")).submitOk(client, siteId);
    DocumentSearchAttribute date =
        new DocumentSearchAttribute().key("dueDate").range(new DocumentSearchRange()
            .start("2026-08-01").end("2026-08-31").type(SearchRangeDataType.DATE));
    for (boolean dateFirst : List.of(false, true)) {
      // given - drive the search by category or date
      List<DocumentSearchAttribute> attributes =
          new ArrayList<>(List.of(new DocumentSearchAttribute().key("category").eq("invoice"), date,
              new DocumentSearchAttribute().key("approved").eq("true"),
              new DocumentSearchAttribute().key("amount").eq("123.5")));
      if (dateFirst) {
        Collections.swap(attributes, 0, 1);
      }

      // when
      List<SearchResultDocument> documents = new SearchDocumentRequestBuilder()
          .queryAttributes(attributes).getDocuments(client, siteId);

      // then
      assertDocumentIds(expected, documents);

      // when
      DocumentSearchResponse count = new SearchDocumentRequestBuilder().queryAttributes(attributes)
          .projection("COUNT").submitOk(client, siteId).response();

      // then
      assertEquals(2, count.getCount());
      assertNotTruncated(count);
    }
  }

  /**
   * /search rejects invalid and incompatible COUNT projections.
   *
   */
  @Test
  public void testSearchRejectsInvalidCountProjection() {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    SearchDocumentRequestBuilder request = new SearchDocumentRequestBuilder()
        .query(new DocumentSearch().tag(new DocumentSearchTag().key("category")));

    // when
    var response = request.projection("INVALID").submit(client, siteId);

    // then
    assertNotNull(response.exception());
    assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
    assertEquals("{\"message\":\"Unsupported projection 'INVALID'\"}",
        response.exception().getResponseBody());

    // when
    response = request.limit("1").projection("COUNT").submit(client, siteId);

    // then
    assertNotNull(response.exception());
    assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
    assertEquals("{\"errors\":[{\"error\":\"projection=COUNT cannot be combined with 'limit'\"}]}",
        response.exception().getResponseBody());
  }

  /**
   * Post search by 'meta' and 'attributes'.
   *
   */
  @Test
  public void testSearchRejectsMetaWithAttribute() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().eq(""))
              .attribute(new DocumentSearchAttribute().key("category").eq("person")));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"error\":\"'meta' cannot be combined with 'tags' or 'attributes'\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Post search by 'meta' and 'tags'.
   *
   */
  @Test
  public void testSearchRejectsMetaWithTag() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch()
              .meta(new DocumentSearchMeta().eq("")).tag(new DocumentSearchTag().key("category")));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"error\":\"'meta' cannot be combined with 'tags' or 'attributes'\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Invalid search.
   *
   */
  @Test
  public void testSearchRejectsMissingQuery() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      SearchDocumentRequestBuilder req = new SearchDocumentRequestBuilder();

      // when
      var response = req.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals("{\"errors\":[{\"error\":\"invalid body\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Missing Tag Key.
   *
   */
  @Test
  public void testSearchRejectsMissingTagKey() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags()));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals("{\"errors\":[{\"key\":\"tag/key\",\"error\":\"tag 'key' is required\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Invalid POST search by beginsWith / range query.
   *
   */
  @Test
  public void testSearchRejectsMultipleTagRanges() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      DocumentSearchTags range = new DocumentSearchTags().key("date")
          .range(new DocumentSearchRange().start("2024-03-10").end("2024-03-20"));

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().addTagsItem(range).addTagsItem(range));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"tags\"," + "\"error\":\"multiple tags search not supported\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Test Setting multiple tags.
   *
   */
  @Test
  public void testSearchRejectsMultipleTags() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags().key("test"))
              .addTagsItem(new DocumentSearchTags().key("test")));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"key\":\"tags\",\"error\":\"multiple tags search not supported\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Invalid POST search by range query.
   *
   */
  @Test
  public void testSearchRejectsRangeWithoutEnd() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().tag(new DocumentSearchTag()
              .key("date").range(new DocumentSearchRange().start("2024-03-10"))));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals("{\"errors\":[{\"key\":\"range/end\",\"error\":\"range end is required\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Post search by 'text' and 'tags' / 'attributes'.
   *
   */
  @Test
  public void testSearchRejectsTextWithTagsOrAttributes() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().text("123")
              .attribute(new DocumentSearchAttribute().key("test").eq("123")));

      // when
      var response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals(
          "{\"errors\":[{\"error\":\"'text' search cannot be combined with 'attributes'\"}]}",
          response.exception().getResponseBody());

      // given
      dsq = new SearchDocumentRequestBuilder().query(
          new DocumentSearch().text("123").tag(new DocumentSearchTag().key("test").eq("123")));

      // when
      response = dsq.submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals("{\"errors\":[{\"error\":\"'text' search cannot be combined with 'tags'\"}]}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Valid POST search by eq tagValue and TOO many DocumentId.
   *
   */
  @Test
  public void testSearchRejectsTooManyDocumentIds() {
    final int count = 101;

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      List<String> ids = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        ids.add(ID.uuid());
      }

      // when
      var response = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("test")).documentIds(ids))
          .submit(client, siteId);

      // then
      assertNotNull(response.exception());
      assertEquals(SC_BAD_REQUEST.getStatusCode(), response.exception().getCode());
      assertEquals("{\"message\":\"Maximum number of DocumentIds is 100\"}",
          response.exception().getResponseBody());
    }
  }

  /**
   * Test Setting multiple tags.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchSingleTagInTagsArray() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags().key("test")));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      assertEquals(0, notNull(response.getDocuments()).size());
    }
  }

  /**
   * /search COUNT projection.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagCount() throws Exception {
    // given
    String siteId = ID.uuid();
    setBearerToken(siteId);
    String tagKey = ID.uuid();
    String tagValue = "person";
    addDocument(siteId, tagKey, tagValue, null);
    addDocument(siteId, tagKey, tagValue, null);
    addDocument(siteId, tagKey, "other", null);
    SearchDocumentRequestBuilder request = new SearchDocumentRequestBuilder()
        .query(new DocumentSearch().tag(new DocumentSearchTag().key(tagKey).eq(tagValue)));

    // when
    DocumentSearchResponse response =
        request.projection("COUNT").submitOk(client, siteId).response();

    // then
    assertEquals(2, response.getCount());
    assertNotTruncated(response);
    assertTrue(notNull(response.getDocuments()).isEmpty());
    assertNull(response.getNext());
    assertNull(response.getPrevious());
  }

  /**
   * Valid POST search by eq tagValue with > 10 Document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagDocumentIdsIgnoreDefaultLimit() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      final int count = 13;
      final String tagKey = "category";
      final String tagvalue = "person";
      List<String> documentIds = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        String documentId = addDocument(siteId, tagKey, tagvalue, null);
        documentIds.add(documentId);
      }

      // when
      DocumentSearchResponse response = searchByTag(siteId, tagKey, tagvalue, null, documentIds);

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(count, documents.size());

      // given not search by documentIds should be limited to 10
      // when
      response = searchByTag(siteId, tagKey, tagvalue, null, null);

      // then
      final int ten = 10;
      assertEquals(ten, notNull(response.getDocuments()).size());
    }
  }

  /**
   * Valid POST search by eq/eqOr tagValue and valid/invalid DocumentId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagEqualityFiltersDocumentIds() throws Exception {
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, ID.uuid())) {
        // given
        setBearerToken(siteId);

        final String tagKey = "category";
        final String tagvalue = "person";

        String documentId = addDocument(siteId, tagKey, tagvalue, null);
        addDocument(siteId, tagKey, tagvalue + "!", null);

        List<String> eqOr = null;
        String eq = "person";

        if ("eqOr".equals(op)) {
          eq = null;
          eqOr = (List.of("person"));
        }

        // when
        DocumentSearchResponse response =
            searchByTag(siteId, "category", eq, eqOr, Collections.singletonList(documentId));

        // then
        List<SearchResultDocument> documents = response.getDocuments();
        assertNotNull(documents);
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.getFirst().getDocumentId());
        assertEquals("joesmith", documents.getFirst().getUserId());
        assertNotNull(documents.getFirst().getInsertedDate());

        DocumentSearchMatchTag matchedTag = documents.getFirst().getMatchedTag();
        assertNotNull(matchedTag);
        assertEquals("category", matchedTag.getKey());
        assertEquals("person", matchedTag.getValue());
        assertEquals("USERDEFINED", matchedTag.getType());

        // when
        response = searchByTag(siteId, "category", eq, eqOr, List.of("123"));

        // then
        documents = notNull(response.getDocuments());
        assertEquals(0, documents.size());
      }
    }
  }

  /**
   * Valid POST search by eq tagValues.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagEqualityMatchesOneOfMultipleValues() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);
      String tagKey = "category";

      String documentId = addDocument(siteId, tagKey, null, Arrays.asList("abc", "xyz"));

      // when
      DocumentSearchResponse response =
          searchByTag(siteId, "category", "xyz", null, Collections.singletonList(documentId));

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertNotNull(documents);
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.getFirst().getDocumentId());
      assertEquals("joesmith", documents.getFirst().getUserId());
      assertNotNull(documents.getFirst().getInsertedDate());

      DocumentSearchMatchTag matchedTag = documents.getFirst().getMatchedTag();
      assertNotNull(matchedTag);
      assertEquals("USERDEFINED", matchedTag.getType());
      assertEquals("category", matchedTag.getKey());
      assertEquals("xyz", matchedTag.getValue());
      assertNull(response.getNext());
      assertNull(response.getPrevious());
    }
  }

  /**
   * Valid POST search by eq tagValue.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagEqualityWithDocumentIds() throws Exception {
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, ID.uuid())) {
        // given
        setBearerToken(siteId);

        final String tagKey = "category";
        final String tagvalue = "person";

        AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder()
            .addTag(new AddDocumentTag().key(tagKey).value(tagvalue));
        String documentId = uploadReq.submitOk(client, siteId).response().getDocumentId();

        SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
            .query(new DocumentSearch().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Collections.singletonList(documentId)));

        if ("eqOr".equals(op)) {
          dsq.query(new DocumentSearch()
              .tag(new DocumentSearchTag().key("category").eqOr(List.of("person")))
              .documentIds(List.of(documentId)));
        }

        // when
        DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

        // then
        List<SearchResultDocument> documents = notNull(response.getDocuments());
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.getFirst().getDocumentId());
        assertEquals("joesmith", documents.getFirst().getUserId());
        assertNotNull(documents.getFirst().getInsertedDate());

        DocumentSearchMatchTag matchedTag = documents.getFirst().getMatchedTag();
        assertNotNull(matchedTag);
        assertEquals("USERDEFINED", matchedTag.getType());
        assertEquals("category", matchedTag.getKey());
        assertEquals("person", matchedTag.getValue());
        assertNull(response.getNext());
        assertNull(response.getPrevious());
      }
    }
  }

  /**
   * Tag equality search with no matching documents.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagEqualityWithoutMatches() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      SearchDocumentRequestBuilder req = new SearchDocumentRequestBuilder()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("category").eq("person")));

      // when
      DocumentSearchResponse response = req.submitOk(client, siteId).response();

      // then
      assertEquals(0, notNull(response.getDocuments()).size());
    }
  }

  /**
   * Valid POST search by range query.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagRange() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final String documentId0 = addDocument(siteId, "date", "2024-03-10", null);
      final String documentId1 = addDocument(siteId, "date", "2024-03-12", null);
      addDocument(siteId, "date", "2024-03-14", null);

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().tag(new DocumentSearchTag()
              .key("date").range(new DocumentSearchRange().start("2024-03-10").end("2024-03-12"))));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(2, documents.size());
      assertEquals(documentId0, documents.get(0).getDocumentId());
      assertEquals(documentId1, documents.get(1).getDocumentId());
    }
  }

  /**
   * /search and return responseFields.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchTagResponseFields() throws Exception {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      final int count = 3;
      final String tagKey0 = "category";
      final String tagvalue0 = "person";
      final String tagKey1 = "playerId";
      final String tagvalue1 = "111";

      for (int i = 0; i < count; i++) {

        List<AddDocumentTag> tags =
            Arrays.asList(new AddDocumentTag().key(tagKey0).value(tagvalue0),
                new AddDocumentTag().key(tagKey1).value(tagvalue1));
        addDocument(siteId, tags);
      }

      SearchDocumentRequestBuilder dsq = new SearchDocumentRequestBuilder()
          .query(
              new DocumentSearch().addTagsItem(new DocumentSearchTags().key(tagKey0).eq(tagvalue0)))
          .responseFields(new SearchResponseFields().tags(List.of(tagKey1)));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(count, documents.size());

      documents.forEach(doc -> {
        assertNotNull(doc.getTags());
        assertEquals(1, doc.getTags().size());
        assertEquals(tagvalue1, doc.getTags().get(tagKey1));
      });
    }
  }

  /**
   * Add watermark attribute types.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testSearchWatermarkAttribute() throws Exception {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      setBearerToken(siteId);

      new AddAttributeRequestBuilder().keyAsWatermark("wm1", new Watermark().text("123"))
          .submitOk(client, siteId);

      AddDocumentUploadRequestBuilder uploadReq = new AddDocumentUploadRequestBuilder();

      AddDocumentAttribute attr0 =
          new AddDocumentAttribute(new AddDocumentAttributeStandard().key("wm1"));
      uploadReq.addAttribute(attr0);

      uploadReq.submitOk(client, siteId);

      DocumentSearchAttribute attributes = new DocumentSearchAttribute().key("wm1");

      SearchDocumentRequestBuilder dsq =
          new SearchDocumentRequestBuilder().query(new DocumentSearch().attribute(attributes))
              .responseFields(new SearchResponseFields().addAttributesItem("wm1"));

      // when
      DocumentSearchResponse response = dsq.submitOk(client, siteId).response();

      // then
      List<SearchResultDocument> documents = notNull(response.getDocuments());
      assertEquals(1, documents.size());
    }
  }

}
