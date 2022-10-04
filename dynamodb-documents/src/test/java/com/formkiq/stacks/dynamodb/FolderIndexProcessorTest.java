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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Unit Test {@link FolderIndexProcessor}.
 *
 */
class FolderIndexProcessorTest implements DbKeys {

  /** {@link FolderIndexProcessor}. */
  private IndexProcessor index = new FolderIndexProcessor();

  @Test
  void testGenerateIndex01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/b/c/test.pdf");

      // when
      List<Map<String, AttributeValue>> indexes = this.index.generateIndex(siteId, item);

      // then
      final int expected = 4;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("a", indexes.get(i).get(SK).s());
      assertEquals("a", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#a", indexes.get(i).get(PK).s());
      assertEquals("b", indexes.get(i).get(SK).s());
      assertEquals("a/b", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#a/b", indexes.get(i).get(PK).s());
      assertEquals("c", indexes.get(i).get(SK).s());
      assertEquals("a/b/c", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#a/b/c", indexes.get(i).get(PK).s());
      assertEquals("test.pdf#" + documentId, indexes.get(i).get(SK).s());
      assertEquals("a/b/c/test.pdf", indexes.get(i).get("path").s());
      assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
    }
  }

  @Test
  void testGenerateIndex02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/test.pdf");

      // when
      List<Map<String, AttributeValue>> indexes = this.index.generateIndex(siteId, item);

      // then
      final int expected = 1;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("test.pdf#" + item.getDocumentId(), indexes.get(i).get(SK).s());
      assertEquals("test.pdf", indexes.get(i).get("path").s());
      assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
    }
  }

  @Test
  void testGenerateIndex03() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");

      // when
      List<Map<String, AttributeValue>> indexes = this.index.generateIndex(siteId, item);

      // then
      assertTrue(indexes.isEmpty());
    }
  }

  @Test
  void testGenerateIndex04() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("formkiq:://sample/test.txt");

      // when
      List<Map<String, AttributeValue>> indexes = this.index.generateIndex(siteId, item);

      // then
      final int expected = 3;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("formkiq", indexes.get(i).get(SK).s());
      assertEquals("formkiq", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#formkiq", indexes.get(i).get(PK).s());
      assertEquals("sample", indexes.get(i).get(SK).s());
      assertEquals("formkiq/sample", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#formkiq/sample", indexes.get(i).get(PK).s());
      assertEquals("test.txt#" + item.getDocumentId(), indexes.get(i).get(SK).s());
      assertEquals("formkiq/sample/test.txt", indexes.get(i).get("path").s());
      assertEquals(item.getDocumentId(), indexes.get(i++).get("documentId").s());
    }
  }

  /**
   * Test Folders structure only.
   */
  @Test
  void testGenerateIndex05() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String site = siteId != null ? siteId + "/" : "";
      String documentId = UUID.randomUUID().toString();
      DocumentItem item = new DocumentItemDynamoDb(documentId, new Date(), "joe");
      item.setPath("/a/b/");

      // when
      List<Map<String, AttributeValue>> indexes = this.index.generateIndex(siteId, item);

      // then
      final int expected = 2;
      assertEquals(expected, indexes.size());

      int i = 0;
      assertEquals(site + "global#folders#", indexes.get(i).get(PK).s());
      assertEquals("a", indexes.get(i).get(SK).s());
      assertEquals("a", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));

      assertEquals(site + "global#folders#a", indexes.get(i).get(PK).s());
      assertEquals("b", indexes.get(i).get(SK).s());
      assertEquals("a/b", indexes.get(i).get("path").s());
      assertNull(indexes.get(i++).get("documentId"));
    }
  }
}
