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
package com.formkiq.aws.dynamodb.documentattributes;

import static com.formkiq.aws.dynamodb.DynamodbRecord.MAX_SORT_KEY_BYTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DocumentAttributeRecord}.
 */
public class DocumentAttributeRecordTest {

  private void assertKeyEquals(final String siteId, final String expected, final String actual) {
    assertEquals(siteId != null ? siteId + "/" + expected : expected, actual);
  }

  /**
   * Verify key construction for a standard document attribute without an artifact id.
   */
  @Test
  void testBuildKey01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String attributeKey = "status";
      String value = "active";

      DocumentAttributeRecord record =
          new DocumentAttributeRecord().setDocument(DocumentArtifact.of(documentId, null))
              .setKey(attributeKey).setStringValue(value).updateValueType();

      assertKeyEquals(siteId, "docs#" + documentId, record.pk(siteId));
      assertEquals("attr#" + attributeKey + "#" + value, record.sk());

      DynamoDbKey key = record.buildKey(siteId);

      assertEquals(record.pk(siteId), key.pk());
      assertEquals(record.sk(), key.sk());
      assertKeyEquals(siteId, "docs#attr#" + attributeKey, key.gsi1Pk());
      assertEquals(value, key.gsi1Sk());
    }
  }

  /**
   * Verify key construction for a document attribute associated with a document artifact.
   */
  @Test
  void testBuildKey02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String artifactId = ID.ulid();
      String attributeKey = "category";
      String value = "invoice";

      DocumentAttributeRecord record =
          new DocumentAttributeRecord().setDocument(DocumentArtifact.of(documentId, artifactId))
              .setKey(attributeKey).setStringValue(value).updateValueType();

      assertKeyEquals(siteId, "docs#" + documentId, record.pk(siteId));
      assertEquals("attr_art#" + artifactId + "#" + attributeKey + "#" + value, record.sk());

      DynamoDbKey key = record.buildKey(siteId);

      assertEquals(record.pk(siteId), key.pk());
      assertEquals(record.sk(), key.sk());
      assertKeyEquals(siteId, "docs#attr#" + attributeKey, key.gsi1Pk());
      assertEquals(value, key.gsi1Sk());
    }
  }

  /**
   * Verify multi-byte string attribute values are truncated to valid DynamoDB key byte limits.
   */
  @Test
  void testBuildKey03() {
    String documentId = ID.uuid();
    String attributeKey = "ocrOutputs";
    String value = "中".repeat(337);

    DocumentAttributeRecord record =
        new DocumentAttributeRecord().setDocument(DocumentArtifact.of(documentId, null))
            .setKey(attributeKey).setStringValue(value).updateValueType();

    int skBytes = record.sk().getBytes(StandardCharsets.UTF_8).length;
    int gsi1SkBytes = record.skGsi1().getBytes(StandardCharsets.UTF_8).length;
    assertTrue(skBytes <= MAX_SORT_KEY_BYTES,
        "DynamoDB sort key exceeds " + MAX_SORT_KEY_BYTES + " bytes: " + skBytes);
    assertTrue(gsi1SkBytes <= MAX_SORT_KEY_BYTES,
        "DynamoDB GSI1 sort key exceeds " + MAX_SORT_KEY_BYTES + " bytes: " + gsi1SkBytes);
  }

  /**
   * Verify very long ASCII string attribute values are truncated to DynamoDB key byte limits.
   */
  @Test
  void testBuildKey04() {
    String documentId = ID.uuid();
    String attributeKey = "ocrOutputs";
    String value = "a".repeat(50000);

    DocumentAttributeRecord record =
        new DocumentAttributeRecord().setDocument(DocumentArtifact.of(documentId, null))
            .setKey(attributeKey).setStringValue(value).updateValueType();

    int skBytes = record.sk().getBytes(StandardCharsets.UTF_8).length;
    int gsi1SkBytes = record.skGsi1().getBytes(StandardCharsets.UTF_8).length;
    assertTrue(skBytes <= MAX_SORT_KEY_BYTES,
        "DynamoDB sort key exceeds " + MAX_SORT_KEY_BYTES + " bytes: " + skBytes);
    assertTrue(gsi1SkBytes <= MAX_SORT_KEY_BYTES,
        "DynamoDB GSI1 sort key exceeds " + MAX_SORT_KEY_BYTES + " bytes: " + gsi1SkBytes);
  }
}
