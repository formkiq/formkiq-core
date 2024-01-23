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
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

/**
 * Unit Tests for {@link DocumentServiceImpl}.
 */
@ExtendWith(DynamoDbExtension.class)
public class DocumentServiceImplTest implements DbKeys {

  /** {@link DocumentSearchService}. */
  private static DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor folderIndexProcessor;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeAll() throws Exception {

    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());
    searchService =
        new DocumentSearchServiceImpl(dynamoDbConnection, service, DOCUMENTS_TABLE, null);
    folderIndexProcessor = new FolderIndexProcessorImpl(dynamoDbConnection, DOCUMENTS_TABLE);
  }

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {
    this.df.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Create Document.
   *
   * @param uuid {@link String}
   * @param date {@link ZonedDateTime}
   * @param contentType {@link String}
   * @param path {@link String}
   * @return {@link DocumentItem}
   */
  private DocumentItem createDocument(final String uuid, final ZonedDateTime date,
      final String contentType, final String path) {

    String userId = "jsmith";

    DocumentItem item = new DocumentItemDynamoDb(uuid, Date.from(date.toInstant()), userId);
    item.setContentType(contentType);
    item.setPath(path);
    item.setUserId(UUID.randomUUID().toString());
    item.setChecksum(UUID.randomUUID().toString());
    item.setContentLength(Long.valueOf(2));
    return item;
  }

  /**
   * Create {@link DynamicDocumentItem} with Child Documents.
   * 
   * @param now {@link Date}
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createSubDocuments(final Date now) {
    String username = UUID.randomUUID() + "@formkiq.com";

    DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc.setContentType("text/plain");

    DynamicDocumentItem doc1 = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc1.setContentType("text/html");
    doc1.put("tags", Arrays.asList(Map.of("documentId", doc1.getDocumentId(), "key", "category1",
        "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    DynamicDocumentItem doc2 = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
    doc2.setContentType("application/json");
    doc2.put("tags", Arrays.asList(Map.of("documentId", doc2.getDocumentId(), "key", "category2",
        "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

    doc.put("documents", Arrays.asList(doc1, doc2));

    return doc;
  }

  /**
   * Create Test {@link DocumentItem}.
   *
   * @param siteId DynamoDB PK Prefix
   * @return {@link List} {@link DocumentItem}
   */
  private List<DocumentItem> createTestData(final String siteId) {

    List<String> dates = Arrays.asList("2020-01-30T00:00:00", "2020-01-30T01:20:00",
        "2020-01-30T02:20:00", "2020-01-30T05:20:00", "2020-01-30T11:45:00", "2020-01-30T13:22:00",
        "2020-01-30T17:02:00", "2020-01-30T19:10:00", "2020-01-30T20:54:00", "2020-01-30T23:59:59",
        "2020-01-31T00:00:00", "2020-01-31T03:00:00", "2020-01-31T05:00:00", "2020-01-31T07:00:00",
        "2020-01-31T08:00:00", "2020-01-31T09:00:00", "2020-01-31T10:00:00", "2020-01-31T11:00:00",
        "2020-01-31T23:00:00");

    List<DocumentItem> items = new ArrayList<>();

    dates.forEach(date -> {
      ZonedDateTime zdate = DateUtil.toDateTimeFromString(date, null);
      items.add(createDocument(UUID.randomUUID().toString(), zdate, "text/plain", "test.txt"));
    });

    items.forEach(item -> {
      Collection<DocumentTag> tags = Arrays.asList(
          new DocumentTag(item.getDocumentId(), "status", "active", new Date(), "testuser"));
      service.saveDocument(siteId, item, tags);
    });

    return items;
  }

  /** Add Tag Name with TAG DELIMINATOR. */
  @Test
  public void testAddTags01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem document = createTestData(siteId).get(0);
      String documentId = document.getDocumentId();
      String tagKey = "tag" + TAG_DELIMINATOR;
      String tagValue = UUID.randomUUID().toString();
      String userId = "jsmith";

      List<DocumentTag> tags = Arrays.asList(
          new DocumentTag(documentId, tagKey, tagValue, document.getInsertedDate(), userId));

      // when
      service.addTags(siteId, documentId, tags, null);

      // then
      PaginationResults<DocumentTag> results =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertNull(results.getToken());
      assertEquals(2, results.getResults().size());
      assertEquals("status", results.getResults().get(0).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());
      assertEquals("active", results.getResults().get(0).getValue());

      assertEquals(tagKey, results.getResults().get(1).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(1).getType());
      assertEquals(tagValue, results.getResults().get(1).getValue());

      assertEquals(tagValue, service.findDocumentTag(siteId, documentId, tagKey).getValue());

      SearchTagCriteria s = new SearchTagCriteria(tagKey);
      SearchQuery q = new SearchQuery().tag(s);

      PaginationResults<DynamicDocumentItem> list =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertNull(list.getToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().get(0).getDocumentId());
      assertEquals(tagKey, list.getResults().get(0).getMap("matchedTag").get("key"));
      assertEquals(tagValue, list.getResults().get(0).getMap("matchedTag").get("value"));
    }
  }

  /** Add Tag Name only. */
  @Test
  public void testAddTags02() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "tag";
      String tagValue = null;
      Date now = new Date();
      String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), now, userId);
      final String documentId = item.getDocumentId();

      DocumentTag ti = new DocumentTag(documentId, tagKey, tagValue, now, userId);

      List<DocumentTag> tags = Arrays.asList(ti);

      // when
      service.saveDocument(siteId, item, tags);

      // then
      PaginationResults<DocumentTag> results =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertNull(results.getToken());
      assertEquals(1, results.getResults().size());
      assertEquals(tagKey, results.getResults().get(0).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());
      assertEquals("", results.getResults().get(0).getValue());

      assertEquals("", service.findDocumentTag(siteId, documentId, tagKey).getValue());

      SearchTagCriteria s = new SearchTagCriteria(tagKey);
      SearchQuery q = new SearchQuery().tag(s);

      PaginationResults<DynamicDocumentItem> list =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertNull(list.getToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().get(0).getDocumentId());
      assertEquals("tag", list.getResults().get(0).getMap("matchedTag").get("key"));
      assertEquals("", list.getResults().get(0).getMap("matchedTag").get("value"));
    }
  }

  /**
   * Test trying to save a "system tag".
   */
  @Test
  public void testAddTags03() {
    // given
    String siteId = null;
    String documentId = UUID.randomUUID().toString();
    DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
    List<DocumentTag> tags = SYSTEM_DEFINED_TAGS.stream()
        .map(tag -> new DocumentTag(documentId, tag, "A", new Date(), "joe"))
        .collect(Collectors.toList());

    // when
    service.saveDocument(siteId, item, tags);

    // then
    assertEquals(0,
        service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().size());
  }

  /** Add Tag with Values. */
  @Test
  public void testAddTags04() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem document = createTestData(siteId).get(0);
      String documentId = document.getDocumentId();
      String tagKey = "category";
      List<String> tagValues = Arrays.asList("ABC", "XYZ");
      String userId = "jsmith";

      List<DocumentTag> tags = Arrays.asList(new DocumentTag(documentId, tagKey, tagValues,
          document.getInsertedDate(), userId, DocumentTagType.USERDEFINED));

      // when
      service.addTags(siteId, documentId, tags, null);

      // then
      final int count = 2;
      PaginationResults<DocumentTag> results =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertNull(results.getToken());
      assertEquals(count, results.getResults().size());

      assertEquals(tagKey, results.getResults().get(0).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());
      assertNull(results.getResults().get(0).getValue());
      assertEquals(tagValues, results.getResults().get(0).getValues());

      assertEquals("status", results.getResults().get(1).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(1).getType());
      assertEquals("active", results.getResults().get(1).getValue());
      assertNull(results.getResults().get(1).getValues());

      assertEquals(tagValues, service.findDocumentTag(siteId, documentId, tagKey).getValues());

      SearchTagCriteria s = new SearchTagCriteria(tagKey);
      SearchQuery q = new SearchQuery().tag(s);

      PaginationResults<DynamicDocumentItem> list =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertNull(list.getToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().get(0).getDocumentId());
      assertEquals(tagKey, list.getResults().get(0).getMap("matchedTag").get("key"));
      assertNull(list.getResults().get(0).getMap("matchedTag").get("value"));
      assertEquals(Arrays.asList("XYZ", "ABC"),
          list.getResults().get(0).getMap("matchedTag").get("values"));
    }
  }

  /**
   * Add a tag to a lot of documents.
   */
  @Test
  void testAddTags05() {
    // given
    final int count = 200;
    final String tagKey = "category123";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      Map<String, Collection<DocumentTag>> tagMap = new HashMap<>();

      for (int i = 0; i < count; i++) {
        String documentId = UUID.randomUUID().toString();
        List<DocumentTag> tags = Arrays.asList(new DocumentTag(documentId, tagKey, "person",
            new Date(), "joe", DocumentTagType.USERDEFINED));

        tagMap.put(documentId, tags);
      }

      // when
      service.addTags(siteId, tagMap, null);

      // then
      for (String documentId : tagMap.keySet()) {
        PaginationResults<DocumentTag> results =
            service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
        assertEquals(1, results.getResults().size());
        assertEquals(tagKey, results.getResults().get(0).getKey());
        assertEquals("person", results.getResults().get(0).getValue());
      }

      SearchQuery query = new SearchQuery().meta(new SearchMetaCriteria().indexType("tags"));
      PaginationResults<DynamicDocumentItem> results =
          searchService.search(siteId, query, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      assertEquals(tagKey, results.getResults().get(0).get("value"));
    }
  }

  /**
   * Test add Tag with Values then change to Value.
   */
  @Test
  void testAddTags06() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();

      List<DocumentTag> tags0 = Arrays.asList(new DocumentTag(null, "category",
          Arrays.asList("person1", "person2"), new Date(), "joe", DocumentTagType.USERDEFINED));

      List<DocumentTag> tags1 = Arrays.asList(new DocumentTag(null, "category", "person0",
          new Date(), "joe", DocumentTagType.USERDEFINED));

      // when
      service.addTags(siteId, documentId, tags0, null);
      service.addTags(siteId, documentId, tags1, null);

      // then
      PaginationResults<DocumentTag> results =
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      assertEquals("category", results.getResults().get(0).getKey());
      assertEquals("person0", results.getResults().get(0).getValue());
    }
  }

  /** Delete Document. */
  @Test
  public void testDeleteDocument01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      for (boolean softDelete : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

        DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), now, userId);
        item.setPath("a/test.txt");
        String documentId = item.getDocumentId();

        DocumentTag tag = new DocumentTag(null, "status", "active", now, userId);
        tag.setUserId(UUID.randomUUID().toString());

        service.saveDocument(siteId, item, Arrays.asList(tag));

        PaginationResults<DocumentTag> results =
            service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
        assertEquals(1, results.getResults().size());

        // when
        service.deleteDocument(siteId, documentId, softDelete);

        // then
        assertNull(service.findDocument(siteId, documentId));

        results = service.findDocumentTags(siteId, documentId, null, MAX_RESULTS);
        assertEquals(0, results.getResults().size());

        SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));
        PaginationResults<DynamicDocumentItem> folders =
            searchService.search(siteId, q, null, MAX_RESULTS);
        assertEquals(1, folders.getResults().size());

        q = new SearchQuery().meta(new SearchMetaCriteria().folder("a"));
        folders = searchService.search(siteId, q, null, MAX_RESULTS);

        assertEquals(0, folders.getResults().size());
      }
    }
  }

  /**
   * Delete / restore Document.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testDeleteDocument02() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int tagCount = 200;
      Date now = new Date();
      String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), now, userId);
      item.setPath("a/test.txt");
      String documentId = item.getDocumentId();

      List<DocumentTag> tags = new ArrayList<>();

      for (int i = 0; i < tagCount; i++) {
        DocumentTag tag = new DocumentTag(null, "status_" + i, "active", now, userId);
        tag.setUserId(UUID.randomUUID().toString());
        tags.add(tag);
      }

      service.saveDocument(siteId, item, tags);
      assertNotNull(service.findDocument(siteId, documentId));
      assertFalse(
          service.findDocumentTags(siteId, documentId, null, tagCount).getResults().isEmpty());

      boolean softDelete = true;

      // when
      assertTrue(service.deleteDocument(siteId, documentId, softDelete));

      // then
      assertNull(service.findDocument(siteId, documentId));
      assertTrue(
          service.findDocumentTags(siteId, documentId, null, tagCount).getResults().isEmpty());

      List<DocumentItem> results =
          service.findSoftDeletedDocuments(siteId, null, tagCount).getResults();
      assertEquals(documentId, results.get(0).getDocumentId());

      // when
      assertTrue(service.restoreSoftDeletedDocument(siteId, documentId));

      // then
      results = service.findSoftDeletedDocuments(siteId, null, tagCount).getResults();
      assertEquals(0, results.size());

      assertNotNull(service.findDocument(siteId, documentId));

      Map<String, String> map = folderIndexProcessor.getIndex(siteId, item.getPath());
      assertEquals("test.txt", map.get("path"));

      assertEquals(tagCount,
          service.findDocumentTags(siteId, documentId, null, tagCount).getResults().size());
    }
  }

  /**
   * Delete Soft then Delete Document.
   * 
   * @throws IOException IOException
   */
  @Test
  public void testDeleteDocument03() throws IOException {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String userId = "jsmith";

      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), now, userId);
      item.setPath("a/test52.txt");
      String documentId = item.getDocumentId();

      DocumentTag tag = new DocumentTag(null, "status", "active", now, userId);
      tag.setUserId(UUID.randomUUID().toString());

      service.saveDocument(siteId, item, Arrays.asList(tag));

      assertNotNull(service.findDocument(siteId, documentId));
      assertEquals(1,
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().size());

      boolean softDelete = true;

      // when
      assertTrue(service.deleteDocument(siteId, documentId, softDelete));

      // then
      assertNull(service.findDocument(siteId, documentId));
      assertEquals(0,
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().size());

      List<DocumentItem> results =
          service.findSoftDeletedDocuments(siteId, null, MAX_RESULTS).getResults();
      assertEquals(documentId, results.get(0).getDocumentId());

      // given
      softDelete = false;

      // when
      assertTrue(service.deleteDocument(siteId, documentId, softDelete));

      // then
      results = service.findSoftDeletedDocuments(siteId, null, MAX_RESULTS).getResults();
      assertEquals(0, results.size());

      assertNull(service.findDocument(siteId, documentId));
      assertEquals(0,
          service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test document exists or not.
   */
  @Test
  public void testExists01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId0 = UUID.randomUUID().toString();
      String documentId1 = UUID.randomUUID().toString();
      DocumentItem item0 =
          createDocument(documentId0, ZonedDateTime.now(), "text/plain", "test.txt");

      // when
      service.saveDocument(siteId, item0, null);

      // then
      assertTrue(service.exists(siteId, documentId0));
      assertFalse(service.exists(siteId, documentId1));
    }
  }

  /** Find valid document. */
  @Test
  public void testFindDocument01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem document = createTestData(siteId).get(0);
      String documentId = document.getDocumentId();

      // when
      DocumentItem item = service.findDocument(siteId, documentId);

      // then
      assertEquals(documentId, item.getDocumentId());
      assertNotNull(item.getInsertedDate());
      assertNotNull(item.getLastModifiedDate());
      assertEquals(document.getInsertedDate(), item.getInsertedDate());
      assertEquals(document.getInsertedDate(), item.getLastModifiedDate());
      assertNotNull(item.getPath());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getChecksum());
      assertNotNull(item.getUserId());
      assertNotNull(item.getContentLength());
    }
  }

  /**
   * Test FindDocument with child documents pagination.
   */
  @Test
  public void testFindDocument02() {
    Date now = new Date();
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final Collection<String> list = new HashSet<>();
      DynamicDocumentItem doc = createSubDocuments(now);
      service.saveDocumentItemWithTag(siteId, doc);

      // when
      PaginationResult<DocumentItem> result =
          service.findDocument(siteId, doc.getDocumentId(), true, null, 1);

      // then
      assertEquals(doc.getDocumentId(), result.getResult().getDocumentId());
      List<DocumentItem> documents = result.getResult().getDocuments();
      assertEquals(1, documents.size());

      list.add(documents.get(0).getDocumentId());
      assertNotNull(result.getToken());

      // when
      result = service.findDocument(siteId, doc.getDocumentId(), true, result.getToken(), 1);

      // then
      assertEquals(doc.getDocumentId(), result.getResult().getDocumentId());
      documents = result.getResult().getDocuments();

      list.add(documents.get(0).getDocumentId());
      assertEquals(1, documents.size());
      assertNotNull(result.getToken());

      // when
      result = service.findDocument(siteId, doc.getDocumentId(), true, result.getToken(), 1);

      // then
      assertEquals(doc.getDocumentId(), result.getResult().getDocumentId());
      documents = result.getResult().getDocuments();
      assertTrue(documents.isEmpty());
      assertNull(result.getToken());

      assertEquals(2, list.size());
    }
  }

  /** Find documents. */
  @Test
  public void testFindDocuments01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Iterator<DocumentItem> itr = createTestData(siteId).iterator();
      DocumentItem d0 = itr.next();
      DocumentItem d1 = itr.next();
      DocumentItem d2 = itr.next();

      createTestData("finance");

      List<String> documentIds =
          Arrays.asList(d0.getDocumentId(), d1.getDocumentId(), d2.getDocumentId());

      // when
      List<DocumentItem> items = service.findDocuments(siteId, documentIds);

      // then
      int i = 0;
      assertEquals(items.size(), documentIds.size());
      assertEquals(d0.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertNotNull(items.get(i).getLastModifiedDate());
      assertEquals(d0.getInsertedDate(), items.get(i++).getInsertedDate());

      assertEquals(d1.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertNotNull(items.get(i).getLastModifiedDate());
      assertEquals(d1.getInsertedDate(), items.get(i++).getInsertedDate());

      assertEquals(d2.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertNotNull(items.get(i).getLastModifiedDate());
      assertEquals(d2.getInsertedDate(), items.get(i++).getInsertedDate());
    }
  }

  /** Find all documents. */
  @Test
  public void testFindDocuments02() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData("finance");
      List<String> documentIds =
          createTestData(siteId).stream().map(k -> k.getDocumentId()).collect(Collectors.toList());

      // when
      List<DocumentItem> items = service.findDocuments(siteId, documentIds);

      // then
      assertEquals(items.size(), documentIds.size());
      assertNotNull(items.get(0).getDocumentId());
      assertNotNull(items.get(0).getInsertedDate());
      assertNotNull(items.get(0).getLastModifiedDate());
    }
  }

  /**
   * Test finding 10 documents all created in one day.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(siteId);
      createTestData("finance");

      List<String> expected = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]", "2020-01-30T05:20Z[UTC]", "2020-01-30T11:45Z[UTC]",
          "2020-01-30T13:22Z[UTC]", "2020-01-30T17:02Z[UTC]", "2020-01-30T19:10Z[UTC]",
          "2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(MAX_RESULTS, results.getResults().size());
      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());

      assertArrayEquals(expected.toArray(new String[0]), resultDates.toArray(new String[0]));
      assertNull(results.getToken());
    }
  }

  /**
   * Test paging through results.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(siteId);

      final int max = 3;
      List<String> expected0 = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          service.findDocumentsByDate(siteId, date, null, max);

      // then
      assertEquals(max, results.getResults().size());

      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());

      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      String documentId = results.getResults().get(results.getResults().size() - 1).getDocumentId();
      if (siteId == null) {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=docts#2020-01-30, GSI1SK=2020-01-30T02:20:00+0000#" + documentId
                + ", SK=document, PK="));
      } else {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=" + siteId + "/docts#2020-01-30, GSI1SK=2020-01-30T02:20:00+0000#"
                + documentId + ", SK=document, PK="));
      }

      // given
      final List<String> expected1 = Arrays.asList("2020-01-30T05:20Z[UTC]",
          "2020-01-30T11:45Z[UTC]", "2020-01-30T13:22Z[UTC]");

      // when - get next page
      results = service.findDocumentsByDate(siteId, date, results.getToken(), max);

      // then
      assertEquals(max, results.getResults().size());
      assertNotNull(results.getToken());
      resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());

      assertArrayEquals(expected1.toArray(new String[0]), resultDates.toArray(new String[0]));
    }
  }

  /**
   * Test paging through results over multiple days from a different TZ.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(siteId);

      List<String> expected0 = Arrays.asList("2020-01-30T19:10Z[UTC]", "2020-01-30T20:54Z[UTC]",
          "2020-01-30T23:59:59Z[UTC]", "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]",
          "2020-01-31T05:00Z[UTC]", "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]",
          "2020-01-31T09:00Z[UTC]", "2020-01-31T10:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T12:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(expected0.size(), results.getResults().size());

      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());
      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      String documentId = results.getResults().get(results.getResults().size() - 1).getDocumentId();

      if (siteId == null) {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=docts#2020-01-31, GSI1SK=2020-01-31T10:00:00+0000#" + documentId
                + ", SK=document, PK="));
      } else {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=" + siteId + "/docts#2020-01-31, GSI1SK=2020-01-31T10:00:00+0000#"
                + documentId + ", SK=document, PK="));
      }

      // given
      List<String> expected1 = Arrays.asList("2020-01-31T11:00Z[UTC]");

      // when
      results = service.findDocumentsByDate(siteId, date, results.getToken(), MAX_RESULTS);

      // then
      assertEquals(expected1.size(), results.getResults().size());
      resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());

      assertArrayEquals(expected1.toArray(new String[0]), resultDates.toArray(new String[0]));
      assertNull(results.getToken());
    }
  }

  /**
   * Test fetching 10 results over 2 days from a different TZ.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(siteId);

      List<String> expected0 = Arrays.asList("2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]",
          "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]", "2020-01-31T05:00Z[UTC]",
          "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]", "2020-01-31T09:00Z[UTC]",
          "2020-01-31T10:00Z[UTC]", "2020-01-31T11:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T14:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

      // then
      assertEquals(expected0.size(), results.getResults().size());

      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());
      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      assertNull(results.getToken());
    }
  }

  /**
   * Find Documents by date with document that has child documents.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate05() throws Exception {
    // given
    Date now = new Date();
    String siteId = null;
    DynamicDocumentItem doc = createSubDocuments(now);
    service.saveDocumentItemWithTag(siteId, doc);
    ZonedDateTime date = service.findMostDocumentDate();
    assertNotNull(date);

    // when
    PaginationResults<DocumentItem> results =
        service.findDocumentsByDate(siteId, date, null, MAX_RESULTS);

    // then
    assertEquals(1, results.getResults().size());
    assertNull(results.getResults().get(0).getBelongsToDocumentId());
  }

  /**
   * Find Documents Tags.
   */
  @Test
  public void testFindDocumentsTags01() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();
      DocumentItem document = new DocumentItemDynamoDb(documentId, now, userId);
      String tagKey0 = "category";
      String tagValue0 = "person";
      String tagKey1 = "playerId";
      List<String> tagValue1 = Arrays.asList("111", "222");
      List<DocumentTag> tags = Arrays.asList(
          new DocumentTag(documentId, tagKey0, tagValue0, now, userId), new DocumentTag(documentId,
              tagKey1, tagValue1, now, userId, DocumentTagType.USERDEFINED));

      service.saveDocument(siteId, document, tags);

      // when
      final Map<String, Collection<DocumentTag>> tagMap0 = service.findDocumentsTags(siteId,
          Arrays.asList(documentId), Arrays.asList(tagKey0, tagKey1));
      final Map<String, Collection<DocumentTag>> tagMap1 =
          service.findDocumentsTags(siteId, Arrays.asList(documentId), Arrays.asList(tagKey0));
      final Map<String, Collection<DocumentTag>> tagMap2 =
          service.findDocumentsTags(siteId, Arrays.asList(documentId), Arrays.asList(tagKey1));

      // then
      assertEquals(1, tagMap0.size());
      Collection<DocumentTag> tags0 = tagMap0.get(documentId);
      assertEquals(2, tags0.size());

      Collection<DocumentTag> tags1 = tagMap1.get(documentId);
      assertEquals(1, tags1.size());
      DocumentTag next = tags1.iterator().next();
      assertEquals(documentId, next.getDocumentId());
      assertEquals(tagKey0, next.getKey());
      assertEquals(tagValue0, next.getValue());
      assertNull(next.getValues());

      Collection<DocumentTag> tags2 = tagMap2.get(documentId);
      assertEquals(1, tags2.size());
      next = tags2.iterator().next();
      assertEquals(documentId, next.getDocumentId());
      assertEquals(tagKey1, next.getKey());
      assertNull(next.getValue());
      assertEquals("[111, 222]", next.getValues().toString());
    }
  }

  /**
   * Find Documents more than 100 combination of Tags.
   */
  @Test
  public void testFindDocumentsTags02() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int count = 1000;
      Date now = new Date();
      String userId = "jsmith";
      String documentId = UUID.randomUUID().toString();
      DocumentItem document = new DocumentItemDynamoDb(documentId, now, userId);

      String tagKey0 = "category";
      String tagValue0 = "person";
      String tagKey1 = "playerId";

      List<String> tagValue1 = Arrays.asList("111", "222");
      List<DocumentTag> tags = Arrays.asList(
          new DocumentTag(documentId, tagKey0, tagValue0, now, userId), new DocumentTag(documentId,
              tagKey1, tagValue1, now, userId, DocumentTagType.USERDEFINED));

      service.saveDocument(siteId, document, tags);

      List<String> documentIds = new ArrayList<>();
      documentIds.add(documentId);
      for (int i = 0; i < count; i++) {
        documentIds.add(UUID.randomUUID().toString());
      }

      // when
      Map<String, Collection<DocumentTag>> tagMap =
          service.findDocumentsTags(siteId, documentIds, Arrays.asList(tagKey0, tagKey1));

      // then
      assertEquals(count + 1, tagMap.size());
      Collection<DocumentTag> tags0 = tagMap.get(documentId);
      assertEquals(2, tags0.size());
    }
  }

  /** Test Finding Document's Tag && Remove one. */
  @Test
  public void testFindDocumentTags01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      PaginationMapToken startkey = null;
      createTestData("finance");
      String documentId = createTestData(siteId).get(0).getDocumentId();

      // when
      PaginationResults<DocumentTag> results =
          service.findDocumentTags(siteId, documentId, startkey, MAX_RESULTS);

      // then
      assertNull(results.getToken());
      assertEquals(1, results.getResults().size());

      assertEquals("status", results.getResults().get(0).getKey());
      assertEquals("active", results.getResults().get(0).getValue());
      assertNotNull(results.getResults().get(0).getInsertedDate());
      assertNotNull(results.getResults().get(0).getUserId());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());

      // given
      List<String> tags = Arrays.asList("status");

      // when
      service.removeTags(siteId, documentId, tags);

      // then
      results = service.findDocumentTags(siteId, documentId, startkey, MAX_RESULTS);

      assertNull(results.getToken());
      assertEquals(0, results.getResults().size());
    }
  }

  /** Test Finding Document's particular Tag. */
  @Test
  public void testFindDocumentTags02() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "status";
      String tagValue = "active";
      String documentId = createTestData(siteId).get(0).getDocumentId();

      // when
      String result = service.findDocumentTag(siteId, documentId, tagKey).getValue();

      // then
      assertEquals(tagValue, result);
    }
  }

  /** Test Finding Document's Tag does not exist. */
  @Test
  public void testFindDocumentTags03() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "status";
      String documentId = createTestData(siteId).get(0).getDocumentId();

      // when
      DocumentTag result = service.findDocumentTag(siteId, documentId, tagKey + "!");

      // then
      assertNull(result);
    }
  }

  /**
   * Test finding most recent documents date.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindMostDocumentDate01() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(siteId);
      createTestData("finance");

      // when
      ZonedDateTime date = service.findMostDocumentDate();

      // then
      final int year = 2020;
      final int day = 31;
      assertEquals(year, date.getYear());
      assertEquals(Month.JANUARY, date.getMonth());
      assertEquals(day, date.getDayOfMonth());
      assertEquals(ZoneOffset.UTC, date.getZone());
    }
  }

  /**
   * Test finding most recent documents date (no data).
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindMostDocumentDate02() throws Exception {
    // given
    // when
    ZonedDateTime date = service.findMostDocumentDate();

    // then
    assertNull(date);
  }

  /**
   * Find / Save Presets.
   * 
   * @deprecated method needs to be updated
   */
  @Test
  @Deprecated
  public void testFindPresets01() {

    String type = "tagging";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      List<String> ids = new ArrayList<>();

      try {
        // given
        final int count = 19;
        Preset preset = new Preset();

        for (int i = 0; i < count; i++) {

          String id = UUID.randomUUID().toString();
          ids.add(id);

          preset = new Preset();
          preset.setId(id);
          preset.setName(UUID.randomUUID().toString());
          preset.setType(type);
          preset.setInsertedDate(new Date());
          preset.setUserId("joe");

          service.savePreset(siteId, id, type, preset, null);
        }

        // when
        PaginationResults<Preset> p0 =
            service.findPresets(siteId, null, type, null, null, MAX_RESULTS);

        // then
        assertEquals(MAX_RESULTS, p0.getResults().size());
        assertNotNull(p0.getToken());

        // when
        PaginationResults<Preset> p1 =
            service.findPresets(siteId, preset.getId(), type, preset.getName(), null, MAX_RESULTS);

        // then
        assertEquals(1, p1.getResults().size());
        assertEquals(preset.getName(), p1.getResults().get(0).getName());
        assertEquals(type, p1.getResults().get(0).getType());
        assertNotNull(p1.getResults().get(0).getInsertedDate());
        assertNotNull(p1.getResults().get(0).getId());
        assertNull(p1.getResults().get(0).getUserId());

        // when
        p0 = service.findPresets(siteId, null, type, null, p0.getToken(), MAX_RESULTS);

        // then
        assertEquals(ids.size() - MAX_RESULTS, p0.getResults().size());

        // when
        Optional<Preset> p = service.findPreset(siteId, preset.getId());

        // then
        assertTrue(p.isPresent());
        assertEquals(preset.getId(), p.get().getId());

      } finally {
        ids.forEach(id -> {
          service.deletePreset(siteId, id);
        });

        PaginationResults<Preset> p0 =
            service.findPresets(siteId, null, type, null, null, MAX_RESULTS);
        assertEquals(0, p0.getResults().size());
      }
    }
  }

  /**
   * Test Find Preset Tag.
   * 
   * @deprecated method needs to be updated
   */
  @Test
  @Deprecated
  public void testFindPresetTags01() {
    // given
    String type = "tagging";
    String presetId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      PresetTag tag = new PresetTag();

      try {
        tag.setInsertedDate(new Date());
        tag.setKey(UUID.randomUUID().toString());
        tag.setUserId("joe");

        service.savePreset(siteId, presetId, type, null, Arrays.asList(tag));

        // when
        PaginationResults<PresetTag> results =
            service.findPresetTags(siteId, presetId, null, MAX_RESULTS);
        Optional<PresetTag> ptag = service.findPresetTag(siteId, presetId, tag.getKey());

        // then
        assertEquals(1, results.getResults().size());
        assertTrue(ptag.isPresent());
        assertEquals(tag.getKey(), ptag.get().getKey());
        assertEquals("joe", ptag.get().getUserId());
        assertNotNull(ptag.get().getInsertedDate());

      } finally {
        service.deletePresetTag(siteId, presetId, tag.getKey());
        assertEquals(0,
            service.findPresetTags(siteId, presetId, null, MAX_RESULTS).getResults().size());
      }
    }
  }

  /**
   * Test No extra formats.
   */
  @Test
  public void testGetDocumentFormats01() {
    // given
    String userId = "test";
    Date now = new Date();
    String contentType = "application/pdf";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, now, userId);
      item.setContentType(contentType);
      service.saveDocument(siteId, item, null);

      // when
      Optional<DocumentFormat> format = service.findDocumentFormat(siteId, documentId, contentType);
      PaginationResults<DocumentFormat> formats =
          service.findDocumentFormats(siteId, documentId, null, MAX_RESULTS);

      // then
      assertFalse(format.isPresent());
      assertEquals(0, formats.getResults().size());
    }
  }

  /**
   * Test extra formats.
   */
  @Test
  public void testGetDocumentFormats02() {
    // given
    String userId = "test";
    Date now = new Date();
    String contentType = "application/pdf";
    List<String> contentTypes = Arrays.asList("text/plain", "text/html");

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, now, userId);
      item.setContentType(contentType);
      service.saveDocument(siteId, item, null);

      for (String format : contentTypes) {
        DocumentFormat f = new DocumentFormat();
        f.setContentType(format);
        f.setDocumentId(documentId);
        f.setInsertedDate(now);
        f.setUserId(userId);
        service.saveDocumentFormat(siteId, f);
      }

      // when
      Optional<DocumentFormat> format =
          service.findDocumentFormat(siteId, documentId, contentTypes.get(0));
      PaginationResults<DocumentFormat> formats =
          service.findDocumentFormats(siteId, documentId, null, 1);

      // then
      assertTrue(format.isPresent());
      assertEquals(1, formats.getResults().size());

      assertEquals(contentTypes.get(0), format.get().getContentType());
      assertEquals(documentId, format.get().getDocumentId());
      assertEquals(this.df.format(now), this.df.format(format.get().getInsertedDate()));
      assertEquals(userId, format.get().getUserId());

      assertEquals(contentTypes.get(1), formats.getResults().get(0).getContentType());
      assertEquals(documentId, formats.getResults().get(0).getDocumentId());
      assertEquals(this.df.format(now),
          this.df.format(formats.getResults().get(0).getInsertedDate()));
      assertEquals(userId, formats.getResults().get(0).getUserId());

      // when
      formats = service.findDocumentFormats(siteId, documentId, formats.getToken(), 1);

      // then
      assertEquals(1, formats.getResults().size());
      assertEquals(contentTypes.get(0), formats.getResults().get(0).getContentType());
      assertEquals(documentId, formats.getResults().get(0).getDocumentId());
      assertEquals(this.df.format(now),
          this.df.format(formats.getResults().get(0).getInsertedDate()));
      assertEquals(userId, formats.getResults().get(0).getUserId());
    }
  }

  /**
   * Test Remove 1 tag value from a 2 multi-value Document Tag.
   */
  @Test
  public void testRemoveTag01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "category";
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag = new DocumentTag(docid, tagKey, null, new Date(), "jsmith");
      tag.setValues(Arrays.asList("abc", "xyz"));
      Collection<DocumentTag> tags = Arrays.asList(tag);
      service.saveDocument(siteId, item, tags);

      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertEquals("[abc, xyz]", results.get(0).getValues().toString());
      assertNull(results.get(0).getValue());

      // when
      assertTrue(service.removeTag(siteId, docid, tagKey, "xyz"));

      // then
      results = service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertNull(results.get(0).getValues());
      assertEquals("abc", results.get(0).getValue());
    }
  }

  /**
   * Test Remove 1 tag value from a 3 multi-value Document Tag.
   */
  @Test
  public void testRemoveTag02() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "category";
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag = new DocumentTag(docid, tagKey, null, new Date(), "jsmith");
      tag.setValues(Arrays.asList("abc", "mno", "xyz"));
      Collection<DocumentTag> tags = Arrays.asList(tag);
      service.saveDocument(siteId, item, tags);

      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertEquals("[abc, mno, xyz]", results.get(0).getValues().toString());
      assertNull(results.get(0).getValue());

      // when
      assertTrue(service.removeTag(siteId, docid, tagKey, "xyz"));

      // then
      results = service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertEquals("[abc, mno]", results.get(0).getValues().toString());
      assertNull(results.get(0).getValue());
    }
  }

  /**
   * Test Remove 1 tag value from a 1 multi-value Document Tag.
   */
  @Test
  public void testRemoveTag03() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "category";
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag = new DocumentTag(docid, tagKey, null, new Date(), "jsmith");
      tag.setValues(Arrays.asList("xyz"));
      Collection<DocumentTag> tags = Arrays.asList(tag);
      service.saveDocument(siteId, item, tags);

      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertEquals("[xyz]", results.get(0).getValues().toString());
      assertNull(results.get(0).getValue());

      // when
      assertTrue(service.removeTag(siteId, docid, tagKey, "xyz"));

      // then
      results = service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(0, results.size());
    }
  }

  /**
   * Test Remove tag value.
   */
  @Test
  public void testRemoveTag04() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "category";
      String tagValue = "person";
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag = new DocumentTag(docid, tagKey, tagValue, new Date(), "jsmith");
      Collection<DocumentTag> tags = Arrays.asList(tag);
      service.saveDocument(siteId, item, tags);

      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertNull(results.get(0).getValues());
      assertEquals(tagValue, results.get(0).getValue());

      // when
      assertTrue(service.removeTag(siteId, docid, tagKey, tagValue));

      // then
      results = service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(0, results.size());
    }
  }

  /**
   * Test Remove wrong tag value.
   */
  @Test
  public void testRemoveTag05() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "category";
      String tagValue = "person";
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag = new DocumentTag(docid, tagKey, tagValue, new Date(), "jsmith");
      Collection<DocumentTag> tags = Arrays.asList(tag);
      service.saveDocument(siteId, item, tags);

      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertNull(results.get(0).getValues());
      assertEquals(tagValue, results.get(0).getValue());

      // when
      assertFalse(service.removeTag(siteId, docid, tagKey, tagValue + "!"));

      // then
      results = service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
    }
  }

  /**
   * Test Remove Tags from Document.
   */
  @Test
  public void testRemoveTags01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      Collection<DocumentTag> tags =
          Arrays.asList(new DocumentTag(docid, "untagged", "true", new Date(), "jsmith"));
      service.saveDocument(siteId, item, tags);

      // when
      service.removeTags(siteId, docid, Arrays.asList(tags.iterator().next().getKey()));

      // then
      assertEquals(0,
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test Remove 'VALUES' Tags from Document.
   */
  @Test
  public void testRemoveTags02() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      DocumentTag tag0 = new DocumentTag(docid, "category", null, new Date(), "jsmith");
      tag0.setValues(Arrays.asList("abc", "xyz"));
      DocumentTag tag1 = new DocumentTag(docid, "category2", null, new Date(), "jsmith");
      tag1.setValues(Arrays.asList("abc2", "xyz2"));
      Collection<DocumentTag> tags = Arrays.asList(tag0, tag1);
      service.saveDocument(siteId, item, tags);

      assertEquals(2,
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults().size());

      // when
      service.removeTags(siteId, docid, Arrays.asList(tags.iterator().next().getKey()));

      // then
      List<DocumentTag> results =
          service.findDocumentTags(siteId, docid, null, MAX_RESULTS).getResults();
      assertEquals(1, results.size());
      assertEquals("category2", results.get(0).getKey());
      assertEquals("[abc2, xyz2]", results.get(0).getValues().toString());
    }
  }

  /**
   * Test Save {@link DocumentItem} with {@link DocumentMetadata}.
   */
  @Test
  public void testSaveDocumentItemWithMetadata01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final String content = "This is a test";
      final String username = UUID.randomUUID() + "@formkiq.com";

      DocumentMetadata m0 = new DocumentMetadata();
      m0.setKey("some");
      m0.setValue("thing");

      DocumentMetadata m1 = new DocumentMetadata();
      m1.setKey("playerId");
      m1.setValues(Arrays.asList("111", "222"));

      DynamicDocumentItem doc = new DynamicDocumentItem(
          Map.of("documentId", UUID.randomUUID().toString(), "userId", username, "content",
              Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));
      doc.setMetadata(Arrays.asList(m0, m1));

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);
      List<DocumentMetadata> metadata = new ArrayList<>(item.getMetadata());
      Collections.sort(metadata, new DocumentMetadataComparator());
      assertEquals(2, metadata.size());
      assertEquals("playerId", metadata.get(0).getKey());
      assertEquals("[111, 222]", metadata.get(0).getValues().toString());
      assertEquals("some", metadata.get(1).getKey());
      assertEquals("thing", metadata.get(1).getValue());
    }
  }

  /**
   * Test Save {@link DocumentItem} with {@link DocumentTag}.
   */
  @Test
  public void testSaveDocumentItemWithTag01() {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.getLastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("untagged", tags.getResults().get(0).getKey());
      assertEquals("true", tags.getResults().get(0).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(0).getType());
      assertEquals(username, tags.getResults().get(0).getUserId());
      assertEquals(item.getDocumentId(), tags.getResults().get(0).getDocumentId());
      assertNotNull(tags.getResults().get(0).getInsertedDate());
    }
  }

  /**
   * Test Save {@link DocumentItem} with {@link DocumentTag} with tags.
   */
  @Test
  public void testSaveDocumentItemWithTag02() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));

      for (String tagValue : Arrays.asList("person", "thing")) {

        doc.put("tags",
            Arrays.asList(Map.of("documentId", doc.getDocumentId(), "key", "category", "value",
                tagValue, "insertedDate", new Date(), "userId", username, "type",
                DocumentTagType.USERDEFINED.name())));

        // when
        DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

        // then
        item = service.findDocument(siteId, item.getDocumentId());
        assertNotNull(item);

        PaginationResults<DocumentTag> tags =
            service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
        assertEquals(1, tags.getResults().size());
        assertEquals("category", tags.getResults().get(0).getKey());
        assertEquals(tagValue, tags.getResults().get(0).getValue());
        assertEquals(DocumentTagType.USERDEFINED, tags.getResults().get(0).getType());
        assertEquals(username, tags.getResults().get(0).getUserId());
        assertEquals(item.getDocumentId(), tags.getResults().get(0).getDocumentId());
        assertNotNull(tags.getResults().get(0).getInsertedDate());
      }
    }
  }

  /**
   * Test Save Document with SubDocument.
   */
  @Test
  public void testSaveDocumentItemWithTag03() {
    Date now = new Date();
    ZonedDateTime nowDate = ZonedDateTime.ofInstant(now.toInstant(), ZoneId.systemDefault());

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DynamicDocumentItem doc = createSubDocuments(now);

      // when
      service.saveDocumentItemWithTag(siteId, doc);

      // then
      final DocumentItem doc1 = doc.getDocuments().get(0);
      final DocumentItem doc2 = doc.getDocuments().get(1);

      PaginationResult<DocumentItem> result =
          service.findDocument(siteId, doc.getDocumentId(), true, null, MAX_RESULTS);
      assertNull(result.getToken());

      DocumentItem item = result.getResult();
      assertNotNull(item);
      assertEquals("text/plain", item.getContentType());
      assertEquals(2, item.getDocuments().size());
      List<String> ids = Arrays.asList(doc1.getDocumentId(), doc2.getDocumentId());
      Collections.sort(ids);

      assertEquals(ids.get(0), item.getDocuments().get(0).getDocumentId());
      assertEquals(doc.getDocumentId(), item.getDocuments().get(0).getBelongsToDocumentId());
      assertNotNull("text/html", item.getDocuments().get(0).getContentType());
      assertEquals(ids.get(1), item.getDocuments().get(1).getDocumentId());
      assertEquals(doc.getDocumentId(), item.getDocuments().get(1).getBelongsToDocumentId());
      assertNotNull("application/json", item.getDocuments().get(1).getContentType());

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("untagged", tags.get(0).getKey());
      assertEquals("true", tags.get(0).getValue());

      item = service.findDocument(siteId, doc1.getDocumentId());
      assertNotNull(item);
      assertEquals("text/html", item.getContentType());
      assertEquals(doc.getDocumentId(), item.getBelongsToDocumentId());

      tags = service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("category1", tags.get(0).getKey());

      item = service.findDocument(siteId, doc2.getDocumentId());
      assertNotNull(item);
      assertEquals("application/json", item.getContentType());
      assertEquals(doc.getDocumentId(), item.getBelongsToDocumentId());

      tags = service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("category2", tags.get(0).getKey());

      assertEquals(1,
          service.findDocumentsByDate(siteId, nowDate, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test Save Document with SubDocument and tags.
   */
  @Test
  public void testSaveDocumentItemWithTag04() {
    Date now = new Date();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DynamicDocumentItem doc = createSubDocuments(now);
      doc.put("tags",
          Arrays
              .asList(Map.of("documentId", doc.getDocumentId(), "key", "category2", "insertedDate",
                  now, "userId", doc.getUserId(), "type", DocumentTagType.USERDEFINED.name())));

      // when
      service.saveDocumentItemWithTag(siteId, doc);

      // then
      PaginationResult<DocumentItem> result =
          service.findDocument(siteId, doc.getDocumentId(), true, null, MAX_RESULTS);
      assertNull(result.getToken());

      DocumentItem item = result.getResult();

      List<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("category2", tags.get(0).getKey());
      assertEquals("", tags.get(0).getValue());
    }
  }

  /**
   * Test Save sSubDocument.
   */
  @Test
  public void testSaveDocumentItemWithTag05() {
    Date now = new Date();
    String belongsToDocumentId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
      doc.setBelongsToDocumentId(belongsToDocumentId);
      doc.setContentType("text/plain");

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      PaginationResult<DocumentItem> result =
          service.findDocument(siteId, doc.getDocumentId(), true, null, MAX_RESULTS);
      assertNull(result.getToken());
      item = result.getResult();

      assertNotNull(item);
      assertNotNull(item.getBelongsToDocumentId());
      assertEquals("text/plain", item.getContentType());
    }
  }

  /**
   * Test Save {@link DocumentItem} with {@link DocumentTag} and TTL.
   * 
   * @throws URISyntaxException URISyntaxException
   */
  @Test
  public void testSaveDocumentItemWithTag06() throws URISyntaxException {
    // given
    String ttl = "1612058378";

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc =
          new DynamicDocumentItem(Map.of("documentId", UUID.randomUUID().toString(), "TimeToLive",
              ttl, "userId", username, "insertedDate", new Date(), "content",
              Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, item.getDocumentId()))
          .tableName(DOCUMENTS_TABLE).build();

      try (DynamoDbClient dbClient = DynamoDbTestServices.getDynamoDbConnection().build()) {

        Map<String, AttributeValue> result = dbClient.getItem(r).item();
        assertEquals(ttl, result.get("TimeToLive").n());

        for (String tagKey : Arrays.asList("untagged")) {
          r = GetItemRequest.builder().key(keysDocumentTag(siteId, item.getDocumentId(), tagKey))
              .tableName(DOCUMENTS_TABLE).build();

          result = dbClient.getItem(r).item();
          assertEquals(ttl, result.get("TimeToLive").n());
        }
      }
    }
  }

  /**
   * Test Save lots of tag records and duplicates.
   */
  @Test
  public void testSaveDocumentItemWithTag07() {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      final int numberOfTags = 500;
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));

      List<Map<String, Object>> taglist = new ArrayList<>();
      for (int j = 0; j < numberOfTags; j++) {
        for (int i = 0; i < 2; i++) { // add duplicate tags
          taglist
              .add(Map.of("documentId", doc.getDocumentId(), "key", "category_" + j, "insertedDate",
                  new Date(), "userId", username, "type", DocumentTagType.USERDEFINED.name()));
        }
      }

      doc.put("tags", taglist);

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.getLastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS * MAX_RESULTS);
      assertEquals(MAX_RESULTS * MAX_RESULTS, tags.getResults().size());
    }
  }

  /**
   * Test Save 'path', 'userId' tags.
   */
  @Test
  public void testSaveDocumentItemWithTag08() {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));
      doc.setPath("test.pdf");

      List<Map<String, Object>> taglist = new ArrayList<>();

      taglist.add(Map.of("documentId", doc.getDocumentId(), "key", "path", "value", "test.pdf",
          "insertedDate", new Date(), "userId", username, "type",
          DocumentTagType.USERDEFINED.name()));

      taglist.add(Map.of("documentId", doc.getDocumentId(), "key", "userId", "value", "test",
          "insertedDate", new Date(), "userId", username, "type",
          DocumentTagType.USERDEFINED.name()));

      doc.put("tags", taglist);

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.getLastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
      assertEquals(2, tags.getResults().size());
      assertEquals("path", tags.getResults().get(0).getKey());
      assertEquals("userId", tags.getResults().get(1).getKey());
    }
  }

  /**
   * Test Save {@link DocumentItem} with null {@link DocumentTag}.
   */
  @Test
  public void testSaveDocumentItemWithTag09() {
    final int year = Calendar.getInstance().get(Calendar.YEAR);
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));
      doc.put("tags", Arrays.asList(
          Map.of("documentId", doc.getDocumentId(), "insertedDate", now, "userId", username)));

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);

      ZoneId timeZone = ZoneId.systemDefault();
      LocalDate lastModifiedDate =
          item.getLastModifiedDate().toInstant().atZone(timeZone).toLocalDate();
      assertEquals(year, lastModifiedDate.get(ChronoField.YEAR_OF_ERA));

      PaginationResults<DocumentTag> tags =
          service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
      assertEquals(1, tags.getResults().size());
      assertEquals("untagged", tags.getResults().get(0).getKey());
      assertEquals("true", tags.getResults().get(0).getValue());
      assertEquals(DocumentTagType.SYSTEMDEFINED, tags.getResults().get(0).getType());
      assertEquals(username, tags.getResults().get(0).getUserId());
      assertEquals(item.getDocumentId(), tags.getResults().get(0).getDocumentId());
      assertNotNull(tags.getResults().get(0).getInsertedDate());
    }
  }

  /**
   * Test Save case insensitive tag keys.
   */
  @Test
  public void testSaveDocumentItemWithTag10() {

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";
      String documentId = UUID.randomUUID().toString();

      List<String> tagKeys = Arrays.asList("filetype", "fileType");

      DynamicDocumentItem doc = new DynamicDocumentItem(new HashMap<>());
      doc.put("documentId", documentId);
      List<Map<String, Object>> taglist = new ArrayList<>();

      for (String tagKey : tagKeys) {
        taglist.add(Map.of("documentId", documentId, "key", tagKey, "value", "test", "insertedDate",
            new Date(), "userId", username, "type", DocumentTagType.USERDEFINED.name()));
      }

      doc.put("tags", taglist);

      // when
      DocumentItem item = service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);
    }
  }

  /**
   * Test Saving / updating folders.
   * 
   * @throws InterruptedException InterruptedException
   */
  @Test
  public void testSaveFolders01() throws InterruptedException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String userId0 = "joe";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), userId0);
      item.setPath("a/b/test.txt");
      service.saveDocument(siteId, item, null);

      // when
      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));

      // then
      PaginationResults<DynamicDocumentItem> folders =
          searchService.search(siteId, q, null, MAX_RESULTS);

      assertEquals(1, folders.getResults().size());
      DynamicDocumentItem result = folders.getResults().get(0);
      assertEquals(result.get("insertedDate"), result.get("lastModifiedDate"));
      assertEquals(Boolean.TRUE, result.get("folder"));
      assertEquals("a", result.get("path"));
      assertEquals(userId0, result.get("userId"));

      TimeUnit.SECONDS.sleep(1);

      // given
      String userId1 = "frank";
      documentId = UUID.randomUUID().toString();
      item = new DocumentItemDynamoDb(documentId, new Date(), userId1);
      item.setPath("a/something.txt");
      service.saveDocument(siteId, item, null);

      // when
      q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));

      // then
      folders = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      result = folders.getResults().get(0);
      assertEquals(Boolean.TRUE, result.get("folder"));
      assertEquals("a", result.get("path"));
      assertEquals(userId0, result.get("userId"));
      assertNotEquals(result.get("insertedDate"), result.get("lastModifiedDate"));
    }
  }

  /**
   * Test Saving / adding folders.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSaveFolders02() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String userId0 = "joe";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), userId0);
      item.setPath("a/b/test.txt");
      service.saveDocument(siteId, item, null);

      // when
      service.addFolderIndex(siteId, item);
      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));

      // then
      PaginationResults<DynamicDocumentItem> folders =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      DynamicDocumentItem result = folders.getResults().get(0);
      assertEquals(result.get("insertedDate"), result.get("lastModifiedDate"));
      assertEquals(Boolean.TRUE, result.get("folder"));
      assertEquals("a", result.get("path"));
      assertEquals(userId0, result.get("userId"));

      TimeUnit.SECONDS.sleep(1);

      // given
      String userId1 = "frank";
      documentId = UUID.randomUUID().toString();
      item = new DocumentItemDynamoDb(documentId, new Date(), userId1);
      item.setPath("a/something.txt");
      service.saveDocument(siteId, item, null);

      // when
      q = new SearchQuery().meta(new SearchMetaCriteria().folder(""));

      // then
      folders = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, folders.getResults().size());
      result = folders.getResults().get(0);
      assertEquals(Boolean.TRUE, result.get("folder"));
      assertEquals("a", result.get("path"));
      assertEquals(userId0, result.get("userId"));
      assertNotEquals(result.get("insertedDate"), result.get("lastModifiedDate"));
    }
  }

  /**
   * Test Saving multiple files/folders.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSaveFolders03() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String path = "a/b/test.txt";
      String userId0 = "joe";
      String documentId0 = UUID.randomUUID().toString();
      DocumentItem item0 = new DocumentItemDynamoDb(documentId0, null, userId0);
      item0.setPath(path);

      // when
      service.saveDocument(siteId, item0, null);
      final Date item0Date =
          service.findDocument(siteId, item0.getDocumentId()).getLastModifiedDate();

      TimeUnit.SECONDS.sleep(1);

      String documentId1 = UUID.randomUUID().toString();
      DocumentItem item1 = new DocumentItemDynamoDb(documentId1, null, userId0);
      item1.setPath(path);
      service.saveDocument(siteId, item1, null);
      final Date item1Date =
          service.findDocument(siteId, item1.getDocumentId()).getLastModifiedDate();

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
      SearchQuery q = new SearchQuery().meta(smc);

      PaginationResults<DynamicDocumentItem> items =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      DynamicDocumentItem result = items.getResults().get(0);
      assertEquals("a", result.get("path"));
      assertEquals(item0Date, result.getLastModifiedDate());

      smc = new SearchMetaCriteria().folder("a");
      q = new SearchQuery().meta(smc);
      items = searchService.search(siteId, q, null, MAX_RESULTS);

      assertEquals(1, items.getResults().size());
      result = items.getResults().get(0);
      assertEquals("b", result.get("path"));
      assertNotEquals(item1Date, result.getLastModifiedDate());

      smc = new SearchMetaCriteria().folder("a/b");
      q = new SearchQuery().meta(smc);
      items = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(2, items.getResults().size());
      assertEquals("a/b/test (" + items.getResults().get(0).get("documentId") + ").txt",
          items.getResults().get(0).get("path"));
      assertEquals("a/b/test.txt", items.getResults().get(1).get("path"));
    }
  }

  /**
   * Test Saving same document multiple times.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testSaveFolders04() throws Exception {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String path = "a/test.txt";
      String userId0 = "joe";
      String documentId0 = UUID.randomUUID().toString();
      DocumentItem item0 = new DocumentItemDynamoDb(documentId0, new Date(), userId0);
      item0.setPath(path);

      // when
      service.saveDocument(siteId, item0, null);
      service.saveDocument(siteId, item0, null);

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
      SearchQuery q = new SearchQuery().meta(smc);

      PaginationResults<DynamicDocumentItem> items =
          searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      DynamicDocumentItem result = items.getResults().get(0);
      assertEquals("a", result.get("path"));

      smc = new SearchMetaCriteria().folder("a");
      q = new SearchQuery().meta(smc);
      items = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, items.getResults().size());
      result = items.getResults().get(0);
      assertEquals("a/test.txt", result.get("path"));
    }
  }

  /**
   * Update Document.
   */
  @Test
  public void updateDocument01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("test.pdf");
      service.saveDocument(siteId, item, null);
      Map<String, AttributeValue> newAttributes =
          Map.of("path", AttributeValue.fromS("sample.pdf"));

      // when
      service.updateDocument(siteId, documentId, newAttributes, false);
      service.updateDocument(siteId, documentId, newAttributes, true);

      // then
      assertEquals("sample.pdf", service.findDocument(siteId, documentId).getPath());
    }
  }
}
