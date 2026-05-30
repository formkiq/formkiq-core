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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.validation.ValidationChecks;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a Document Workflow relationship, with its DynamoDB key structure and
 * metadata.
 */
public record DocumentWorkflowRecord(DynamoDbKey key, String documentId, String artifactId,
    String workflowId, String workflowName, String status, String actionPk, String actionSk,
    String currentStepId, Date insertedDate) {

  /** DynamoDB attribute name for artifact id. */
  private static final String ATTR_ARTIFACT_ID = "artifactId";
  /** DynamoDB attribute name for document id. */
  private static final String ATTR_DOCUMENT_ID = "documentId";
  /** DynamoDB attribute name for workflow id. */
  private static final String ATTR_WORKFLOW_ID = "workflowId";
  /** DynamoDB attribute name for workflow name. */
  private static final String ATTR_WORKFLOW_NAME = "workflowName";
  /** DynamoDB attribute name for status. */
  private static final String ATTR_STATUS = "status";
  /** DynamoDB attribute name for action PK. */
  private static final String ATTR_ACTION_PK = "actionPk";
  /** DynamoDB attribute name for action SK. */
  private static final String ATTR_ACTION_SK = "actionSk";
  /** DynamoDB attribute name for current step id. */
  private static final String ATTR_CURRENT_STEP_ID = "currentStepId";
  /** DynamoDB attribute name for inserted date. */
  private static final String ATTR_INSERTED_DATE = "inserteddate";

  /**
   * Canonical constructor to enforce non-null properties and defensive copy of Date.
   */
  public DocumentWorkflowRecord {
    Objects.requireNonNull(key, "key must not be null");
    if (insertedDate != null) {
      insertedDate = new Date(insertedDate.getTime());
    }
  }

  /**
   * Constructs a {@code DocumentWorkflowRecord} from a map of DynamoDB attributes.
   *
   * @param attributes the map of attribute names to {@link AttributeValue}
   * @return a new {@code DocumentWorkflowRecord} instance, or {@code null} if {@code attributes} is
   *         null or empty
   */
  public static DocumentWorkflowRecord fromAttributeMap(
      final Map<String, AttributeValue> attributes) {

    if (attributes != null && !attributes.isEmpty()) {
      DynamoDbKey key = DynamoDbKey.fromAttributeMap(attributes);

      String documentId = DynamoDbTypes.toString(attributes.get(ATTR_DOCUMENT_ID));
      String artifactId = DynamoDbTypes.toString(attributes.get(ATTR_ARTIFACT_ID));
      String workflowId = DynamoDbTypes.toString(attributes.get(ATTR_WORKFLOW_ID));
      String workflowName = DynamoDbTypes.toString(attributes.get(ATTR_WORKFLOW_NAME));
      String status = DynamoDbTypes.toString(attributes.get(ATTR_STATUS));
      String actionPk = DynamoDbTypes.toString(attributes.get(ATTR_ACTION_PK));
      String actionSk = DynamoDbTypes.toString(attributes.get(ATTR_ACTION_SK));
      String currentStepId = DynamoDbTypes.toString(attributes.get(ATTR_CURRENT_STEP_ID));
      Date insertedDate = DynamoDbTypes.toDate(attributes.get(ATTR_INSERTED_DATE));

      return new DocumentWorkflowRecord(key, documentId, artifactId, workflowId, workflowName,
          status, actionPk, actionSk, currentStepId, insertedDate);
    }

    return null;
  }

  /**
   * Builds the DynamoDB item attribute map for this document-workflow record, starting from the key
   * attributes and adding metadata fields.
   *
   * @return a map of attribute names to {@link AttributeValue} instances
   */
  public Map<String, AttributeValue> getAttributes() {
    return key.getAttributesBuilder().withString(ATTR_DOCUMENT_ID, documentId)
        .withString(ATTR_ARTIFACT_ID, artifactId).withString(ATTR_WORKFLOW_ID, workflowId)
        .withString(ATTR_WORKFLOW_NAME, workflowName).withString(ATTR_STATUS, status)
        .withString(ATTR_ACTION_PK, actionPk).withString(ATTR_ACTION_SK, actionSk)
        .withString(ATTR_CURRENT_STEP_ID, currentStepId).withDate(ATTR_INSERTED_DATE, insertedDate)
        .build();
  }

  /**
   * Creates a new {@link Builder} for {@link DocumentWorkflowRecord}.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for {@link DocumentWorkflowRecord} that computes the {@link DynamoDbKey}.
   */
  public static class Builder implements DynamoDbEntityBuilder<DocumentWorkflowRecord> {

    /** DocumentId. */
    private String documentId;
    /** ArtifactId. */
    private String artifactId;
    /** WorkflowId. */
    private String workflowId;
    /** WorkflowName. */
    private String workflowName;
    /** Status. */
    private String status;
    /** Action PK. */
    private String actionPk;
    /** Action SK. */
    private String actionSk;
    /** Current Step Id. */
    private String currentStepId;
    /** Inserted Date. */
    private Date insertedDate = new Date();

    /**
     * Sets the action partition key.
     *
     * @param pk the action PK
     * @return this {@link Builder}
     */
    public Builder actionPk(final String pk) {
      this.actionPk = pk;
      return this;
    }

    /**
     * Sets the action sort key.
     *
     * @param sk the action SK
     * @return this {@link Builder}
     */
    public Builder actionSk(final String sk) {
      this.actionSk = sk;
      return this;
    }

    /**
     * Sets the artifact id.
     *
     * @param id the artifact id
     * @return this {@link Builder}
     */
    public Builder artifactId(final String id) {
      this.artifactId = id;
      return this;
    }

    @Override
    public DocumentWorkflowRecord build(final DynamoDbKey key) {
      return new DocumentWorkflowRecord(key, documentId, artifactId, workflowId, workflowName,
          status, actionPk, actionSk, currentStepId, insertedDate);
    }

    @Override
    public DocumentWorkflowRecord build(final String siteId) {
      DynamoDbKey key = buildKey(siteId);
      return build(key);
    }

    /**
     * Builds the {@link DynamoDbKey} for this document-workflow record.
     *
     * @param siteId the site identifier
     * @return a {@link DynamoDbKey}
     */
    @Override
    public DynamoDbKey buildKey(final String siteId) {
      ValidationChecks.checkNotNull("documentId", documentId);
      ValidationChecks.checkNotNull("workflowId", workflowId);
      ValidationChecks.checkNotNull("workflowName", workflowName);

      String pk = "docs#" + documentId;
      String sk = buildWorkflowSk();

      String gsi1Pk = buildWorkflowDocumentKey("#");
      String gsi1Sk = "wf#" + workflowName + "#" + workflowId;

      String gsi2Pk = "wf#" + workflowId;
      String gsi2Sk = buildWorkflowDocumentKey("_");

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(gsi2Sk).build();
    }

    /**
     * Build {@link DynamoDbKey} for installs previous to 1.19.
     * 
     * @param siteId {@link String}
     * @return {@link DynamoDbKey}
     */
    public DynamoDbKey buildLegacyKey(final String siteId) {

      String pk = "wfdoc#" + documentId;
      String sk = buildWorkflowSk();

      String gsi1Pk = buildWorkflowDocumentKey("#");
      String gsi1Sk = "wf#" + workflowName + "#" + workflowId;

      String gsi2Pk = "wf#" + workflowId;
      String gsi2Sk = buildWorkflowDocumentKey("_");

      return DynamoDbKey.builder().pk(siteId, pk).sk(sk).gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk)
          .gsi2Pk(siteId, gsi2Pk).gsi2Sk(gsi2Sk).build();
    }

    private String buildWorkflowDocumentKey(final String delimiter) {
      return artifactId != null ? "wfdoc#" + documentId + delimiter + "art#" + artifactId
          : "wfdoc#" + documentId;
    }

    private String buildWorkflowSk() {
      return artifactId != null ? "wf#" + workflowId + "#art#" + artifactId : "wf#" + workflowId;
    }

    /**
     * Sets the current workflow step id.
     *
     * @param stepId the current step id
     * @return this {@link Builder}
     */
    public Builder currentStepId(final String stepId) {
      this.currentStepId = stepId;
      return this;
    }

    /**
     * Sets the document id.
     *
     * @param id the document id
     * @return this {@link Builder}
     */
    public Builder documentId(final String id) {
      this.documentId = id;
      return this;
    }

    /**
     * Sets the inserted date (defensive copy).
     *
     * @param date the inserted date
     * @return this {@link Builder}
     */
    public Builder insertedDate(final Date date) {
      this.insertedDate = date == null ? null : new Date(date.getTime());
      return this;
    }

    /**
     * Sets the status.
     *
     * @param workflowStatus the status
     * @return this {@link Builder}
     */
    public Builder status(final String workflowStatus) {
      this.status = workflowStatus;
      return this;
    }

    /**
     * Sets the workflow id.
     *
     * @param id the workflow id
     * @return this {@link Builder}
     */
    public Builder workflowId(final String id) {
      this.workflowId = id;
      return this;
    }

    /**
     * Sets the workflow name.
     *
     * @param name the workflow name
     * @return this {@link Builder}
     */
    public Builder workflowName(final String name) {
      this.workflowName = name;
      return this;
    }
  }
}
