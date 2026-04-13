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
package com.formkiq.module.actions.workflows;

import com.formkiq.aws.dynamodb.WriteRequestAppender;
import com.formkiq.aws.dynamodb.WriteRequestBuilder;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.DocumentWorkflowRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * {@link WriteRequestAppender} implementation that updates the workflow status of a document.
 *
 * <p>
 * This class encapsulates the logic required to compute the effective ActionStatus for a workflow
 * step and append the corresponding DynamoDB update operation to a {@link WriteRequestBuilder}.
 *
 * <p>
 * The update targets the DocumentWorkflowRecord associated with the document and workflow. The
 * record attributes are constructed using DocumentWorkflowRecord.Builder and then appended to the
 * write request builder as an update operation.
 *
 * <p>
 * Status resolution follows these rules:
 *
 * <ul>
 * <li>If the requested status is {@link ActionStatus#FAILED}, the workflow remains in the failed
 * state.</li>
 * <li>If the action represents the final workflow step, the workflow status becomes
 * {@link ActionStatus#COMPLETE}.</li>
 * <li>Otherwise the workflow remains {@link ActionStatus#IN_PROGRESS}.</li>
 * </ul>
 *
 * <p>
 * This abstraction allows workflow updates to be represented as reusable write operations that can
 * be composed into a {@link WriteRequestBuilder} without embedding DynamoDB update logic inside
 * service methods.
 */
public class DocumentWorkflowStatusUpdate implements WriteRequestAppender {

  /** Attributes to update. */
  private final Map<String, AttributeValue> attributes;
  /** Dynamodb Table Name. */
  private final String dynamodbTableName;

  /**
   * constructor.
   * 
   * @param tableName {@link String}
   * @param siteId {@link String}
   * @param document {@link DocumentArtifact}
   * @param action {@link Action}
   * @param newStatus {@link ActionStatus}
   */
  public DocumentWorkflowStatusUpdate(final String tableName, final String siteId,
      final DocumentArtifact document, final Action action, final ActionStatus newStatus) {
    this.dynamodbTableName = tableName;
    attributes = updateDocumentWorkflow(siteId, document, action, newStatus);
  }

  @Override
  public void appendTo(final WriteRequestBuilder wrb) {
    if (attributes != null) {
      wrb.appendUpdate(dynamodbTableName, attributes);
    }
  }

  private Map<String, AttributeValue> updateDocumentWorkflow(final String siteId,
      final DocumentArtifact document, final Action action, final ActionStatus newStatus) {

    if (!isEmpty(action.workflowId()) && !isEmpty(action.workflowStepId())) {
      return updateDocumentWorkflowStatus(siteId, document, action, newStatus);
    }

    return null;
  }

  private Map<String, AttributeValue> updateDocumentWorkflowStatus(final String siteId,
      final DocumentArtifact document, final Action action, final ActionStatus newStatus) {

    String workflowId = action.workflowId();
    String stepId = action.workflowStepId();

    DocumentWorkflowRecord r = DocumentWorkflowRecord.builder().documentId(document.documentId())
        .workflowName("").workflowId(workflowId).build(siteId);

    DocumentWorkflowRecord.Builder dwr =
        DocumentWorkflowRecord.builder().documentId(document.documentId()).workflowId(workflowId)
            .currentStepId(stepId).actionPk(action.key().pk()).actionSk(action.key().sk());

    dwr.status(newStatus.name());
    if (!ActionStatus.FAILED.equals(newStatus)) {
      ActionStatus status =
          !isEmpty(action.workflowLastStep()) ? ActionStatus.COMPLETE : ActionStatus.IN_PROGRESS;
      dwr.status(status.name());
    }

    return dwr.build(r.key()).getAttributes();
  }
}
