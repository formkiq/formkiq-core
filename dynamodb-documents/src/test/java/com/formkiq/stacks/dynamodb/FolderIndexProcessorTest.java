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
 * Unit Test {@link FolderIndexProcessor}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class FolderIndexProcessorTest implements DbKeys {

  /** {@link FolderIndexProcessor}. */
  private static FolderIndexProcessor index;
  /** {@link DocumentService}. */
  private static DocumentSearchService searchService;
  /** {@link DocumentService}. */
  private static DocumentService service;

  @BeforeAll
  static void beforeAll() throws URISyntaxException {
    DynamoDbConnectionBuilder dynamoDbConnection = DynamoDbTestServices.getDynamoDbConnection(null);
    index = new FolderIndexProcessor(dynamoDbConnection, DOCUMENTS_TABLE);

    service = new DocumentServiceImpl(dynamoDbConnection, DOCUMENTS_TABLE);
    searchService =
        new DocumentSearchServiceImpl(dynamoDbConnection, service, DOCUMENTS_TABLE, null);
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

        if (j < 2) {
          index.clearCache();
        }

        // when
        List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

        // then
        final int expected = 4;
        assertEquals(expected, indexes.size());

        int i = 0;
        assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
        assertEquals("f#a", indexes.get(i).get(SK).s());
        assertEquals("a", indexes.get(i).get("path").s());
        assertNotNull(indexes.get(i).get("documentId"));
        assertNotNull(indexes.get(i).get("inserteddate"));
        assertNotNull(indexes.get(i).get("lastModifiedDate"));
        documentIdA = documentIdA != null ? documentIdA : indexes.get(i).get("documentId").s();
        assertEquals("joe", indexes.get(i++).get("userId").s());

        assertEquals(site + "global#folders#" + documentIdA, indexes.get(i).get(PK).s());
        assertEquals("f#b", indexes.get(i).get(SK).s());
        assertEquals("b", indexes.get(i).get("path").s());
        assertNotNull(indexes.get(i).get("inserteddate"));
        assertNotNull(indexes.get(i).get("lastModifiedDate"));
        documentIdB = documentIdB != null ? documentIdB : indexes.get(i).get("documentId").s();
        assertEquals("joe", indexes.get(i++).get("userId").s());

        assertEquals(site + "global#folders#" + documentIdB, indexes.get(i).get(PK).s());
        assertEquals("f#c", indexes.get(i).get(SK).s());
        assertEquals("c", indexes.get(i).get("path").s());
        assertNotNull(indexes.get(i).get("inserteddate"));
        assertNotNull(indexes.get(i).get("lastModifiedDate"));
        documentIdC = documentIdC != null ? documentIdC : indexes.get(i).get("documentId").s();
        assertEquals("joe", indexes.get(i++).get("userId").s());

        assertEquals(site + "global#folders#" + documentIdC, indexes.get(i).get(PK).s());
        assertEquals("f#test.pdf", indexes.get(i).get(SK).s());
        assertEquals("test.pdf", indexes.get(i).get("path").s());
        assertNull(indexes.get(i).get("inserteddate"));
        assertNull(indexes.get(i).get("lastModifiedDate"));
        assertNull(indexes.get(i).get("userId"));
        assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
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
      assertEquals("f#test.pdf", indexes.get(i).get(SK).s());
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
      assertEquals("f#formkiq", indexes.get(i).get(SK).s());
      assertEquals("formkiq", indexes.get(i).get("path").s());
      String documentId0 = indexes.get(i++).get("documentId").s();

      assertEquals(site + "global#folders#" + documentId0, indexes.get(i).get(PK).s());
      assertEquals("f#sample", indexes.get(i).get(SK).s());
      assertEquals("sample", indexes.get(i).get("path").s());
      String documentId1 = indexes.get(i++).get("documentId").s();
      assertNotEquals(documentId0, documentId1);

      assertEquals(site + "global#folders#" + documentId1, indexes.get(i).get(PK).s());
      assertEquals("f#test.txt", indexes.get(i).get(SK).s());
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
      item.setPath("/a/b/");

      // when
      List<Map<String, AttributeValue>> indexes = index.generateIndex(siteId, item);

      // then
      final int expected = 2;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("f#a", indexes.get(i).get(SK).s());
      assertEquals("a", indexes.get(i).get("path").s());
      String documentIdA = indexes.get(i).get("documentId").s();
      assertNotNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#" + documentIdA, indexes.get(i).get(PK).s());
      assertEquals("f#b", indexes.get(i).get(SK).s());
      assertEquals("b", indexes.get(i).get("path").s());
      assertNotNull(indexes.get(i++).get("documentId"));
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
      DynamicDocumentItem doc = results.getResults().get(0);
      assertEquals("directory1", doc.get("path"));
      DynamicDocumentItem dir2 = results.getResults().get(1);
      assertEquals("directory2", dir2.get("path"));

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
}
