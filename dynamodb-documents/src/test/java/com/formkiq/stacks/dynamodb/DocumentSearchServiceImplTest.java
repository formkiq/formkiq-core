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
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.ID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.validation.ValidationException;

/** Unit Tests for {@link DocumentSearchServiceImpl}. */
@ExtendWith(DynamoDbExtension.class)
public class DocumentSearchServiceImplTest implements DbKeys {

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  /** {@link DocumentService}. */
  private DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private DocumentService service;

  /**
   * Before All.
   */
  @BeforeAll
  public static void beforeAll() {
    ApiAuthorization.login(new ApiAuthorization().username("System"));
  }

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @BeforeEach
  public void before() throws Exception {

    this.df.setTimeZone(TimeZone.getTimeZone("UTC"));
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    this.service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE,
        DOCUMENT_SYNCS_TABLE, new DocumentVersionServiceNoVersioning());
    this.searchService =
        new DocumentSearchServiceImpl(dynamoDbConnection, this.service, DOCUMENTS_TABLE);
  }

  /**
   * Create Document with Tag.
   * 
   * @param siteId {@link String}
   * @param tagKey {@link String}
   * @param tagValue {@link String}
   * @return {@link String}
   * @throws ValidationException ValidationException
   */
  private String createDocument(final String siteId, final String tagKey, final String tagValue)
      throws ValidationException {
    ZonedDateTime now = ZonedDateTime.now();
    DocumentItem doc = createDocument(ID.uuid(), now);
    Collection<DocumentTag> tags =
        List.of(new DocumentTag(doc.getDocumentId(), tagKey, tagValue, new Date(), "testuser"));
    this.service.saveDocument(siteId, doc, tags);

    return doc.getDocumentId();
  }

  /**
   * Create Document.
   *
   * @param uuid {@link String}
   * @param date {@link ZonedDateTime}
   * @return {@link DocumentItem}
   */
  private DocumentItem createDocument(final String uuid, final ZonedDateTime date) {

    String userId = "jsmith";

    DocumentItem item = new DocumentItemDynamoDb(uuid, Date.from(date.toInstant()), userId);
    item.setContentType("text/plain");
    item.setPath("test.txt");
    item.setUserId(ID.uuid());
    item.setChecksum(ID.uuid());
    item.setContentLength(2L);
    return item;
  }

  /**
   * Create Test {@link DocumentItem}.
   *
   * @param prefix DynamoDB PK Prefix
   * @return {@link List} {@link DocumentItem}
   */
  private List<DocumentItem> createTestData(final String prefix) {

    List<String> dates = Arrays.asList("2020-01-30T00:00:00", "2020-01-30T01:20:00",
        "2020-01-30T02:20:00", "2020-01-30T05:20:00", "2020-01-30T11:45:00", "2020-01-30T13:22:00",
        "2020-01-30T17:02:00", "2020-01-30T19:10:00", "2020-01-30T20:54:00", "2020-01-30T23:59:59",
        "2020-01-31T00:00:00", "2020-01-31T03:00:00", "2020-01-31T05:00:00", "2020-01-31T07:00:00",
        "2020-01-31T08:00:00", "2020-01-31T09:00:00", "2020-01-31T10:00:00", "2020-01-31T11:00:00",
        "2020-01-31T23:00:00");

    List<DocumentItem> items = new ArrayList<>();

    dates.forEach(date -> {
      ZonedDateTime zdate = DateUtil.toDateTimeFromString(date, null);
      String id = ID.uuid();
      items.add(createDocument(id, zdate));
    });

    items.forEach(item -> {
      Collection<DocumentTag> tags = List
          .of(new DocumentTag(item.getDocumentId(), "status", "active", new Date(), "testuser"));
      try {
        this.service.saveDocument(prefix, item, tags);
      } catch (ValidationException e) {
        throw new RuntimeException(e);
      }
    });

    return items;
  }

  /**
   * Create a Test Document with 2 tags.
   * 
   * @param tags {@link Map}
   * @param value whether to set value or values
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createTestDocumentWithTags(final Map<String, Object> tags,
      final boolean value) {
    String username = "testuser";
    String content = ID.uuid();
    DynamicDocumentItem doc = new DynamicDocumentItem(
        Map.of("documentId", ID.uuid(), "userId", username, "insertedDate", new Date(), "content",
            Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));


    List<Map<String, Object>> list = new ArrayList<>();
    doc.put("tags", list);

    for (Map.Entry<String, Object> e : tags.entrySet()) {
      if (value) {
        list.add(Map.of("documentId", doc.getDocumentId(), "key", e.getKey(), "value", e.getValue(),
            "insertedDate", new Date(), "userId", username, "type",
            DocumentTagType.USERDEFINED.name()));
      } else {
        list.add(Map.of("documentId", doc.getDocumentId(), "key", e.getKey(), "values",
            e.getValue(), "insertedDate", new Date(), "userId", username, "type",
            DocumentTagType.USERDEFINED.name()));
      }
    }

    return doc;
  }

  /**
   * Search by 'eq' Tag Key & Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch01() throws ValidationException {
    for (String prefix : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData("finance");
      createTestData(prefix);
      String tagKey = "status";
      String tagValue = "active";
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(prefix, q, null, null, MAX_RESULTS);

      // then
      assertEquals(MAX_RESULTS, results.getResults().size());
      assertNotNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("status", s.getMap("matchedTag").get("key"));
        assertEquals("active", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(prefix, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search by Tag Key & Invalid Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch02() throws ValidationException {
    for (String prefix : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(prefix);
      String tagKey = "day";
      String tagValue = "today2";
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(prefix, q, null, null, MAX_RESULTS);

      // then
      assertEquals(0, results.getResults().size());
      assertNull(results.getToken());
    }
  }

  /**
   * Search by 'beginsWith' Tag Key & Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch03() throws ValidationException {
    for (String prefix : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData(prefix);
      createTestData("finance");
      String tagKey = "status";
      SearchTagCriteria c = new SearchTagCriteria(tagKey).beginsWith("a");
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(prefix, q, null, null, MAX_RESULTS);

      // then
      assertEquals(MAX_RESULTS, results.getResults().size());
      assertNotNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("status", s.getMap("matchedTag").get("key"));
        assertEquals("active", s.getMap("matchedTag").get("value"));
        DocumentItem i = this.service.findDocument(prefix, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search by 'eq' Tag Key & Value & paginating.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch04() throws ValidationException {
    for (String prefix : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData("finance");
      createTestData(prefix);

      int limit = 1;
      String tagKey = "status";
      String tagValue = "active";
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(prefix, q, null, null, limit);

      // then
      assertEquals(1, results.getResults().size());
      assertNotNull(results.getToken());

      // given
      PaginationMapToken startkey = results.getToken();

      // when
      PaginationResults<DynamicDocumentItem> results2 =
          this.searchService.search(prefix, q, null, startkey, limit);

      // then
      assertEquals(1, results.getResults().size());
      assertNotEquals(results.getResults().get(0).getDocumentId(),
          results2.getResults().get(0).getDocumentId());
      assertEquals("status", results.getResults().get(0).getMap("matchedTag").get("key"));
      assertEquals("active", results.getResults().get(0).getMap("matchedTag").get("value"));
    }
  }

  /**
   * Search multi-value tag 'eq' Tag Key & Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch05() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      createTestData("finance");
      List<DocumentItem> items = createTestData(siteId);
      DocumentItem item = items.get(0);
      DocumentTag tag =
          new DocumentTag(item.getDocumentId(), "status", null, new Date(), "testuser")
              .setValues(List.of("active", "notactive"));
      this.service.saveDocument(siteId, item, List.of(tag));

      String tagKey = "status";
      String tagValue = "notactive";
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("status", s.getMap("matchedTag").get("key"));
        assertEquals("notactive", s.getMap("matchedTag").get("value"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search for tag 'eq' with DocumentId.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch06() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);

      SearchTagCriteria c0 = new SearchTagCriteria("category").eq("thing");
      SearchQuery q = new SearchQuery().tag(c0);
      q.documentsIds(
          Arrays.asList(doc0.getDocumentId(), doc1.getDocumentId(), doc2.getDocumentId()));

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });

      // given
      q = new SearchQuery().tag(new SearchTagCriteria("nocategory"));

      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertEquals(doc2.getDocumentId(), results.getResults().get(0).getDocumentId());

      // given
      q.documentsIds(List.of("123"));
      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Search for tag with DocumentId.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch07() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given - tag only
      SearchTagCriteria c = new SearchTagCriteria("category");
      SearchQuery q = new SearchQuery().tag(c);

      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      // then
      assertEquals(2, results.getResults().size());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertNotNull(s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search for wrong eq with DocumentId.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch08() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given - wrong value
      SearchTagCriteria c = new SearchTagCriteria("category").eq("thing123");
      SearchQuery q = new SearchQuery().tag(c);

      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Search for tag 'beginsWith' with DocumentId.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch09() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);

      SearchTagCriteria c = new SearchTagCriteria("category").beginsWith("th");
      SearchQuery q = new SearchQuery().tag(c);
      q.documentsIds(Arrays.asList(doc0.getDocumentId(), doc1.getDocumentId()));

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search for 100 DocumentIds.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch10() throws ValidationException {
    // given
    final int count = 100;
    SearchTagCriteria c = new SearchTagCriteria("category");
    SearchQuery q = new SearchQuery().tag(c);

    Collection<String> docNumbers = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      DynamicDocumentItem doc = createTestDocumentWithTags(Map.of("category", "person_" + i), true);
      docNumbers.add(doc.getDocumentId());
      this.service.saveDocumentItemWithTag(null, doc);
    }

    q.documentsIds(docNumbers);

    // when - wrong document id
    PaginationResults<DynamicDocumentItem> results =
        this.searchService.search(null, q, null, null, MAX_RESULTS);

    // then
    assertEquals(count, results.getResults().size());
  }

  /**
   * Search for tag 'eq' with DocumentId & values.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch11() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 =
          createTestDocumentWithTags(Map.of("category", Arrays.asList("thing", "thing1")), false);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);

      SearchTagCriteria c = new SearchTagCriteria("category").eq("thing");
      SearchQuery q = new SearchQuery().tag(c);
      q.documentsIds(
          Arrays.asList(doc0.getDocumentId(), doc1.getDocumentId(), doc2.getDocumentId()));

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });

      // given
      q.documentsIds(List.of("123"));
      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Search for tag 'eqOr' with DocumentId & values.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch12() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      doc0.setDocumentId("1");
      DynamicDocumentItem doc1 =
          createTestDocumentWithTags(Map.of("category", Arrays.asList("thing", "thing1")), false);
      doc1.setDocumentId("2");
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("category", "person3"), true);
      doc2.setDocumentId("3");
      DynamicDocumentItem doc3 = createTestDocumentWithTags(Map.of("category", "person2"), true);
      doc3.setDocumentId("4");
      DynamicDocumentItem doc4 = createTestDocumentWithTags(Map.of("category", "person5"), true);
      doc4.setDocumentId("5");

      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);
      this.service.saveDocumentItemWithTag(siteId, doc3);
      this.service.saveDocumentItemWithTag(siteId, doc4);

      SearchTagCriteria c =
          new SearchTagCriteria("category").eqOr(Arrays.asList("thing", "person2"));
      SearchQuery q = new SearchQuery().tag(c);
      q.documentsIds(Arrays.asList(doc0.getDocumentId(), doc1.getDocumentId(), doc2.getDocumentId(),
          doc3.getDocumentId()));

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(2, list.size());
      assertNull(results.getToken());

      assertEquals("category", list.get(0).getMap("matchedTag").get("key"));
      assertEquals("thing", list.get(0).getMap("matchedTag").get("value"));
      assertEquals("USERDEFINED", list.get(0).getMap("matchedTag").get("type"));

      assertEquals("category", list.get(1).getMap("matchedTag").get("key"));
      assertEquals("person2", list.get(1).getMap("matchedTag").get("value"));
      assertEquals("USERDEFINED", list.get(1).getMap("matchedTag").get("type"));

      list.forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });

      // given
      q.documentsIds(List.of("123"));
      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      // then
      list = results.getResults();
      assertEquals(0, list.size());
    }
  }

  /**
   * Search for tag 'eqOr'.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch13() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("category", "person1"), true);
      DynamicDocumentItem doc3 = createTestDocumentWithTags(Map.of("nocategory", "person"), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);
      this.service.saveDocumentItemWithTag(siteId, doc3);

      SearchTagCriteria c =
          new SearchTagCriteria("category").eqOr(Arrays.asList("thing", "person1"));
      SearchQuery q = new SearchQuery().tag(c);

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(2, list.size());
      assertNull(results.getToken());

      assertEquals("thing", list.get(0).getMap("matchedTag").get("value"));
      assertEquals("person1", list.get(1).getMap("matchedTag").get("value"));

      list.forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNotNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }

  /**
   * Search for meta "folder" data.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch14() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentItem doc0 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc0.setPath("test2.pdf");
      this.service.saveDocument(siteId, doc0, null);

      DocumentItem doc1 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc1.setPath("test1.pdf");
      this.service.saveDocument(siteId, doc1, null);

      DocumentItem doc2 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc2.setPath("sample/test3.pdf");
      this.service.saveDocument(siteId, doc2, null);

      DocumentItem doc3 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc3.setPath("sample/anotherone/test4.pdf");
      this.service.saveDocument(siteId, doc3, null);

      String folder = "";
      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(folder));

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      final int expected = 3;
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(expected, list.size());
      assertNull(results.getToken());

      int i = 0;
      assertNotNull(list.get(i).getDocumentId());
      assertEquals("sample", list.get(i++).getPath());
      assertEquals(doc1.getDocumentId(), results.getResults().get(i).getDocumentId());
      assertEquals("test1.pdf", list.get(i++).getPath());
      assertEquals(doc0.getDocumentId(), results.getResults().get(i).getDocumentId());
      assertEquals("test2.pdf", list.get(i).getPath());

      // given
      folder = "sample";
      q = new SearchQuery().meta(new SearchMetaCriteria().folder(folder));

      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      list = results.getResults();
      assertEquals(2, list.size());
      assertEquals("anotherone", list.get(0).getPath());
      assertNotNull(list.get(0).getDocumentId());
      assertEquals("sample/test3.pdf", list.get(1).getPath());
      assertEquals(doc2.getDocumentId(), list.get(1).getDocumentId());

      // given
      folder = "sample/anotherone";
      q = new SearchQuery().meta(new SearchMetaCriteria().folder(folder));

      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      list = results.getResults();
      assertEquals(1, list.size());
      assertEquals("sample/anotherone/test4.pdf", list.get(0).getPath());
      assertEquals(doc3.getDocumentId(), list.get(0).getDocumentId());
    }
  }

  /**
   * Add and Delete Document.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch15() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentItem doc0 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc0.setPath("sample/test2.pdf");
      this.service.saveDocument(siteId, doc0, null);

      SearchQuery q0 = new SearchQuery().meta(new SearchMetaCriteria().folder(""));
      SearchQuery q1 = new SearchQuery().meta(new SearchMetaCriteria().folder("sample"));

      // when
      PaginationResults<DynamicDocumentItem> results0 =
          this.searchService.search(siteId, q0, null, null, MAX_RESULTS);

      PaginationResults<DynamicDocumentItem> results1 =
          this.searchService.search(siteId, q1, null, null, MAX_RESULTS);

      // then
      List<DynamicDocumentItem> list0 = results0.getResults();
      assertEquals(1, list0.size());
      assertEquals("sample", list0.get(0).getPath());

      List<DynamicDocumentItem> list1 = results1.getResults();
      assertEquals(1, list1.size());
      assertEquals("sample/test2.pdf", list1.get(0).getPath());

      // given
      q0 = new SearchQuery().meta(new SearchMetaCriteria().folder(""));
      q1 = new SearchQuery().meta(new SearchMetaCriteria().folder("sample"));

      // when
      this.service.deleteDocument(siteId, doc0.getDocumentId(), false);

      // then
      results0 = this.searchService.search(siteId, q0, null, null, MAX_RESULTS);
      list0 = results0.getResults();
      assertEquals(1, list0.size());
      assertEquals("sample", list0.get(0).getPath());

      results1 = this.searchService.search(siteId, q1, null, null, MAX_RESULTS);
      assertEquals(0, results1.getResults().size());
    }
  }

  /**
   * Search for meta "folder" different cases data.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch16() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentItem doc0 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc0.setPath("Chicago/test2.pdf");
      this.service.saveDocument(siteId, doc0, null);

      DocumentItem doc1 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc1.setPath("abc.pdf");
      this.service.saveDocument(siteId, doc1, null);

      DocumentItem doc2 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc2.setPath("aaaa/test3.pdf");
      this.service.saveDocument(siteId, doc2, null);

      String folder = "";
      SearchQuery q = new SearchQuery().meta(new SearchMetaCriteria().folder(folder));

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      final int expected = 3;
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(expected, list.size());
      assertNull(results.getToken());

      int i = 0;
      assertNotNull(list.get(i).getDocumentId());
      assertEquals("aaaa", list.get(i++).getPath());
      assertEquals("Chicago", list.get(i++).getPath());
      assertEquals("abc.pdf", list.get(i).getPath());
    }
  }

  /**
   * Save document twice and change path make sure only 1 result.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch17() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      DocumentItem doc0 = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc0.setPath("/a/b/test2.pdf");
      this.service.saveDocument(siteId, doc0, null);

      doc0.setPath("/c/b/test3.pdf");
      this.service.saveDocument(siteId, doc0, null);

      String folder = "";
      SearchMetaCriteria meta = new SearchMetaCriteria();
      SearchQuery q = new SearchQuery().meta(meta.folder(folder));

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(2, list.size());
      assertEquals("a", list.get(0).getPath());
      assertEquals("c", list.get(1).getPath());

      meta.folder("a/b");
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(0, list.size());
    }
  }

  /**
   * Save document and search by path.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch18() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      String path = "/a/b/test2.pdf";
      DocumentItem doc = new DocumentItemDynamoDb(ID.uuid(), new Date(), "joe");
      doc.setPath(path);
      this.service.saveDocument(siteId, doc, null);

      SearchMetaCriteria meta = new SearchMetaCriteria();
      SearchQuery q = new SearchQuery().meta(meta.path(path));

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(1, list.size());
      assertEquals(doc.getDocumentId(), list.get(0).getDocumentId());
      assertEquals(doc.getPath(), list.get(0).getPath());

      // given - invalid path
      path = ID.uuid();
      q = new SearchQuery().meta(meta.path(path));

      // when
      results = this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(0, results.getResults().size());
    }
  }

  /**
   * Search by 'eq' / 'beginsWith' Tag Key & Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearchForDocumentIds01() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final String documentId0 = createDocument(siteId, "category", "person0");
      final String documentId1 = createDocument(siteId, "category", "person1");
      createDocument(siteId, "category", "other");

      SearchTagCriteria c = new SearchTagCriteria("category").eq("person0");

      // when
      PaginationResults<String> results =
          this.searchService.searchForDocumentIds(siteId, c, null, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());
      assertEquals(documentId0, results.getResults().get(0));

      // given
      c = new SearchTagCriteria("category").beginsWith("per");

      // when
      results = this.searchService.searchForDocumentIds(siteId, c, null, MAX_RESULTS);

      // then
      assertNull(results.getToken());
      assertEquals(2, results.getResults().size());
      assertTrue(results.getResults().contains(documentId0));
      assertTrue(results.getResults().contains(documentId1));
    }
  }

  /**
   * Search by 'between' Tag Key & Value.
   * 
   * @throws ValidationException ValidationException
   */
  @Test
  public void testSearch19() throws ValidationException {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      final String documentId0 = createDocument(siteId, "date", "2024-03-19T02:45:04+0000");
      final String documentId1 = createDocument(siteId, "date", "2024-03-20T02:45:04+0000");
      createDocument(siteId, "date", "2024-03-21T02:45:04+0000");

      SearchTagCriteria c = new SearchTagCriteria("date")
          .range(new SearchTagCriteriaRange().start("2024-03-10").end("2024-03-21"));
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          this.searchService.search(siteId, q, null, null, MAX_RESULTS);

      // then
      assertEquals(2, results.getResults().size());
      assertEquals(documentId0, results.getResults().get(0).getDocumentId());
      assertEquals(documentId1, results.getResults().get(1).getDocumentId());
    }
  }

  /**
   * Search by duplicate attributes.
   *
   */
  @Test
  public void testSearch20() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      SearchAttributeCriteria a0 = new SearchAttributeCriteria().key("category").eq("person");
      SearchQuery q = new SearchQuery().attributes(Arrays.asList(a0, a0));

      // when
      try {
        this.searchService.search(siteId, q, null, null, MAX_RESULTS);
        fail();
      } catch (ValidationException e) {
        // then
        assertEquals("duplicate attributes in query", e.errors().iterator().next().error());
      }
    }
  }
}
