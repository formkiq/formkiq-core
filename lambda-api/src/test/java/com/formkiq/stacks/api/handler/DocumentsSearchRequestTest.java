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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_BAD_REQUEST;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_PAYMENT;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.model.DocumentSyncStatus;
import com.formkiq.client.model.GetDocumentSyncResponse;
import com.formkiq.module.typesense.TypeSenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.model.DocumentMapToDocument;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchAttribute;
import com.formkiq.client.model.DocumentSearchMatchTag;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchMeta.IndexTypeEnum;
import com.formkiq.client.model.DocumentSearchRange;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.client.model.DocumentSearchTag;
import com.formkiq.client.model.DocumentSearchTags;
import com.formkiq.client.model.SearchResponseFields;
import com.formkiq.client.model.SearchResultDocument;
import com.formkiq.module.lambda.typesense.TypesenseProcessor;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TypesenseExtension;

/** Unit Tests for request /search. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
@ExtendWith(TypesenseExtension.class)
public class DocumentsSearchRequestTest extends AbstractApiClientRequestTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  private void addAttribute(final String siteId, final String attribute) throws ApiException {
    AddAttributeRequest req =
        new AddAttributeRequest().attribute(new AddAttribute().key(attribute));
    this.attributesApi.addAttribute(req, siteId);
  }

  private String addDocument(final String siteId, final List<AddDocumentTag> tags)
      throws ApiException {

    AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest().tags(tags);

    return this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null).getDocumentId();
  }

  private String addDocument(final String siteId, final String tagKey, final String tagValue,
      final List<String> tagValues) throws ApiException {

    AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest();

    if (tagKey != null) {
      AddDocumentTag tag = new AddDocumentTag().key(tagKey).value(tagValue).values(tagValues);
      uploadReq.addTagsItem(tag);
    }

    return this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null).getDocumentId();
  }

  private String addDocumentWithAttributes(final String siteId, final String attributeKey,
      final String attributeValue, final List<String> attributeValues) throws ApiException {

    AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest();

    if (attributeKey != null) {
      AddDocumentAttribute attr = new AddDocumentAttribute().key(attributeKey)
          .stringValue(attributeValue).stringValues(attributeValues);
      uploadReq.addAttributesItem(attr);
    }

    return this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null).getDocumentId();
  }

  private DocumentSearchResponse query(final String siteId, final String key, final String eq,
      final List<String> eqOr, final List<String> documentIds) throws ApiException {
    DocumentSearchRequest dsq = new DocumentSearchRequest().query(new DocumentSearch()
        .tag(new DocumentSearchTag().key(key).eq(eq).eqOr(eqOr)).documentIds(documentIds));

    return this.searchApi.documentSearch(dsq, siteId, null, null, null);
  }

  private String saveDocument(final String siteId, final String path) throws Exception {

    getAwsServices().environment().put("TYPESENSE_HOST",
        "http://localhost:" + TypesenseExtension.getMappedPort());
    getAwsServices().environment().put("TYPESENSE_API_KEY", API_KEY);
    getAwsServices().environment().put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);

    AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest().path(path);
    String documentId =
        this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null).getDocumentId();

    Map<String, Object> data =
        Map.of("documentId", Map.of("S", documentId), "path", Map.of("S", path));
    Map<String, Object> document = new DocumentMapToDocument().apply(data);

    TypesenseProcessor processor = new TypesenseProcessor(getAwsServices());

    TypeSenseService typeSenseService = getAwsServices().getExtension(TypeSenseService.class);
    HttpResponse<String> healthy = typeSenseService.isHealthy();
    if (healthy.statusCode() != ApiResponseStatus.SC_OK.getStatusCode()) {
      throw new IOException("status: " + healthy.statusCode() + " body: " + healthy.body());
    }

    HttpResponse<String> response =
        processor.addOrUpdate(siteId, documentId, document, "joesmith", false);
    if (response.statusCode() != ApiResponseStatus.SC_CREATED.getStatusCode()) {
      throw new IOException("status: " + response.statusCode() + " body: " + response.body());
    }

    return documentId;
  }

  /**
   * Invalid search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      DocumentSearchRequest req = new DocumentSearchRequest();

      // when
      try {
        this.searchApi.documentSearch(req, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"error\":\"invalid body\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Valid GET search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchRequest req = new DocumentSearchRequest()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key("category").eq("person")));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(req, siteId, null, null, null);

      // then
      assertEquals(0, response.getDocuments().size());
    }
  }

  /**
   * Valid POST search by eq tagValue.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest03() throws Exception {
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        setBearerToken(siteId);

        final String tagKey = "category";
        final String tagvalue = "person";

        AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest()
            .addTagsItem(new AddDocumentTag().key(tagKey).value(tagvalue));
        String documentId = this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null)
            .getDocumentId();

        DocumentSearchRequest dsq = new DocumentSearchRequest()
            .query(new DocumentSearch().tag(new DocumentSearchTag().key("category").eq("person"))
                .documentIds(Collections.singletonList(documentId)));

        if ("eqOr".equals(op)) {
          dsq.getQuery().getTag().eq(null).eqOr(List.of("person"));
        }

        // when
        DocumentSearchResponse response =
            this.searchApi.documentSearch(dsq, siteId, null, null, null);

        // then
        List<SearchResultDocument> documents = response.getDocuments();
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.get(0).getDocumentId());
        assertEquals("joesmith", documents.get(0).getUserId());
        assertNotNull(documents.get(0).getInsertedDate());

        DocumentSearchMatchTag matchedTag = documents.get(0).getMatchedTag();
        assertEquals("USERDEFINED", matchedTag.getType());
        assertEquals("category", matchedTag.getKey());
        assertEquals("person", matchedTag.getValue());
        assertNull(response.getNext());
        assertNull(response.getPrevious());
      }
    }
  }

  /**
   * Valid POST search by eq tagValues.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest05() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);
      String tagKey = "category";

      String documentId = addDocument(siteId, tagKey, null, Arrays.asList("abc", "xyz"));

      // when
      DocumentSearchResponse response =
          query(siteId, "category", "xyz", null, Collections.singletonList(documentId));

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).getDocumentId());
      assertEquals("joesmith", documents.get(0).getUserId());
      assertNotNull(documents.get(0).getInsertedDate());

      DocumentSearchMatchTag matchedTag = documents.get(0).getMatchedTag();
      assertEquals("USERDEFINED", matchedTag.getType());
      assertEquals("category", matchedTag.getKey());
      assertEquals("xyz", matchedTag.getValue());
      assertNull(response.getNext());
      assertNull(response.getPrevious());
    }
  }

  /**
   * Valid POST search by eq/eqOr tagValue and valid/invalid DocumentId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest06() throws Exception {
    for (String op : Arrays.asList("eq", "eqOr")) {
      for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
        // given
        setBearerToken(siteId);

        final String tagKey = "category";
        final String tagvalue = "person";

        String documentId = addDocument(siteId, null, null, null);

        List<String> eqOr = null;
        String eq = "person";

        if ("eqOr".equals(op)) {
          eq = null;
          eqOr = (List.of("person"));
          // dsq.tag().eq(null).eqOr(Arrays.asList("person"));
        }

        for (String v : Arrays.asList("", "!")) {
          if (v.isEmpty()) {
            this.tagsApi.addDocumentTags(documentId, new AddDocumentTagsRequest()
                .addTagsItem(new AddDocumentTag().key(tagKey).value(tagvalue)), siteId, null);
          } else {
            addDocument(siteId, tagKey, tagvalue + v, null);
          }
        }

        // when
        DocumentSearchResponse response =
            query(siteId, "category", eq, eqOr, Collections.singletonList(documentId));

        // then
        List<SearchResultDocument> documents = response.getDocuments();
        assertEquals(1, documents.size());
        assertEquals(documentId, documents.get(0).getDocumentId());
        assertEquals("joesmith", documents.get(0).getUserId());
        assertNotNull(documents.get(0).getInsertedDate());

        DocumentSearchMatchTag matchedTag = documents.get(0).getMatchedTag();
        assertEquals("category", matchedTag.getKey());
        assertEquals("person", matchedTag.getValue());
        assertEquals("USERDEFINED", matchedTag.getType());

        // when
        response = query(siteId, "category", eq, eqOr, List.of("123"));

        // then
        documents = response.getDocuments();
        assertEquals(0, documents.size());
      }
    }
  }

  /**
   * Valid POST search by eq tagValue and TOO many DocumentId.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest07() throws Exception {
    final int count = 101;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      List<String> ids = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        ids.add(UUID.randomUUID().toString());
      }

      // when
      try {
        query(siteId, "test", null, null, ids);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Maximum number of DocumentIds is 100\"}", e.getResponseBody());
      }
    }
  }

  /**
   * Valid POST search by eq tagValue with > 10 Document.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest08() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
      DocumentSearchResponse response = query(siteId, tagKey, tagvalue, null, documentIds);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(count, documents.size());

      // given not search by documentIds should be limited to 10
      // when
      response = query(siteId, tagKey, tagvalue, null, null);

      // then
      final int ten = 10;
      assertEquals(ten, response.getDocuments().size());
    }
  }

  /**
   * Test Setting multiple tags.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest09() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags().key("test")));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      assertEquals(0, response.getDocuments().size());
    }
  }

  /**
   * Test Setting multiple tags.
   *
   */
  @Test
  public void testHandleSearchRequest10() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags().key("test"))
              .addTagsItem(new DocumentSearchTags().key("test")));

      // when
      try {
        this.searchApi.documentSearch(dsq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_PAYMENT.getStatusCode(), e.getCode());
        assertEquals("{\"message\":\"Feature only available in FormKiQ Enterprise\"}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Missing Tag Key.
   *
   */
  @Test
  public void testHandleSearchRequest11() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().addTagsItem(new DocumentSearchTags()));

      // when
      try {
        this.searchApi.documentSearch(dsq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"tag/key\",\"error\":\"attribute is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * /search and return responseFields.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest12() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().tag(new DocumentSearchTag().key(tagKey0).eq(tagvalue0)))
          .responseFields(new SearchResponseFields().tags(List.of(tagKey1)));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(count, documents.size());

      documents.forEach(doc -> {
        assertEquals(1, doc.getTags().size());
        assertEquals(tagvalue1, doc.getTags().get(tagKey1));
      });
    }
  }

  /**
   * /search meta 'folder' data.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest13() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      AddDocumentUploadRequest uploadReq =
          new AddDocumentUploadRequest().path("something/path.txt");
      this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null);

      for (String folder : Arrays.asList("something", "something/", "")) {

        DocumentSearchRequest dsq = new DocumentSearchRequest()
            .query(new DocumentSearch().meta(new DocumentSearchMeta().folder(folder)));

        // when
        DocumentSearchResponse response =
            this.searchApi.documentSearch(dsq, siteId, null, null, null);

        // then
        List<SearchResultDocument> documents = response.getDocuments();
        assertEquals(1, documents.size());
        assertNotNull(documents.get(0).getInsertedDate());
        assertNotNull(documents.get(0).getLastModifiedDate());

        if (folder.isEmpty()) {
          assertEquals("something", documents.get(0).getPath());
          assertEquals("true", documents.get(0).getFolder().toString());
          assertNotNull(documents.get(0).getDocumentId());
        } else {
          assertEquals("something/path.txt", documents.get(0).getPath());
          assertNull(documents.get(0).getFolder());
          assertNotNull(documents.get(0).getDocumentId());
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
  public void testHandleSearchRequest14() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      for (String path : Arrays.asList("a/b/test.txt", "a/c/test.txt", "a/test.txt")) {
        AddDocumentUploadRequest uploadReq = new AddDocumentUploadRequest().path(path);
        this.documentsApi.addDocumentUpload(uploadReq, siteId, null, null, null);
      }

      DocumentSearchRequest dsq =
          new DocumentSearchRequest().query(new DocumentSearch().meta(new DocumentSearchMeta()
              .indexType(IndexTypeEnum.FOLDER).eq("a").indexFilterBeginsWith("ff#")));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(2, documents.size());
      assertNotNull(documents.get(0).getInsertedDate());
      assertNotNull(documents.get(0).getLastModifiedDate());

      assertEquals("b", documents.get(0).getPath());
      assertEquals("true", documents.get(0).getFolder().toString());
      assertNotNull(documents.get(0).getDocumentId());

      assertEquals("c", documents.get(1).getPath());
      assertEquals("true", documents.get(1).getFolder().toString());
      assertNotNull(documents.get(1).getDocumentId());

      // given
      dsq = new DocumentSearchRequest().query(new DocumentSearch().meta(new DocumentSearchMeta()
          .indexType(IndexTypeEnum.FOLDER).eq("a/").indexFilterBeginsWith("fi#")));

      // when
      response = this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      documents = response.getDocuments();
      assertEquals(1, documents.size());

      assertEquals("a/test.txt", documents.get(0).getPath());
      assertNull(documents.get(0).getFolder());
      assertNotNull(documents.get(0).getDocumentId());
    }
  }

  /**
   * Text Fulltext search.
   *
   * @throws Exception an error has occurred
   */
  @Test
  @Timeout(value = TEST_TIMEOUT * 2)
  public void testHandleSearchRequest15() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      final String text = "My Document.docx";
      final String path = "something/My Document.docx";

      String documentId = saveDocument(siteId, path);
      GetDocumentSyncResponse syncResponse =
          this.documentsApi.getDocumentSyncs(documentId, siteId, null, null);
      assertEquals(1, syncResponse.getSyncs().size());
      assertEquals(DocumentSyncStatus.COMPLETE, syncResponse.getSyncs().get(0).getStatus());

      AdvancedDocumentSearchApi api = new AdvancedDocumentSearchApi(client);
      assertEquals("something/My Document.docx",
          api.getDocumentFulltext(documentId, siteId, null).getPath());

      DocumentSearchRequest dsq =
          new DocumentSearchRequest().query(new DocumentSearch().text(text));

      List<SearchResultDocument> documents = null;

      while (documents == null) {
        // when
        DocumentSearchResponse response =
            this.searchApi.documentSearch(dsq, siteId, null, null, null);

        // then
        documents = response.getDocuments();
        if (documents.isEmpty()) {
          documents = null;
          TimeUnit.SECONDS.sleep(1);
        }
      }

      assertEquals(1, documents.size());
      assertEquals(documentId, documents.get(0).getDocumentId());
      assertEquals(path, documents.get(0).getPath());
    }
  }

  /**
   * Text Fulltext search no data.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest16() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String text = UUID.randomUUID().toString();

      DocumentSearchRequest dsq =
          new DocumentSearchRequest().query(new DocumentSearch().text(text));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      assertEquals(0, response.getDocuments().size());
    }
  }

  /**
   * /search meta path.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest17() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      String path = "something/path.txt";

      final String documentId = saveDocument(siteId, path);

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().meta(new DocumentSearchMeta().path(path)));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(1, documents.size());
      assertNotNull(documents.get(0).getInsertedDate());
      assertNotNull(documents.get(0).getLastModifiedDate());
      assertEquals(documentId, documents.get(0).getDocumentId());
    }
  }

  /**
   * Valid POST search by range query.
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest18() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      final String documentId0 = addDocument(siteId, "date", "2024-03-10", null);
      final String documentId1 = addDocument(siteId, "date", "2024-03-12", null);
      addDocument(siteId, "date", "2024-03-14", null);

      DocumentSearchRequest dsq =
          new DocumentSearchRequest().query(new DocumentSearch().tag(new DocumentSearchTag()
              .key("date").range(new DocumentSearchRange().start("2024-03-10").end("2024-03-12"))));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(2, documents.size());
      assertEquals(documentId0, documents.get(0).getDocumentId());
      assertEquals(documentId1, documents.get(1).getDocumentId());
    }
  }

  /**
   * Invalid POST search by range query.
   *
   */
  @Test
  public void testHandleSearchRequest19() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchRequest dsq =
          new DocumentSearchRequest().query(new DocumentSearch().tag(new DocumentSearchTag()
              .key("date").range(new DocumentSearchRange().start("2024-03-10"))));

      // when
      try {
        this.searchApi.documentSearch(dsq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"range/end\",\"error\":\"range end is required\"}]}",
            e.getResponseBody());
      }
    }
  }

  /**
   * Invalid POST search by beginsWith / range query.
   *
   */
  @Test
  public void testHandleSearchRequest20() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      DocumentSearchTags range = new DocumentSearchTags().key("date")
          .range(new DocumentSearchRange().start("2024-03-10").end("2024-03-20"));

      DocumentSearchRequest dsq = new DocumentSearchRequest()
          .query(new DocumentSearch().addTagsItem(range).addTagsItem(range));

      // when
      try {
        this.searchApi.documentSearch(dsq, siteId, null, null, null);
        fail();
      } catch (ApiException e) {
        // then
        assertEquals(SC_BAD_REQUEST.getStatusCode(), e.getCode());
        assertEquals("{\"errors\":[{\"key\":\"tag/eq\",\"error\":\"'beginsWith','range' "
            + "is only supported on the last tag\"}]}", e.getResponseBody());
      }
    }
  }

  /**
   * Post search by attribute "eq".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest21() throws Exception {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      setBearerToken(siteId);

      addAttribute(siteId, "category");

      String documentId0 = addDocumentWithAttributes(siteId, "category", "person", null);
      addDocumentWithAttributes(siteId, "category", "other", null);

      DocumentSearchRequest dsq = new DocumentSearchRequest().query(new DocumentSearch()
          .attribute(new DocumentSearchAttribute().key("category").eq("person")));

      // when
      DocumentSearchResponse response =
          this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      List<SearchResultDocument> documents = response.getDocuments();
      assertEquals(1, documents.size());
      assertEquals(documentId0, documents.get(0).getDocumentId());

      // given
      dsq = new DocumentSearchRequest().query(new DocumentSearch()
          .addAttributesItem(new DocumentSearchAttribute().key("category").eq("person")));

      // when
      response = this.searchApi.documentSearch(dsq, siteId, null, null, null);

      // then
      documents = response.getDocuments();
      assertEquals(1, documents.size());
      assertEquals(documentId0, documents.get(0).getDocumentId());
    }
  }
}
