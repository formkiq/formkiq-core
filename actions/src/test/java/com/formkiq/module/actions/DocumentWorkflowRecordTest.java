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
package com.formkiq.module.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DocumentWorkflowRecord}.
 */
public class DocumentWorkflowRecordTest {

  private void assertKeyEquals(final String siteId, final String expected, final String actual) {
    assertEquals(siteId != null ? siteId + "/" + expected : expected, actual);
  }

  @Test
  void testBuildKey01() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String workflowId = ID.uuid();
      String workflowName = "workflow";

      DocumentWorkflowRecord record = DocumentWorkflowRecord.builder().documentId(documentId)
          .workflowId(workflowId).workflowName(workflowName).build(siteId);

      DynamoDbKey key = record.key();
      assertKeyEquals(siteId, "docs#" + documentId, key.pk());
      assertEquals("wf#" + workflowId, key.sk());
      assertKeyEquals(siteId, "wfdoc#" + documentId, key.gsi1Pk());
      assertEquals("wf#" + workflowName + "#" + workflowId, key.gsi1Sk());
      assertKeyEquals(siteId, "wf#" + workflowId, key.gsi2Pk());
      assertEquals("wfdoc#" + documentId, key.gsi2Sk());
    }
  }

  @Test
  void testBuildKey02() {

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      String documentId = ID.uuid();
      String artifactId = ID.uuid();
      String workflowId = ID.uuid();
      String workflowName = "workflow";

      DocumentWorkflowRecord record = DocumentWorkflowRecord.builder().documentId(documentId)
          .artifactId(artifactId).workflowId(workflowId).workflowName(workflowName).build(siteId);

      DynamoDbKey key = record.key();
      assertKeyEquals(siteId, "docs#" + documentId, key.pk());
      assertEquals("wf#" + workflowId + "#art#" + artifactId, key.sk());
      assertKeyEquals(siteId, "wfdoc#" + documentId + "#art#" + artifactId, key.gsi1Pk());
      assertEquals("wf#" + workflowName + "#" + workflowId, key.gsi1Sk());
      assertKeyEquals(siteId, "wf#" + workflowId, key.gsi2Pk());
      assertEquals("wfdoc#" + documentId + "_art#" + artifactId, key.gsi2Sk());
    }
  }

  @Test
  void testFromAttributeMap01() {
    String documentId = ID.uuid();
    String artifactId = ID.uuid();
    String workflowId = ID.uuid();
    String workflowName = "workflow";

    DocumentWorkflowRecord record = DocumentWorkflowRecord.builder().documentId(documentId)
        .artifactId(artifactId).workflowId(workflowId).workflowName(workflowName).status("PENDING")
        .actionPk("pk").actionSk("sk").currentStepId("step-1").build((String) null);

    DocumentWorkflowRecord fromMap =
        DocumentWorkflowRecord.fromAttributeMap(record.getAttributes());

    assertEquals(record.key(), fromMap.key());
    assertEquals(record.documentId(), fromMap.documentId());
    assertEquals(record.artifactId(), fromMap.artifactId());
    assertEquals(record.workflowId(), fromMap.workflowId());
    assertEquals(record.workflowName(), fromMap.workflowName());
    assertEquals(record.status(), fromMap.status());
    assertEquals(record.actionPk(), fromMap.actionPk());
    assertEquals(record.actionSk(), fromMap.actionSk());
    assertEquals(record.currentStepId(), fromMap.currentStepId());
  }
}
