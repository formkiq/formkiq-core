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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Unit Test {@link FolderIndexProcessorImpl}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class FolderIndexProcessorTest implements DbKeys {

  /** {@link DynamoDbService}. */
  private static DynamoDbService dbService;
  /** {@link FolderIndexProcessorImpl}. */
  private static FolderIndexProcessorImpl index;
  /** {@link DocumentService}. */
  private static DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private static DocumentService service;

  @BeforeAll
  static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection();
    index = new FolderIndexProcessorImpl(dynamoDbConnection, DOCUMENTS_TABLE);

    service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());
    searchService =
        new DocumentSearchServiceImpl(dynamoDbConnection, service, DOCUMENTS_TABLE, null);
    dbService = new DynamoDbServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
  }

  /**
   * Test Create all new directories.
   */
  @Test
  void testGenerateIndex01() throws Exception {
    // given
    final int loop = 3;

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/b/c/test.pdf");

      String documentIdA = null;
      String documentIdB = null;
      String documentIdC = null;

      for (int j = 0; j < loop; j++) {

        // when
        List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

        // then
        final int expected = 4;
        assertEquals(expected, indexes.size());

        int i = 0;
        Map<String, AttributeValue> map = indexes.get(i++);
        assertTrue(dbService.exists(map.get(PK), map.get(SK)));

        verifyIndex(map, map.get(PK).s(), "ff#a", "a", true);
        documentIdA = documentIdA != null ? documentIdA : map.get("documentId").s();

        map = indexes.get(i++);
        assertTrue(dbService.exists(map.get(PK), map.get(SK)));
        verifyIndex(map, site + "global#folders#" + documentIdA, "ff#b", "b", true);
        documentIdB = documentIdB != null ? documentIdB : map.get("documentId").s();

        map = indexes.get(i++);
        assertTrue(dbService.exists(map.get(PK), map.get(SK)));
        verifyIndex(map, site + "global#folders#" + documentIdB, "ff#c", "c", true);
        documentIdC = documentIdC != null ? documentIdC : map.get("documentId").s();

        map = indexes.get(i++);
        assertFalse(dbService.exists(map.get(PK), map.get(SK)));
        verifyIndex(map, site + "global#folders#" + documentIdC, "fi#test.pdf", "test.pdf", false);
      }
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

  @Test
  void testGenerateIndex03() throws Exception {
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
      assertEquals("ff#b", indexes.get(i).get(SK).s());
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

  /**
   * Move Directory to another directory.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testMove01() throws Exception {
    // given
    String userId = "fred";
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String source = "/something/else/";
      String destination = "/a/b/";

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath(source + "/test.txt");
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

      smc.folder("a/b");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(1, list.size());
      assertEquals("else", list.get(0).get("path"));

      smc.folder("something");
      results = searchService.search(siteId, q, null, MAX_RESULTS);
      list = results.getResults();
      assertEquals(0, list.size());
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

      String source = "directory1/test.pdf";
      String destination = "directory2/";

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath(source);
      service.saveDocument(siteId, item, null);

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

        service.deleteDocument(siteId, item0.getDocumentId());
        service.deleteDocument(siteId, item1.getDocumentId());
      }
    }
  }

  private void verifyIndex(final Map<String, AttributeValue> map, final String pk, final String sk,
      final String path, final boolean hasDates) {

    assertEquals(pk, map.get(PK).s());
    assertEquals(sk, map.get(SK).s());
    assertEquals(path, map.get("path").s());
    assertNotNull(map.get("documentId"));

    if (hasDates) {
      assertNotNull(map.get("inserteddate"));
      assertNotNull(map.get("lastModifiedDate"));
      assertEquals("joe", map.get("userId").s());
    } else {
      assertNull(map.get("inserteddate"));
      assertNull(map.get("lastModifiedDate"));
      assertNull(map.get("userId"));
    }
  }
}
