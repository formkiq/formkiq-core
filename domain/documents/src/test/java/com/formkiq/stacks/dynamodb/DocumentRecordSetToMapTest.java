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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.documents.DocumentMetadata;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;

/** Unit tests for {@link DocumentRecordSetToMap}. */
class DocumentRecordSetToMapTest {

  @Test
  @SuppressWarnings("unchecked")
  void testApply01() {
    DocumentMetadata metadata = new DocumentMetadata("category", "person", null);
    DocumentRecord document = DocumentRecord.builder().documentId("document1").userId("jsmith")
        .metadata(List.of(metadata)).s3version("version1").build((String) null);
    DocumentRecord child = DocumentRecord.builder().documentId("document2")
        .belongsToDocumentId("document1").userId("jsmith").build((String) null);
    DocumentRecordSet recordSet = new DocumentRecordSet(document, null, null,
        List.of(new DocumentRecordSet(child, null, null, null)));

    Map<String, Object> result = new DocumentRecordSetToMap().apply(recordSet);

    assertEquals("document1", result.get("documentId"));
    assertEquals(List.of(metadata), result.get("metadata"));
    assertFalse(result.containsKey("category"));
    assertFalse(result.containsKey("s3version"));

    List<Map<String, Object>> children = (List<Map<String, Object>>) result.get("documents");
    assertEquals(1, children.size());
    assertEquals("document2", children.getFirst().get("documentId"));
    assertEquals("document1", children.getFirst().get("belongsToDocumentId"));
  }

  @Test
  void testApplyDeepLink01() {
    DocumentRecord document = DocumentRecord.builder().documentId("document1")
        .deepLinkPath("https://example.com").contentLength(1L).build((String) null);
    DocumentRecordSet recordSet = new DocumentRecordSet(document, null, null, null);

    Map<String, Object> result = new DocumentRecordSetToMap().apply(recordSet);

    assertFalse(result.containsKey("contentLength"));
    assertFalse(result.containsKey("lastModifiedDate"));
  }
}
