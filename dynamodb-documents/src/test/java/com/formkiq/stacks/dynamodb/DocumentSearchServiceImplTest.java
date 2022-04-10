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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URISyntaxException;
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
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link DocumentSearchServiceImpl}. */
public class DocumentSearchServiceImplTest {

  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder adb;

  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbhelper;

  /**
   * Generate Test Data.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @BeforeClass
  public static void beforeClass() throws IOException, URISyntaxException {

    AwsCredentialsProvider cred = StaticCredentialsProvider
        .create(AwsSessionCredentials.create("ACCESSKEY", "SECRETKEY", "TOKENKEY"));

    adb = new DynamoDbConnectionBuilder().setCredentials(cred).setRegion(Region.US_EAST_1)
        .setEndpointOverride("http://localhost:8000");

    dbhelper = new DynamoDbHelper(adb);

    if (!dbhelper.isDocumentsTableExists()) {
      dbhelper.createDocumentsTable();
      dbhelper.createCacheTable();
    }
  }

  /** {@link SimpleDateFormat}. */
  private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  /** Document Table. */
  private DocumentService service;

  /**
   * Before Test.
   *
   * @throws Exception Exception
   */
  @Before
  public void before() throws Exception {

    this.df.setTimeZone(TimeZone.getTimeZone("UTC"));

    this.service = new DocumentServiceImpl(adb, "Documents");

    dbhelper.truncateDocumentsTable();
    dbhelper.truncateDocumentDates();
    dbhelper.truncateWebhooks();
    dbhelper.truncateConfig();

    assertEquals(0, dbhelper.getDocumentItemCount());
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
      items.add(createDocument(UUID.randomUUID().toString(), zdate, "text/plain", "test.txt"));
    });

    items.forEach(item -> {
      Collection<DocumentTag> tags = Arrays.asList(
          new DocumentTag(item.getDocumentId(), "status", "active", new Date(), "testuser"));
      this.service.saveDocument(prefix, item, tags);
    });

    return items;
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

  /** Search by 'eq' Tag Key & Value. */
  @Test
  public void testSearch01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData("finance");
      createTestData(prefix);
      String tagKey = "status";
      String tagValue = "active";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(prefix, q, startkey, MAX_RESULTS);

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

  /** Search by Tag Key & Invalid Value. */
  @Test
  public void testSearch02() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);
      String tagKey = "day";
      String tagValue = "today2";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(prefix, q, startkey, MAX_RESULTS);

      // then
      assertEquals(0, results.getResults().size());
      assertNull(results.getToken());
    }
  }

  /** Missing TagValue. */
  @Test
  public void testSearch03() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagName = null;
      String tagValue = "today";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagName);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      try {
        dbhelper.getSearchService().search(prefix, q, startkey, MAX_RESULTS);
        fail();
      } catch (Exception e) {
        // then
        assertEquals("'key' attribute is required.", e.getMessage());
      }
    }
  }

  /** Search by 'beginsWith' Tag Key & Value. */
  @Test
  public void testSearch04() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);
      createTestData("finance");
      String tagKey = "status";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagKey).beginsWith("a");
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(prefix, q, startkey, MAX_RESULTS);

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

  /** Search by 'eq' Tag Key & Value & paginating. */
  @Test
  public void testSearch05() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData("finance");
      createTestData(prefix);

      int limit = 1;
      String tagKey = "status";
      String tagValue = "active";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(prefix, q, startkey, limit);

      // then
      assertEquals(1, results.getResults().size());
      assertNotNull(results.getToken());

      // given
      startkey = results.getToken();

      // when
      PaginationResults<DynamicDocumentItem> results2 =
          dbhelper.getSearchService().search(prefix, q, startkey, limit);

      // then
      assertEquals(1, results.getResults().size());
      assertNotEquals(results.getResults().get(0).getDocumentId(),
          results2.getResults().get(0).getDocumentId());
      assertEquals("status", results.getResults().get(0).getMap("matchedTag").get("key"));
      assertEquals("active", results.getResults().get(0).getMap("matchedTag").get("value"));
    }
  }
  
  /** Search multi-value tag 'eq' Tag Key & Value. */
  @Test
  public void testSearch06() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData("finance");
      List<DocumentItem> items = createTestData(siteId);
      DocumentItem item = items.get(0);
      DocumentTag tag =
          new DocumentTag(item.getDocumentId(), "status", null, new Date(), "testuser")
              .setValues(Arrays.asList("active", "notactive"));
      this.service.saveDocument(siteId, item, Arrays.asList(tag));
      
      String tagKey = "status";
      String tagValue = "notactive";
      PaginationMapToken startkey = null;
      SearchTagCriteria c = new SearchTagCriteria(tagKey);
      c.eq(tagValue);
      SearchQuery q = new SearchQuery().tag(c);

      // when
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);

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
  
  /** Search for tag 'eq' with DocumentId. */
  @Test
  public void testSearch07() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DynamicDocumentItem doc0 = createTestDocumentWithTags(Map.of("category", "person"), true);
      DynamicDocumentItem doc1 = createTestDocumentWithTags(Map.of("category", "thing"), true);
      DynamicDocumentItem doc2 = createTestDocumentWithTags(Map.of("nocategory", ""), true);
      this.service.saveDocumentItemWithTag(siteId, doc0);
      this.service.saveDocumentItemWithTag(siteId, doc1);
      this.service.saveDocumentItemWithTag(siteId, doc2);
      
      SearchTagCriteria c = new SearchTagCriteria("category").eq("thing");
      SearchQuery q = new SearchQuery().tag(c);
      q.documentsIds(
          Arrays.asList(doc0.getDocumentId(), doc1.getDocumentId(), doc2.getDocumentId()));
      
      PaginationMapToken startkey = null;

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
      
      // given
      q.documentsIds(Arrays.asList("123"));
      // when
      results = dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }
  
  /** Search for tag with DocumentId. */
  @Test
  public void testSearch08() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given - tag only
      PaginationMapToken startkey = null;
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
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);
      // then
      assertEquals(2, results.getResults().size());
      
      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertNotNull(s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }
  
  /** Search for wrong eq with DocumentId. */
  @Test
  public void testSearch09() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given - wrong value
      PaginationMapToken startkey = null;
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
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }
  
  /** Search for tag 'beginsWith' with DocumentId. */
  @Test
  public void testSearch10() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
      
      PaginationMapToken startkey = null;

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
    }
  }
  
  /** Search for 100 DocumentIds. */
  @Test
  public void testSearch11() {
    // given
    String siteId = null;
    final int count = 100;
    SearchTagCriteria c = new SearchTagCriteria("category");
    SearchQuery q = new SearchQuery().tag(c);

    Collection<String> docNumbers = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      DynamicDocumentItem doc = createTestDocumentWithTags(Map.of("category", "person_" + i), true);
      docNumbers.add(doc.getDocumentId());
      this.service.saveDocumentItemWithTag(siteId, doc);
    }

    q.documentsIds(docNumbers);

    PaginationMapToken startkey = null;

    // when - wrong document id
    PaginationResults<DynamicDocumentItem> results =
        dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);

    // then
    assertEquals(count, results.getResults().size());
  }

  /** Search for tag 'eq' with DocumentId & values. */
  @Test
  public void testSearch12() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
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
      
      PaginationMapToken startkey = null;

      // when - wrong document id
      PaginationResults<DynamicDocumentItem> results =
          dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);

      // then
      assertEquals(1, results.getResults().size());
      assertNull(results.getToken());

      results.getResults().forEach(s -> {
        assertNotNull(s.getInsertedDate());
        assertNull(s.getPath());
        assertEquals("category", s.getMap("matchedTag").get("key"));
        assertEquals("thing", s.getMap("matchedTag").get("value"));
        assertEquals("USERDEFINED", s.getMap("matchedTag").get("type"));
        assertNull(s.getMap("matchedTag").get("documentId"));
        DocumentItem i = this.service.findDocument(siteId, s.getDocumentId());
        assertNotNull(i);
      });
      
      // given
      q.documentsIds(Arrays.asList("123"));
      // when
      results = dbhelper.getSearchService().search(siteId, q, startkey, MAX_RESULTS);
      // then
      assertEquals(0, results.getResults().size());
    }
  }
  
  /**
   * Create a Test Document with 2 tags.
   * @param tags {@link Map}
   * @param value whether to set value or values
   * @return {@link DynamicDocumentItem}
   */
  private DynamicDocumentItem createTestDocumentWithTags(final Map<String, Object> tags,
      final boolean value) {
    String username = "testuser";
    String content = UUID.randomUUID().toString();
    DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
        UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
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
}
