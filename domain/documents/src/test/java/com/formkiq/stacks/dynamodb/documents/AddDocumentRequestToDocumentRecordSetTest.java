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
package com.formkiq.stacks.dynamodb.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AddDocumentRequestToDocumentRecordSet}.
 */
public class AddDocumentRequestToDocumentRecordSetTest {

  @Test
  void testApply01() {
    com.formkiq.stacks.dynamodb.documents.AddDocumentRequest request =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentRequest();
    request.setDocumentId("doc01");
    request.setPath("test.txt");
    request.setContentType("text/plain");

    com.formkiq.stacks.dynamodb.documents.AddDocumentTag tag =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentTag();
    tag.setKey("category");
    tag.setValue("invoice");
    request.setTags(List.of(tag));
    com.formkiq.stacks.dynamodb.documents.AddDocumentRequest child =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentRequest();
    child.setDocumentId("doc02");
    child.setContentType("application/json");
    com.formkiq.stacks.dynamodb.documents.AddDocumentTag childTag =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentTag();
    childTag.setKey("child");
    child.setTags(List.of(childTag));
    request.setDocuments(List.of(child));

    AddDocumentRequestToDocumentRecordSet transform =
        new AddDocumentRequestToDocumentRecordSet(new AwsServiceCache(), null, "joe");

    // when
    DocumentRecordSet recordSet = transform.apply("siteId", request);

    // then
    assertEquals("doc01", recordSet.documentRecord().documentId());
    assertEquals("test.txt", recordSet.documentRecord().path());
    assertEquals("text/plain", recordSet.documentRecord().contentType());
    assertEquals("joe", recordSet.documentRecord().userId());
    assertTrue(recordSet.documentAttributeRecords().isEmpty());
    assertEquals(1, recordSet.documentTagRecords().size());
    assertEquals("category", recordSet.documentTagRecords().iterator().next().tagKey());
    assertEquals(1, recordSet.children().size());
    DocumentRecordSet childRecordSet = recordSet.children().iterator().next();
    assertEquals("doc02", childRecordSet.documentRecord().documentId());
    assertEquals("doc01", childRecordSet.documentRecord().belongsToDocumentId());
    assertEquals("application/json", childRecordSet.documentRecord().contentType());
    assertEquals(1, childRecordSet.documentTagRecords().size());
    assertEquals("child", childRecordSet.documentTagRecords().iterator().next().tagKey());
  }

  @Test
  void testApply02() {
    com.formkiq.stacks.dynamodb.documents.AddDocumentRequest request =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentRequest();
    AddDocumentRequestToDocumentRecordSet transform =
        new AddDocumentRequestToDocumentRecordSet(new AwsServiceCache(), null, "joe");

    assertFalse(transform.test(null));
    assertFalse(transform.test(request));
    assertThrows(IllegalArgumentException.class, () -> transform.apply("siteId", request));
  }

  @Test
  void testApplyArtifactCategory01() {
    com.formkiq.stacks.dynamodb.documents.AddDocumentRequest request =
        new com.formkiq.stacks.dynamodb.documents.AddDocumentRequest();
    request.setDocumentId("doc01");
    request.setArtifacts(true);
    request.setArtifactCategory("ocr");

    AddDocumentRequestToDocumentRecordSet transform =
        new AddDocumentRequestToDocumentRecordSet(new AwsServiceCache(), null, "joe");

    DocumentRecordSet recordSet = transform.apply("siteId", request);

    assertTrue(recordSet.documentRecord().artifactId() != null);
    assertEquals("ocr", recordSet.documentRecord().artifactCategory());
  }
}
