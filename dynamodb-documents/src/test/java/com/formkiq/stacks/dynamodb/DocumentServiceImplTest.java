/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.stacks.dynamodb.DocumentService.SYSTEM_DEFINED_TAGS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link DocumentServiceImpl}. */
public class DocumentServiceImplTest {

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

    this.service = dbhelper.getService();

    dbhelper.truncateDocumentsTable();

    // assertEquals(0, dbhelper.getDocumentItemCount());
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

  /** Add Invalid Tag Name. */
  @Test
  public void testAddTags01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem document = createTestData(prefix).get(0);
      String documentId = document.getDocumentId();
      String tagName = "tag\t";
      String tagValue = UUID.randomUUID().toString();
      String userId = "jsmith";

      List<DocumentTag> tags = Arrays.asList(
          new DocumentTag(documentId, tagName, tagValue, document.getInsertedDate(), userId));

      // when
      try {
        this.service.addTags(prefix, documentId, tags);
        fail();

      } catch (Exception e) {
        // then
        assertEquals("Tabs are not allowed in Tag Name", e.getMessage());
      }
    }
  }

  /** Add Tag Name only. */
  @Test
  public void testAddTags02() {

    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
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
      this.service.saveDocument(prefix, item, tags);

      // then
      PaginationResults<DocumentTag> results =
          this.service.findDocumentTags(prefix, documentId, null, MAX_RESULTS);
      assertNull(results.getToken());
      assertEquals(1, results.getResults().size());
      assertEquals(tagKey, results.getResults().get(0).getKey());
      assertEquals(DocumentTagType.USERDEFINED, results.getResults().get(0).getType());
      assertNull(results.getResults().get(0).getValue());

      tagValue = this.service.findDocumentTag(prefix, documentId, tagKey).getValue();
      assertNull(tagValue);

      SearchTagCriteria s = new SearchTagCriteria(tagKey);
      PaginationResults<DynamicDocumentItem> list =
          dbhelper.getSearchService().search(prefix, s, null, MAX_RESULTS);
      assertNull(list.getToken());
      assertEquals(1, list.getResults().size());
      assertEquals(documentId, list.getResults().get(0).getDocumentId());
      assertEquals("tag", list.getResults().get(0).getMap("matchedTag").get("key"));
      assertNull(list.getResults().get(0).getMap("matchedTag").get("value"));
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
    this.service.saveDocument(siteId, item, tags);

    // then
    assertEquals(0,
        this.service.findDocumentTags(siteId, documentId, null, MAX_RESULTS).getResults().size());
  }

  /** Delete Document. */
  @Test
  public void testDeleteDocument01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String userId = "jsmith";
      DocumentItem item = new DocumentItemDynamoDb(UUID.randomUUID().toString(), now, userId);
      final String documentId = item.getDocumentId();

      DocumentTag tag = new DocumentTag(null, "status", "active", now, userId);
      tag.setUserId(UUID.randomUUID().toString());

      this.service.saveDocument(prefix, item, Arrays.asList(tag));

      // when
      this.service.deleteDocument(prefix, documentId);

      // then
      assertNull(this.service.findDocument(prefix, documentId));
      PaginationResults<DocumentTag> results =
          this.service.findDocumentTags(prefix, documentId, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());
    }
  }

  /** Find valid document. */
  @Test
  public void testFindDocument01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      DocumentItem document = createTestData(prefix).get(0);
      String documentId = document.getDocumentId();

      // when
      DocumentItem item = this.service.findDocument(prefix, documentId);

      // then
      assertEquals(documentId, item.getDocumentId());
      assertNotNull(item.getInsertedDate());
      assertEquals(document.getInsertedDate(), item.getInsertedDate());
      assertNotNull(item.getPath());
      assertEquals("text/plain", item.getContentType());
      assertNotNull(item.getChecksum());
      assertNotNull(item.getUserId());
      assertNotNull(item.getContentLength());
    }
  }

  /** Find documents. */
  @Test
  public void testFindDocuments01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Iterator<DocumentItem> itr = createTestData(prefix).iterator();
      DocumentItem d0 = itr.next();
      DocumentItem d1 = itr.next();
      DocumentItem d2 = itr.next();

      createTestData("finance");

      Collection<String> documentIds =
          Arrays.asList(d0.getDocumentId(), d1.getDocumentId(), d2.getDocumentId());

      // when
      List<DocumentItem> items = this.service.findDocuments(prefix, documentIds);

      // then
      int i = 0;
      assertEquals(items.size(), documentIds.size());
      assertEquals(d0.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertEquals(d0.getInsertedDate(), items.get(i++).getInsertedDate());

      assertEquals(d1.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertEquals(d1.getInsertedDate(), items.get(i++).getInsertedDate());

      assertEquals(d2.getDocumentId(), items.get(i).getDocumentId());
      assertNotNull(items.get(i).getInsertedDate());
      assertEquals(d2.getInsertedDate(), items.get(i++).getInsertedDate());
    }
  }

  /** Find all documents. */
  @Test
  public void testFindDocuments02() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData("finance");
      List<String> documentIds =
          createTestData(prefix).stream().map(k -> k.getDocumentId()).collect(Collectors.toList());

      // when
      List<DocumentItem> items = this.service.findDocuments(prefix, documentIds);

      // then
      assertEquals(items.size(), documentIds.size());
      assertNotNull(items.get(0).getDocumentId());
      assertNotNull(items.get(0).getInsertedDate());
      assertNotNull(items.get(0).getInsertedDate());
    }
  }

  /**
   * Test finding 10 documents all created in one day.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testFindDocumentsByDate01() throws Exception {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);
      createTestData("finance");

      List<String> expected = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]", "2020-01-30T05:20Z[UTC]", "2020-01-30T11:45Z[UTC]",
          "2020-01-30T13:22Z[UTC]", "2020-01-30T17:02Z[UTC]", "2020-01-30T19:10Z[UTC]",
          "2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          this.service.findDocumentsByDate(prefix, date, null, MAX_RESULTS);

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
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);

      final int max = 3;
      List<String> expected0 = Arrays.asList("2020-01-30T00:00Z[UTC]", "2020-01-30T01:20Z[UTC]",
          "2020-01-30T02:20Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-29T18:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          this.service.findDocumentsByDate(prefix, date, null, max);

      // then
      assertEquals(max, results.getResults().size());

      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());

      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      if (prefix == null) {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=2020-01-30, GSI1SK=2020-01-30T02:20:00+0000, SK=document, PK="));
      } else {
        assertTrue(results.getToken().toString().startsWith("{GSI1PK=" + prefix
            + "/2020-01-30, GSI1SK=2020-01-30T02:20:00+0000, SK=document, PK="));
      }

      // given
      final List<String> expected1 = Arrays.asList("2020-01-30T05:20Z[UTC]",
          "2020-01-30T11:45Z[UTC]", "2020-01-30T13:22Z[UTC]");

      // when - get next page
      results = this.service.findDocumentsByDate(prefix, date, results.getToken(), max);

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
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);

      List<String> expected0 = Arrays.asList("2020-01-30T19:10Z[UTC]", "2020-01-30T20:54Z[UTC]",
          "2020-01-30T23:59:59Z[UTC]", "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]",
          "2020-01-31T05:00Z[UTC]", "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]",
          "2020-01-31T09:00Z[UTC]", "2020-01-31T10:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T12:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          this.service.findDocumentsByDate(prefix, date, null, MAX_RESULTS);

      // then
      assertEquals(expected0.size(), results.getResults().size());

      List<String> resultDates = results
          .getResults().stream().map(r -> ZonedDateTime
              .ofInstant(r.getInsertedDate().toInstant(), ZoneId.of("UTC")).toString())
          .collect(Collectors.toList());
      assertArrayEquals(expected0.toArray(new String[0]), resultDates.toArray(new String[0]));

      if (prefix == null) {
        assertTrue(results.getToken().toString()
            .startsWith("{GSI1PK=2020-01-31, GSI1SK=2020-01-31T10:00:00+0000, SK=document, PK="));
      } else {
        assertTrue(results.getToken().toString().startsWith("{GSI1PK=" + prefix
            + "/2020-01-31, GSI1SK=2020-01-31T10:00:00+0000, SK=document, PK="));
      }

      // given
      List<String> expected1 = Arrays.asList("2020-01-31T11:00Z[UTC]");

      // when
      results = this.service.findDocumentsByDate(prefix, date, results.getToken(), MAX_RESULTS);

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
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      createTestData(prefix);

      List<String> expected0 = Arrays.asList("2020-01-30T20:54Z[UTC]", "2020-01-30T23:59:59Z[UTC]",
          "2020-01-31T00:00Z[UTC]", "2020-01-31T03:00Z[UTC]", "2020-01-31T05:00Z[UTC]",
          "2020-01-31T07:00Z[UTC]", "2020-01-31T08:00Z[UTC]", "2020-01-31T09:00Z[UTC]",
          "2020-01-31T10:00Z[UTC]", "2020-01-31T11:00Z[UTC]");

      ZonedDateTime date = DateUtil.toDateTimeFromString("2020-01-30T14:00:00", "-600");

      // when
      PaginationResults<DocumentItem> results =
          this.service.findDocumentsByDate(prefix, date, null, MAX_RESULTS);

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

  /** Test Finding Document's Tag && Remove one. */
  @Test
  public void testFindDocumentTags01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      PaginationMapToken startkey = null;
      createTestData("finance");
      String documentId = createTestData(prefix).get(0).getDocumentId();

      // when
      PaginationResults<DocumentTag> results =
          this.service.findDocumentTags(prefix, documentId, startkey, MAX_RESULTS);

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
      this.service.removeTags(prefix, documentId, tags);

      // then
      results = this.service.findDocumentTags(prefix, documentId, startkey, MAX_RESULTS);

      assertNull(results.getToken());
      assertEquals(0, results.getResults().size());
    }
  }

  /** Test Finding Document's particular Tag. */
  @Test
  public void testFindDocumentTags02() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "status";
      String tagValue = "active";
      String documentId = createTestData(prefix).get(0).getDocumentId();

      // when
      String result = this.service.findDocumentTag(prefix, documentId, tagKey).getValue();

      // then
      assertEquals(tagValue, result);
    }
  }

  /** Test Finding Document's Tag does not exist. */
  @Test
  public void testFindDocumentTags03() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String tagKey = "status";
      String documentId = createTestData(prefix).get(0).getDocumentId();

      // when
      DocumentTag result = this.service.findDocumentTag(prefix, documentId, tagKey + "!");

      // then
      assertNull(result);
    }
  }

  /**
   * Find / Save Presets.
   */
  @Test
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

          this.service.savePreset(siteId, id, type, preset, null);
        }

        // when
        PaginationResults<Preset> p0 =
            this.service.findPresets(siteId, null, type, null, null, MAX_RESULTS);

        // then
        assertEquals(MAX_RESULTS, p0.getResults().size());
        assertNotNull(p0.getToken());

        // when
        PaginationResults<Preset> p1 = this.service.findPresets(siteId, preset.getId(), type,
            preset.getName(), null, MAX_RESULTS);

        // then
        assertEquals(1, p1.getResults().size());
        assertEquals(preset.getName(), p1.getResults().get(0).getName());
        assertEquals(type, p1.getResults().get(0).getType());
        assertNotNull(p1.getResults().get(0).getInsertedDate());
        assertNotNull(p1.getResults().get(0).getId());
        assertNull(p1.getResults().get(0).getUserId());

        // when
        p0 = this.service.findPresets(siteId, null, type, null, p0.getToken(), MAX_RESULTS);

        // then
        assertEquals(ids.size() - MAX_RESULTS, p0.getResults().size());

        // when
        Optional<Preset> p = this.service.findPreset(siteId, preset.getId());

        // then
        assertTrue(p.isPresent());
        assertEquals(preset.getId(), p.get().getId());

      } finally {
        ids.forEach(id -> {
          this.service.deletePreset(siteId, id);
        });

        PaginationResults<Preset> p0 =
            this.service.findPresets(siteId, null, type, null, null, MAX_RESULTS);
        assertEquals(0, p0.getResults().size());
      }
    }
  }

  /**
   * Test Find Preset Tag.
   */
  @Test
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

        this.service.savePreset(siteId, presetId, type, null, Arrays.asList(tag));

        // when
        PaginationResults<PresetTag> results =
            this.service.findPresetTags(siteId, presetId, null, MAX_RESULTS);
        Optional<PresetTag> ptag = this.service.findPresetTag(siteId, presetId, tag.getKey());

        // then
        assertEquals(1, results.getResults().size());
        assertTrue(ptag.isPresent());
        assertEquals(tag.getKey(), ptag.get().getKey());
        assertEquals("joe", ptag.get().getUserId());
        assertNotNull(ptag.get().getInsertedDate());

      } finally {
        this.service.deletePresetTag(siteId, presetId, tag.getKey());
        assertEquals(0,
            this.service.findPresetTags(siteId, presetId, null, MAX_RESULTS).getResults().size());
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
      this.service.saveDocument(siteId, item, null);

      // when
      Optional<DocumentFormat> format =
          this.service.findDocumentFormat(siteId, documentId, contentType);
      PaginationResults<DocumentFormat> formats =
          this.service.findDocumentFormats(siteId, documentId, null, MAX_RESULTS);

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
      this.service.saveDocument(siteId, item, null);

      for (String format : contentTypes) {
        DocumentFormat f = new DocumentFormat();
        f.setContentType(format);
        f.setDocumentId(documentId);
        f.setInsertedDate(now);
        f.setUserId(userId);
        this.service.saveDocumentFormat(siteId, f);
      }

      // when
      Optional<DocumentFormat> format =
          this.service.findDocumentFormat(siteId, documentId, contentTypes.get(0));
      PaginationResults<DocumentFormat> formats =
          this.service.findDocumentFormats(siteId, documentId, null, 1);

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
      formats = this.service.findDocumentFormats(siteId, documentId, formats.getToken(), 1);

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
   * Test Remove Tags from Document.
   */
  @Test
  public void testRemoveTags01() {
    for (String prefix : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String docid = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(docid, new Date(), "jsmith");

      Collection<DocumentTag> tags =
          Arrays.asList(new DocumentTag(docid, "untagged", "true", new Date(), "jsmith"));
      this.service.saveDocument(prefix, item, tags);

      // when
      this.service.removeTags(prefix, docid, Arrays.asList(tags.iterator().next().getKey()));

      // then
      assertEquals(0,
          this.service.findDocumentTags(prefix, docid, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test Save {@link DocumentItem} with {@link DocumentTag}.
   */
  @Test
  public void testSaveDocumentItemWithTag01() {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String content = "This is a test";
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", new Date(), "content",
          Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8))));

      // when
      DocumentItem item = this.service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = this.service.findDocument(siteId, item.getDocumentId());
      assertNotNull(item);

      PaginationResults<DocumentTag> tags =
          this.service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
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
        DocumentItem item = this.service.saveDocumentItemWithTag(siteId, doc);

        // then
        item = this.service.findDocument(siteId, item.getDocumentId());
        assertNotNull(item);

        PaginationResults<DocumentTag> tags =
            this.service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS);
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
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", now));

      DynamicDocumentItem doc1 = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
      doc1.put("tags", Arrays.asList(Map.of("documentId", doc1.getDocumentId(), "key", "category1",
          "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

      DynamicDocumentItem doc2 = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
      doc2.put("tags", Arrays.asList(Map.of("documentId", doc2.getDocumentId(), "key", "category2",
          "insertedDate", now, "userId", username, "type", DocumentTagType.USERDEFINED.name())));

      doc.put("documents", Arrays.asList(doc1, doc2));

      // when
      DocumentItem item = this.service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = this.service.findDocument(siteId, doc.getDocumentId(), true);
      assertNotNull(item);
      assertEquals(2, item.getDocuments().size());
      List<String> ids = Arrays.asList(doc1.getDocumentId(), doc2.getDocumentId());
      Collections.sort(ids);

      assertEquals(ids.get(0), item.getDocuments().get(0).getDocumentId());
      assertEquals(ids.get(1), item.getDocuments().get(1).getDocumentId());

      List<DocumentTag> tags = this.service
          .findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS).getResults();
      assertEquals(1, tags.size());
      assertEquals("untagged", tags.get(0).getKey());
      assertEquals("true", tags.get(0).getValue());

      item = this.service.findDocument(siteId, doc1.getDocumentId());
      assertNotNull(item);
      assertEquals(doc.getDocumentId(), item.getBelongsToDocumentId());

      tags = this.service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS)
          .getResults();
      assertEquals(1, tags.size());
      assertEquals("category1", tags.get(0).getKey());

      item = this.service.findDocument(siteId, doc2.getDocumentId());
      assertNotNull(item);
      assertEquals(doc.getDocumentId(), item.getBelongsToDocumentId());

      tags = this.service.findDocumentTags(siteId, item.getDocumentId(), null, MAX_RESULTS)
          .getResults();
      assertEquals(1, tags.size());
      assertEquals("category2", tags.get(0).getKey());

      assertEquals(1,
          this.service.findDocumentsByDate(siteId, nowDate, null, MAX_RESULTS).getResults().size());
    }
  }

  /**
   * Test Save sSubDocument.
   */
  @Test
  public void testSaveDocumentItemWithTag04() {
    Date now = new Date();
    String belongsToDocumentId = UUID.randomUUID().toString();

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String username = UUID.randomUUID() + "@formkiq.com";

      DynamicDocumentItem doc = new DynamicDocumentItem(Map.of("documentId",
          UUID.randomUUID().toString(), "userId", username, "insertedDate", now));
      doc.setBelongsToDocumentId(belongsToDocumentId);

      // when
      DocumentItem item = this.service.saveDocumentItemWithTag(siteId, doc);

      // then
      item = this.service.findDocument(siteId, doc.getDocumentId(), true);
      assertNotNull(item);
      assertNotNull(item.getBelongsToDocumentId());
    }
  }
}
