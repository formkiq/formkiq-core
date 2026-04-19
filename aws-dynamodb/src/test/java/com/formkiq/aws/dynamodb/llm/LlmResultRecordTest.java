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
package com.formkiq.aws.dynamodb.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unit tests for {@link LlmResultRecord}.
 */
public class LlmResultRecordTest {

  private void assertKeyEquals(final String siteId, final String expected, final String actual) {
    assertEquals(siteId != null ? siteId + "/" + expected : expected, actual);
  }

  @Test
  void testBuildKey01() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      DocumentArtifact document = DocumentArtifact.of(documentId, null);

      DynamoDbKey key =
          LlmResultRecord.builder().document(document).llmPromptEntityName("InvoicePrompt")
              .content("{}").attributes(List.of()).userId("joe").build(siteId).key();

      assertKeyEquals(siteId, "docs#" + documentId, key.pk());
      assertTrue(key.sk().startsWith("llmresult#"));
      assertTrue(key.sk().endsWith("#InvoicePrompt"));
    }
  }

  @Test
  void testBuildKey02() {
    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String artifactId = ID.ulid();
      DocumentArtifact document = DocumentArtifact.of(documentId, artifactId);

      DynamoDbKey key =
          LlmResultRecord.builder().document(document).llmPromptEntityName("InvoicePrompt")
              .content("{}").attributes(List.of()).userId("joe").build(siteId).key();

      assertKeyEquals(siteId, "docs#" + documentId, key.pk());
      assertTrue(key.sk().startsWith("llmresult_art#" + artifactId + "#"));
      assertTrue(key.sk().endsWith("#InvoicePrompt"));
    }
  }

  @Test
  void testFromAttributeMap01() {
    String documentId = ID.uuid();
    DocumentArtifact document = DocumentArtifact.of(documentId, null);
    Date insertedDate = new Date();
    List<Map<String, Object>> attributes = List.of(Map.of("key", "invoiceNumber", "value", "1"));

    LlmResultRecord record = LlmResultRecord.builder().document(document)
        .llmPromptEntityName("InvoicePrompt").content("{\"invoiceNumber\":\"1\"}")
        .insertedDate(insertedDate).attributes(attributes).userId("joe").build((String) null);

    Map<String, AttributeValue> map = record.getAttributes();
    LlmResultRecord fromMap = LlmResultRecord.fromAttributeMap(map);

    assertEquals(record.key(), fromMap.key());
    assertEquals(record.documentId(), fromMap.documentId());
    assertNull(fromMap.artifactId());
    assertEquals(record.llmPromptEntityName(), fromMap.llmPromptEntityName());
    assertEquals(record.content(), fromMap.content());
    assertEquals(record.attributes(), fromMap.attributes());
    assertEquals(record.userId(), fromMap.userId());
    assertEquals(DateUtil.getIsoDateFormatter().format(record.insertedDate()),
        DateUtil.getIsoDateFormatter().format(fromMap.insertedDate()));
  }

  @Test
  void testFromAttributeMap02() {
    String documentId = ID.uuid();
    String artifactId = ID.ulid();
    DocumentArtifact document = DocumentArtifact.of(documentId, artifactId);
    Date insertedDate = new Date();
    List<Map<String, Object>> attributes = List.of(Map.of("key", "invoiceNumber", "value", "1"));

    LlmResultRecord record = LlmResultRecord.builder().document(document)
        .llmPromptEntityName("InvoicePrompt").content("{\"invoiceNumber\":\"1\"}")
        .insertedDate(insertedDate).attributes(attributes).userId("joe").build((String) null);

    Map<String, AttributeValue> map = record.getAttributes();
    LlmResultRecord fromMap = LlmResultRecord.fromAttributeMap(map);

    assertEquals(record.key(), fromMap.key());
    assertEquals(record.documentId(), fromMap.documentId());
    assertEquals(record.artifactId(), fromMap.artifactId());
    assertEquals(record.llmPromptEntityName(), fromMap.llmPromptEntityName());
    assertEquals(record.content(), fromMap.content());
    assertEquals(record.attributes(), fromMap.attributes());
    assertEquals(record.userId(), fromMap.userId());
    assertEquals(DateUtil.getIsoDateFormatter().format(record.insertedDate()),
        DateUtil.getIsoDateFormatter().format(fromMap.insertedDate()));
  }
}
