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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.formkiq.aws.dynamodb.DbKeys;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit Test for FolderIndexRecord.
 */
class FolderIndexRecordTest {

  /**
   * Test Folder Key Folder in root directory.
   */
  @Test
  void testPk01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();

      // when
      FolderIndexRecord record = new FolderIndexRecord().parentDocumentId("").path("test")
          .type("folder").documentId(documentId);

      // then
      assertEquals(createDatabaseKey(siteId, "global#folders#"), record.pk(siteId));
      assertEquals("ff#test", record.sk());
      assertEquals(createDatabaseKey(siteId, "folder#" + documentId), record.pkGsi1(siteId));
      assertEquals("folder", record.skGsi1());
      assertNull(record.pkGsi2(siteId));
      assertNull(record.skGsi2());
    }
  }

  /**
   * Test Folder Key File in root directory.
   */
  @Test
  void testPk02() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();

      // when
      FolderIndexRecord record = new FolderIndexRecord().parentDocumentId("").path("test.txt")
          .type("file").documentId(documentId);

      // then
      assertEquals(createDatabaseKey(siteId, "global#folders#"), record.pk(siteId));
      assertEquals("fi#test.txt", record.sk());
      assertEquals(createDatabaseKey(siteId, "folder#" + documentId), record.pkGsi1(siteId));
      assertEquals("folder", record.skGsi1());
      assertNull(record.pkGsi2(siteId));
      assertNull(record.skGsi2());
    }
  }

  /**
   * Test Legacy Folder Key.
   */
  @Test
  void testGetFromAttributes01() {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String documentId = "b423dec1-b30c-45b7-86ea-19569b475072";

      Map<String, AttributeValue> attrs = Map.of(DbKeys.PK,
          fromS(createDatabaseKey(siteId, "global#folders#5cce7793-fbc5-446f-89a0-af2f2f48cadf")),
          DbKeys.SK, fromS("ff#test-nested"), "documentId", fromS(documentId), "path",
          fromS("test-nested"), "type", fromS("folder"));

      // when
      FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, attrs);

      // then
      assertEquals(createDatabaseKey(siteId, "global#folders#5cce7793-fbc5-446f-89a0-af2f2f48cadf"),
          record.pk(siteId));
      assertEquals("ff#test-nested", record.sk());
      assertEquals(createDatabaseKey(siteId, "folder#" + documentId), record.pkGsi1(siteId));
      assertEquals("folder", record.skGsi1());
      assertNull(record.pkGsi2(siteId));
      assertNull(record.skGsi2());
    }
  }
}
