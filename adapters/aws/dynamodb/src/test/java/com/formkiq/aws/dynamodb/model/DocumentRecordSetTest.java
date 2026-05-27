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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DocumentRecordSet}.
 */
public class DocumentRecordSetTest {

  @Test
  void testBuilder01() {
    DocumentRecord documentRecord =
        DocumentRecord.builder().documentId("doc01").build((String) null);
    DocumentAttributeRecord attributeRecord = new DocumentAttributeRecord();
    DocumentTagRecord tagRecord = DocumentTagRecord.builder().documentId("doc01").tagKey("category")
        .tagValue("invoice").insertedDate(new Date()).build((String) null).get(0);

    // when
    DocumentRecordSet wrapper = DocumentRecordSet.builder().documentRecord(documentRecord)
        .documentAttributeRecords(List.of(attributeRecord)).documentTagRecords(List.of(tagRecord))
        .build();

    // then
    assertSame(documentRecord, wrapper.documentRecord());
    assertEquals(List.of(attributeRecord), wrapper.documentAttributeRecords());
    assertEquals(List.of(tagRecord), wrapper.documentTagRecords());
    assertTrue(wrapper.children().isEmpty());
  }

  @Test
  void testBuilder02() {
    DocumentRecord documentRecord =
        DocumentRecord.builder().documentId("doc01").build((String) null);

    // when
    DocumentRecordSet wrapper = DocumentRecordSet.builder().documentRecord(documentRecord).build();

    // then
    assertTrue(wrapper.documentAttributeRecords().isEmpty());
    assertTrue(wrapper.documentTagRecords().isEmpty());
    assertTrue(wrapper.children().isEmpty());
  }

  @Test
  void testBuilder03() {
    DocumentRecord documentRecord =
        DocumentRecord.builder().documentId("doc01").build((String) null);
    DocumentRecord childDocumentRecord =
        DocumentRecord.builder().documentId("doc02").build((String) null);
    List<DocumentAttributeRecord> attributeRecords = new ArrayList<>();
    List<DocumentTagRecord> tagRecords = new ArrayList<>();
    List<DocumentRecordSet> children = new ArrayList<>(
        List.of(DocumentRecordSet.builder().documentRecord(childDocumentRecord).build()));

    // when
    final DocumentRecordSet wrapper = DocumentRecordSet.builder().documentRecord(documentRecord)
        .documentAttributeRecords(attributeRecords).documentTagRecords(tagRecords)
        .children(children).build();
    attributeRecords.add(new DocumentAttributeRecord());
    tagRecords.add(DocumentTagRecord.builder().documentId("doc01").tagKey("category")
        .tagValue("invoice").insertedDate(new Date()).build((String) null).get(0));
    children.clear();

    // then
    assertTrue(wrapper.documentAttributeRecords().isEmpty());
    assertTrue(wrapper.documentTagRecords().isEmpty());
    assertEquals(1, wrapper.children().size());
    assertThrows(UnsupportedOperationException.class,
        () -> wrapper.documentAttributeRecords().add(new DocumentAttributeRecord()));
    assertThrows(UnsupportedOperationException.class, () -> wrapper.children()
        .add(DocumentRecordSet.builder().documentRecord(childDocumentRecord).build()));
  }

  @Test
  void testBuilder04() {
    assertThrows(NullPointerException.class, () -> DocumentRecordSet.builder().build());
  }
}
