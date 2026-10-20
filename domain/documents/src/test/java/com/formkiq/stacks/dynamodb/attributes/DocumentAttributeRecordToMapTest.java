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
package com.formkiq.stacks.dynamodb.attributes;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Tests for document attribute timestamp summaries. */
class DocumentAttributeRecordToMapTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testLegacyTimestamps(final boolean hasInsertionTimestamp) {
    // given
    DocumentAttributeRecord attribute = DocumentAttributeRecord.builder()
        .document(DocumentArtifact.of("document", null)).key("security").stringValue("confidential")
        .insertedDate(
            hasInsertionTimestamp ? Date.from(Instant.parse("2026-10-20T12:00:00Z")) : null)
        .build().updateValueType();

    // when
    Map<String, Object> summary =
        new DocumentAttributeRecordToMap(true).apply(null, List.of(attribute)).iterator().next();

    // then
    if (hasInsertionTimestamp) {
      assertEquals("2026-10-20T12:00:00+0000", summary.get("insertedDate"));
      assertEquals(summary.get("insertedDate"), summary.get("lastModifiedDate"));
    } else {
      assertFalse(summary.containsKey("insertedDate"));
      assertFalse(summary.containsKey("lastModifiedDate"));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testTimestampSummaryIgnoresValueOrder(final boolean reverseOrder) {
    // given
    DocumentArtifact document = DocumentArtifact.of("document", null);
    final Date earliest = Date.from(Instant.parse("2026-10-20T12:00:00Z"));
    final Date middle = Date.from(Instant.parse("2026-10-20T13:00:00Z"));
    final Date latest = Date.from(Instant.parse("2026-10-20T14:00:00Z"));
    DocumentAttributeRecord original = DocumentAttributeRecord.builder().document(document)
        .key("security").stringValue("confidential").insertedDate(earliest).lastModifiedDate(middle)
        .build().updateValueType();
    DocumentAttributeRecord added = DocumentAttributeRecord.builder().document(document)
        .key("security").stringValue("classified").insertedDate(middle).lastModifiedDate(latest)
        .build().updateValueType();
    List<DocumentAttributeRecord> attributes =
        reverseOrder ? List.of(added, original) : List.of(original, added);

    // when
    var summaries = new DocumentAttributeRecordToMap(true).apply(null, attributes);

    // then
    assertEquals(1, summaries.size());
    Map<String, Object> summary = summaries.iterator().next();
    assertEquals("2026-10-20T12:00:00+0000", summary.get("insertedDate"));
    assertEquals("2026-10-20T14:00:00+0000", summary.get("lastModifiedDate"));
    assertEquals(earliest, original.getInsertedDate());
    assertEquals(middle, original.getLastModifiedDate());
    assertEquals(latest, added.getLastModifiedDate());
  }
}
