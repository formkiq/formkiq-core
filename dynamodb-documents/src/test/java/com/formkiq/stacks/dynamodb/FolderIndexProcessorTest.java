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

import static com.formkiq.aws.dynamodb.objects.Strings.isUuid;
import static com.formkiq.stacks.dynamodb.DocumentService.MAX_RESULTS;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.clearSqsQueue;
import static com.formkiq.testutils.aws.TestServices.createSnsTopic;
import static com.formkiq.testutils.aws.TestServices.createSqsSubscriptionToSnsTopic;
import static com.formkiq.testutils.aws.TestServices.getMessagesFromSqs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * 
 * Unit Test {@link FolderIndexProcessorImpl}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
class FolderIndexProcessorTest implements DbKeys {

  /** {@link DynamoDbService}. */
  private static DynamoDbService dbService;
  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor index;
  /** {@link DocumentService}. */
  private static DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private static DocumentService service;
  /** Sqs Queue Url. */
  private static String sqsQueueUrl;

  @BeforeAll
  static void beforeAll() throws Exception {

    String snsTopicArn = createSnsTopic();
    sqsQueueUrl = createSqsSubscriptionToSnsTopic(snsTopicArn);

    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    index = new FolderIndexProcessorImpl(dynamoDbConnection, DOCUMENTS_TABLE);

    service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());
    searchService =
        new DocumentSearchServiceImpl(dynamoDbConnection, service, DOCUMENTS_TABLE, null);
    dbService = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
  }

  @BeforeEach
  void before() throws URISyntaxException {
    clearSqsQueue(sqsQueueUrl);
  }

  /**
   * Test Create all new directories.
   */
  @Test
  void testGenerateIndex01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/b/c/test.pdf");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);
      dbService.putItems(indexes);

      // then
      final String site = siteId != null ? siteId + "/" : "";
      final int expected = 4;
      assertEquals(expected, indexes.size());

      int i = 0;
      Map<String, AttributeValue> map = indexes.get(i++);
      assertTrue(dbService.exists(map.get(PK), map.get(SK)));

      verifyIndex(map, map.get(PK).s(), "ff#a", "a", true);
      String documentIdA = map.get("documentId").s();

      map = indexes.get(i++);
      assertTrue(dbService.exists(map.get(PK), map.get(SK)));
      verifyIndex(map, site + "global#folders#" + documentIdA, "ff#b", "b", true);
      String documentIdB = map.get("documentId").s();

      map = indexes.get(i++);
      assertTrue(dbService.exists(map.get(PK), map.get(SK)));
      verifyIndex(map, site + "global#folders#" + documentIdB, "ff#c", "c", true);
      String documentIdC = map.get("documentId").s();

      map = indexes.get(i++);
      assertTrue(dbService.exists(map.get(PK), map.get(SK)));
      verifyIndex(map, site + "global#folders#" + documentIdC, "fi#test.pdf", "test.pdf", false);

      // when
      indexes = index.generateIndex(siteId, item);

      // then
      assertEquals(0, indexes.size());
    }
  }

  @Test
  void testGenerateIndex02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/test.pdf");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      final int expected = 1;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("fi#test.pdf", indexes.get(i).get(SK).s());
      assertEquals("test.pdf", indexes.get(i).get("path").s());
      assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
    }
  }

  /**
   * Empty Path.
   */
  @Test
  void testGenerateIndex03() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      assertTrue(indexes.isEmpty());
    }
  }

  @Test
  void testGenerateIndex04() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("formkiq:://sample/test.txt");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      final int expected = 3;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("ff#formkiq", indexes.get(i).get(SK).s());
      assertEquals("formkiq", indexes.get(i).get("path").s());
      String documentId0 = indexes.get(i++).get("documentId").s();

      assertEquals(site + "global#folders#" + documentId0, indexes.get(i).get(PK).s());
      assertEquals("ff#sample", indexes.get(i).get(SK).s());
      assertEquals("sample", indexes.get(i).get("path").s());
      String documentId1 = indexes.get(i++).get("documentId").s();
      assertNotEquals(documentId0, documentId1);

      assertEquals(site + "global#folders#" + documentId1, indexes.get(i).get(PK).s());
      assertEquals("fi#test.txt", indexes.get(i).get(SK).s());
      assertEquals("test.txt", indexes.get(i).get("path").s());
      String documentId2 = indexes.get(i).get("documentId").s();
      assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
      assertNotEquals(documentId1, documentId2);
    }
  }

  /**
   * Test Folders structure only.
   */
  @Test
  void testGenerateIndex05() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/B/");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      final int expected = 2;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("ff#a", indexes.get(i).get(SK).s());
      assertEquals("a", indexes.get(i).get("path").s());
      String documentIdA = indexes.get(i).get("documentId").s();
      assertNotNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#" + documentIdA, indexes.get(i).get(PK).s());
      assertEquals("fi#b", indexes.get(i).get(SK).s());
      assertEquals("B", indexes.get(i).get("path").s());
      assertNotNull(indexes.get(i++).get("documentId"));
    }
  }

  /**
   * Test ROOT Folder structure only.
   */
  @Test
  void testGenerateIndex06() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      for (String path : Arrays.asList("/", "")) {

        String documentId = UUID.randomUUID().toString();
        DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
        item.setPath(path);

        // when
        List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

        // then
        assertEquals(0, indexes.size());
      }
    }
  }

  /**
   * Filename starts with '/'.
   */
  @Test
  void testGenerateIndex07() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/test.pdf");

      String site = siteId != null ? siteId + "/" : "";

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      final int expected = 1;
      assertEquals(expected, indexes.size());

      Map<String, AttributeValue> map = indexes.get(0);
      assertFalse(dbService.exists(map.get(PK), map.get(SK)));
      verifyIndex(map, site + "global#folders#", "fi#test.pdf", "test.pdf", false);
    }
  }

  @Test
  void testGetFolderByDocumentId01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/test.pdf");

      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);
      assertEquals(2, indexes.size());
      dbService.putItems(indexes);

      // when
      FolderIndexRecord folder =
          index.getFolderByDocumentId(siteId, indexes.get(0).get("documentId").s());
      FolderIndexRecord file =
          index.getFolderByDocumentId(siteId, indexes.get(1).get("documentId").s());

      // then
      assertNull(file);
      assertNotNull(folder);
      assertEquals("a", folder.path());
      assertEquals("folder", folder.type());
    }
  }

  @Test
  void testGetFoldersByDocumentId01() {
    // given
    final int expected = 4;
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/b/c/test.pdf");

      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);
      assertEquals(expected, indexes.size());
      dbService.putItems(indexes);

      // when
      Collection<FolderIndexRecord> folders =
          index.getFoldersByDocumentId(siteId, indexes.get(2).get("documentId").s());

      // then
      final int expectedThen = 3;
      assertEquals(expectedThen, folders.size());

      String path = folders.stream().map(r -> r.path()).collect(Collectors.joining("/"));
      assertEquals("a/b/c", path);
    }
  }

  /**
   * Move Directory to another directory.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.MINUTES, value = 1)
  public void testMove01() throws Exception {
    // given
    String userId = "fred";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      final String source = "/something/else/";
      final String destination = "/a/b/";

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath(source + "test.txt");
      service.saveDocument(siteId, item, null);

      // when
      index.moveIndex(siteId, source, destination, userId);

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
      SearchQuery q = new SearchQuery().meta(smc);
      PaginationResults<DynamicDocumentItem> results =
          searchService.search(siteId, q, null, MAX_RESULTS);
      List<DynamicDocumentItem> list = results.getResults();
      assertEquals(2, list.size());
      assertEquals("a", list.get(0).get("path"));
      assertEquals("something", list.get(1).get("path"));

      smc.folder("a");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(1, list.size());
      assertEquals("b", list.get(0).get("path"));
      // final String bDocumentId = list.get(0).get("documentId").toString();

      smc.folder("a/b");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(1, list.size());

      // this is probably wrong as the folder structure doesn't equal the document path
      // path is stored on the document also in the folder index. They are stored separately
      // it's possible to update all the documents in the same folder but TBD at a later date.
      assertEquals("/something/else/test.txt", list.get(0).get("path"));

      smc.folder("something");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(0, list.size());

      // List<Message> messages = waitForMessagesFromSqs(sqsQueueUrl);
      // assertEquals(1, messages.size());
      //
      // Map<String, Object> map = this.gson.fromJson(messages.get(0).body(), Map.class);
      // map = this.gson.fromJson(map.get("Message").toString(), Map.class);
      // assertEquals("b", map.get("destinationPath"));
      // assertEquals(bDocumentId, map.get("documentId"));
      // assertTrue(map.get("siteId").equals("default") || map.get("siteId").equals(siteId));
      // assertEquals("else", map.get("sourcePath"));
      // assertEquals("folder_move", map.get("type"));
    }
  }

  /**
   * Move File to new directory.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testMove02() throws Exception {
    // given
    String userId = "fred";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      final String source = "directory1/test.pdf";
      final String destination = "directory2/";

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath(source);
      service.saveDocument(siteId, item, null);

      Map<String, String> sourceAttr = index.getIndex(siteId, source);
      assertEquals("test.pdf", sourceAttr.get("path"));
      final String sourceParentDocumentId = sourceAttr.get("parentDocumentId");

      // when
      index.moveIndex(siteId, source, destination, userId);

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
      SearchQuery q = new SearchQuery().meta(smc);
      PaginationResults<DynamicDocumentItem> results =
          searchService.search(siteId, q, null, MAX_RESULTS);

      assertEquals(2, results.getResults().size());
      assertEquals("directory1", results.getResults().get(0).get("path"));
      assertEquals("directory2", results.getResults().get(1).get("path"));

      smc.folder("directory1");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());

      smc.folder("directory2");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(1, results.getResults().size());
      DynamicDocumentItem doc2 = results.getResults().get(0);
      assertEquals("directory2/test.pdf", doc2.get("path"));
      assertEquals(doc2.get("insertedDate"), doc2.get("lastModifiedDate"));

      Map<String, String> destAttr = index.getIndex(siteId, "directory2/test.pdf");
      assertEquals("test.pdf", destAttr.get("path"));
      assertNotEquals(sourceParentDocumentId, destAttr.get("parentDocumentId"));

      List<Message> messages = getMessagesFromSqs(sqsQueueUrl);
      assertEquals(0, messages.size());
    }
  }

  /**
   * Move File to ROOT, keep original directory.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testMove03() throws Exception {
    // given
    String userId = "fred";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String source0 = "directory1/test.pdf";
      String source1 = "directory1/test2.pdf";

      for (String destination : Arrays.asList("/", "")) {

        String documentId0 = UUID.randomUUID().toString();
        DocumentItem item0 = new DocumentItemDynamoDb(documentId0, new Date(), "joe");
        item0.setPath(source0);
        service.saveDocument(siteId, item0, null);

        String documentId1 = UUID.randomUUID().toString();
        DocumentItem item1 = new DocumentItemDynamoDb(documentId1, new Date(), "joe");
        item1.setPath(source1);
        service.saveDocument(siteId, item1, null);

        // when
        index.moveIndex(siteId, source0, destination, userId);

        // then
        SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
        SearchQuery q = new SearchQuery().meta(smc);
        PaginationResults<DynamicDocumentItem> results =
            searchService.search(siteId, q, null, MAX_RESULTS);

        assertEquals(2, results.getResults().size());
        DynamicDocumentItem doc = results.getResults().get(0);
        assertEquals("directory1", doc.get("path"));
        DynamicDocumentItem dir2 = results.getResults().get(1);
        assertEquals("test.pdf", dir2.get("path"));

        smc.folder("directory1");
        results = searchService.search(siteId, q, null, MAX_RESULTS);

        assertEquals(1, results.getResults().size());
        doc = results.getResults().get(0);
        assertEquals("directory1/test2.pdf", doc.get("path"));

        service.deleteDocument(siteId, item0.getDocumentId(), false);
        service.deleteDocument(siteId, item1.getDocumentId(), false);

        List<Message> messages = getMessagesFromSqs(sqsQueueUrl);
        assertEquals(0, messages.size());
      }
    }
  }

  /**
   * Move File to existing directory.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testMove04() throws Exception {
    // given
    String userId = "fred";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      final String source0 = "d1/test1.pdf";
      final String source1 = "d2/test2.pdf";
      final String destination = "d2/";

      String documentId0 = UUID.randomUUID().toString();
      DocumentItem item0 = new DocumentItemDynamoDb(documentId0, new Date(), "joe");
      item0.setPath(source0);
      service.saveDocument(siteId, item0, null);

      String documentId1 = UUID.randomUUID().toString();
      DocumentItem item1 = new DocumentItemDynamoDb(documentId1, new Date(), "joe");
      item1.setPath(source1);
      service.saveDocument(siteId, item1, null);

      TimeUnit.SECONDS.sleep(1);

      // when
      index.moveIndex(siteId, source0, destination, userId);

      // then
      SearchMetaCriteria smc = new SearchMetaCriteria().folder("");
      SearchQuery q = new SearchQuery().meta(smc);
      PaginationResults<DynamicDocumentItem> results =
          searchService.search(siteId, q, null, MAX_RESULTS);

      assertEquals(2, results.getResults().size());
      assertEquals("d1", results.getResults().get(0).get("path"));
      assertEquals("d2", results.getResults().get(1).get("path"));

      smc.folder("d1");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(0, results.getResults().size());

      smc.folder("d2");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      assertEquals(2, results.getResults().size());
      DynamicDocumentItem doc1 = results.getResults().get(0);
      assertEquals("d2/test1.pdf", doc1.get("path"));
      assertEquals(doc1.get("insertedDate"), doc1.get("lastModifiedDate"));

      DynamicDocumentItem doc2 = results.getResults().get(1);
      assertEquals("d2/test2.pdf", doc2.get("path"));
      assertEquals(doc2.get("insertedDate"), doc2.get("lastModifiedDate"));

      List<FolderIndexRecordExtended> list =
          index.get(siteId, source1, "file", "jsmith", new Date());
      assertEquals(2, list.size());
      assertEquals("file", list.get(1).record().type());
      assertEquals("folder", list.get(0).record().type());
      assertNotEquals(list.get(0).record().lastModifiedDate(), list.get(0).record().insertedDate());

      List<Message> messages = getMessagesFromSqs(sqsQueueUrl);
      assertEquals(0, messages.size());
    }
  }

  private void verifyIndex(final Map<String, AttributeValue> map, final String pk, final String sk,
      final String path, final boolean hasDates) {

    assertEquals(pk, map.get(PK).s());
    assertEquals(sk, map.get(SK).s());
    assertEquals(path, map.get("path").s());
    assertNotNull(map.get("documentId"));

    String parentDocumentId = map.get("parentDocumentId").s();
    assertTrue("".equals(parentDocumentId) || isUuid(parentDocumentId));

    if (hasDates) {
      final int expected = 11;
      assertEquals(expected, map.size());
      assertNotNull(map.get("inserteddate"));
      assertNotNull(map.get("lastModifiedDate"));
      assertNotNull(map.get(GSI1_PK));
      assertNotNull(map.get(GSI1_SK));
      assertEquals("joe", map.get("userId").s());
      assertEquals("folder", map.get("type").s());
    } else {
      final int expected = 6;
      assertEquals(expected, map.size());
      assertNull(map.get("inserteddate"));
      assertNull(map.get("lastModifiedDate"));
      assertNull(map.get("userId"));
      assertEquals("file", map.get("type").s());
    }
  }
}
