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
package com.formkiq.aws.dynamodb.entity;

import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.DISPOSITION_DATE_IN_DAYS;
import static com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved.RETENTION_START_DATE_SOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit tests for {@link DispositionDateAttribute}.
 */
class DispositionDateAttributeTest {

  @Test
  void calculateUsesInsertedDateWithUtcTimestamp() {
    DocumentRecord document = documentRecord();
    EntityRecord entityRecord = entityRecord(Map.of(DISPOSITION_DATE_IN_DAYS.getKey(), fromN("2"),
        RETENTION_START_DATE_SOURCE_TYPE.getKey(),
        fromS(RetentionStartDateSourceType.DATE_INSERTED.name())));

    String value = new DispositionDateAttribute().calculate(entityRecord, document);

    assertEquals("2025-01-03T00:00:00+0000", value);
  }

  @Test
  void calculateUsesLastModifiedDateWithUtcTimestamp() {
    DocumentRecord document = documentRecord();
    EntityRecord entityRecord = entityRecord(Map.of(DISPOSITION_DATE_IN_DAYS.getKey(), fromN("3"),
        RETENTION_START_DATE_SOURCE_TYPE.getKey(),
        fromS(RetentionStartDateSourceType.DATE_LAST_MODIFIED.name())));

    String value = new DispositionDateAttribute().calculate(entityRecord, document);

    assertEquals("2025-02-04T00:00:00+0000", value);
  }

  private DocumentRecord documentRecord() {
    return new DocumentRecordBuilder().documentId("documentId")
        .insertedDate(Date.from(Instant.parse("2025-01-01T23:30:45Z")))
        .lastModifiedDate(Date.from(Instant.parse("2025-02-01T04:05:06Z"))).build((String) null);
  }

  private EntityRecord entityRecord(final Map<String, AttributeValue> attributes) {
    return new EntityRecord(
        DynamoDbKey.builder().pk(null, "entity#RetentionPolicy#documentId").sk("entity").build(),
        "RetentionPolicy", "documentId", "RetentionPolicy",
        Date.from(Instant.parse("2025-01-01T00:00:00Z")), attributes);
  }
}
