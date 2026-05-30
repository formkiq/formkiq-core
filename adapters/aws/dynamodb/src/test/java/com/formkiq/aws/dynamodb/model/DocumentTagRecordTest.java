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
package com.formkiq.aws.dynamodb.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit tests for {@link DocumentTagRecord}.
 */
public class DocumentTagRecordTest {

  private void assertKeyEquals(final String siteId, final String expected, final String actual) {
    assertEquals(siteId != null ? siteId + "/" + expected : expected, actual);
  }

  @Test
  void testBuildKey01() {
    Date insertedDate = new Date();
    String fullDate = DateUtil.getIsoDateFormatter().format(insertedDate);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String tagKey = "category";
      String tagValue = "invoice";

      List<DocumentTagRecord> records = DocumentTagRecord.builder().documentId(documentId)
          .tagKey(tagKey).tagValue(tagValue).insertedDate(insertedDate).userId("joe")
          .type(DocumentTagType.USERDEFINED).build(siteId);
      assertEquals(1, records.size());

      DynamoDbKey key = records.get(0).key();

      assertKeyEquals(siteId, "docs#" + documentId, key.pk());
      assertEquals("tags#" + tagKey, key.sk());
      assertKeyEquals(siteId, "tag#" + tagKey + "#" + tagValue, key.gsi1Pk());
      assertEquals(fullDate + "#" + documentId, key.gsi1Sk());
      assertKeyEquals(siteId, "tag#" + tagKey, key.gsi2Pk());
      assertEquals(tagValue, key.gsi2Sk());
    }
  }

  @Test
  void testBuildKey02() {
    Date insertedDate = new Date();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String tagKey = "status";
      String tagValue = "approved";
      List<String> tagValues = List.of("pending", "approved");

      // when
      List<DocumentTagRecord> records = DocumentTagRecord.builder().documentId(documentId)
          .tagKey(tagKey).tagValue(tagValue).tagValues(tagValues).insertedDate(insertedDate)
          .userId("system").type(DocumentTagType.SYSTEMDEFINED).build(siteId);

      // then
      assertEquals(2, records.size());
      DocumentTagRecord record = records.get(0);
      assertKeyEquals(siteId, "docs#" + documentId, record.key().pk());
      assertEquals("tags#" + tagKey, record.key().sk());
      assertIterableEquals(tagValues, record.tagValues());

      record = records.get(1);
      assertKeyEquals(siteId, "docs#" + documentId, record.key().pk());
      assertEquals("tags#" + tagKey + "#idx1", record.key().sk());
      assertIterableEquals(tagValues, record.tagValues());
    }
  }

  @Test
  void testBuildKey03() {
    Date insertedDate = new Date();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String artifactId = ID.ulid();
      String tagKey = "category";
      String tagValue = "invoice";

      List<DocumentTagRecord> records =
          DocumentTagRecord.builder().documentId(documentId).artifactId(artifactId).tagKey(tagKey)
              .tagValue(tagValue).insertedDate(insertedDate).build(siteId);

      assertEquals(1, records.size());
      DocumentTagRecord record = records.get(0);
      assertKeyEquals(siteId, "docs#" + documentId, record.key().pk());
      assertEquals("tags_art#" + artifactId + "#" + tagKey, record.key().sk());
      assertEquals(artifactId, record.artifactId());
    }
  }

  @Test
  void testBuildKey04() {
    Date insertedDate = new Date();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String artifactId = ID.ulid();
      String tagKey = "status";

      List<DocumentTagRecord> records =
          DocumentTagRecord.builder().documentId(documentId).artifactId(artifactId).tagKey(tagKey)
              .tagValue("approved").insertedDate(insertedDate).build(siteId);

      assertEquals(1, records.size());
      DocumentTagRecord record = records.get(0);
      assertKeyEquals(siteId, "docs#" + documentId, record.key().pk());
      assertEquals("tags_art#" + artifactId + "#" + tagKey, record.key().sk());
      assertEquals(artifactId, record.artifactId());
    }
  }

  /**
   * Build Key for single tagValues.
   */
  @Test
  void testBuildKey05() {
    Date insertedDate = new Date();

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String tagKey = "status";
      String tagValue = "approved";
      List<String> tagValues = List.of("pending");

      // when
      List<DocumentTagRecord> records = DocumentTagRecord.builder().documentId(documentId)
          .tagKey(tagKey).tagValue(tagValue).tagValues(tagValues).insertedDate(insertedDate)
          .userId("system").type(DocumentTagType.SYSTEMDEFINED).build(siteId);

      // then
      assertEquals(1, records.size());
      DocumentTagRecord record = records.get(0);
      assertKeyEquals(siteId, "docs#" + documentId, record.key().pk());
      assertEquals("tags#" + tagKey, record.key().sk());
      assertIterableEquals(tagValues, record.tagValues());
    }
  }

  @Test
  void testFromAttributeMap01() {
    // given
    String documentId = ID.uuid();
    Date insertedDate = new Date();

    DocumentTag tag = new DocumentTag(documentId, "department", List.of("finance", "ops"),
        insertedDate, "user1", DocumentTagType.COMPOSITE);

    // when
    List<DocumentTagRecord> records =
        DocumentTagRecord.builder().tag(tag).tagValue("finance").build((String) null);

    // then
    assertEquals(2, records.size());

    // given
    DocumentTagRecord record = records.get(0);
    Map<String, AttributeValue> attributes = record.getAttributes();

    // when
    DocumentTagRecord fromMap = DocumentTagRecord.fromAttributeMap(attributes);

    // then
    assertEquals(record.key(), fromMap.key());
    assertEquals(record.documentId(), fromMap.documentId());
    assertEquals(record.artifactId(), fromMap.artifactId());
    assertEquals(record.tagKey(), fromMap.tagKey());
    assertEquals(record.tagValue(), fromMap.tagValue());
    assertIterableEquals(record.tagValues(), fromMap.tagValues());
    assertEquals(record.userId(), fromMap.userId());
    assertEquals(record.type(), fromMap.type());
    assertEquals(DateUtil.getIsoDateFormatter().format(record.insertedDate()),
        DateUtil.getIsoDateFormatter().format(fromMap.insertedDate()));
  }
}
