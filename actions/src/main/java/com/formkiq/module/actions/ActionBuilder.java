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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.validation.ValidationBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * Fluent builder for {@link Action} that computes the {@link DynamoDbKey}.
 */
public class ActionBuilder implements DynamoDbEntityBuilder<Action>, DbKeys {

  /** DocumentId. */
  private String documentId;
  /** Index. */
  private String index;
  /** Action Type. */
  private ActionType type;
  /** Action Status. */
  private ActionStatus status = ActionStatus.PENDING;
  /** UserId. */
  private String userId;
  /** Message. */
  private String message;
  /** QueueId. */
  private String queueId;
  /** WorkflowId. */
  private String workflowId;
  /** Workflow Last Step. */
  private String workflowLastStep;
  /** Workflow Step Id. */
  private String workflowStepId;
  /** Metadata. */
  private Map<String, String> metadata;
  /** Parameters. */
  private Map<String, Object> parameters;
  /** Retry Count. */
  private Integer retryCount;
  /** Max Retries. */
  private Integer maxRetries;
  /** Inserted Date. */
  private Date insertedDate;
  /** Start Date. */
  private Date startDate;
  /** Completed Date. */
  private Date completedDate;

  /**
   * Set {@link Action}.
   *
   * @param action {@link Action}
   * @return {@link ActionBuilder}
   */
  public ActionBuilder action(final Action action) {

    if (action != null) {
      documentId = action.documentId();
      index = action.index();
      type = action.type();
      status = action.status();
      userId = action.userId();
      message = action.message();
      queueId = action.queueId();
      workflowId = action.workflowId();
      workflowLastStep = action.workflowLastStep();
      workflowStepId = action.workflowStepId();
      metadata = action.metadata() != null ? new HashMap<>(action.metadata()) : null;
      parameters = action.parameters() != null ? new HashMap<>(action.parameters()) : null;
      retryCount = action.retryCount();
      maxRetries = action.maxRetries();
      insertedDate = action.insertedDate();
      startDate = action.startDate();
      completedDate = action.completedDate();
    }

    return this;
  }

  @Override
  public Action build(final DynamoDbKey key) {
    return new Action(key, documentId, index, type, status, userId, message, queueId, workflowId,
        workflowLastStep, workflowStepId, metadata, parameters, retryCount, maxRetries,
        insertedDate, startDate, completedDate);
  }

  /**
   * Builds an {@link Action}, computing its {@link DynamoDbKey}.
   *
   * @param siteId the site identifier
   * @return a new {@link Action}
   */
  @Override
  public Action build(final String siteId) {
    if (insertedDate == null) {
      insertedDate = new Date();
    }

    validate();
    DynamoDbKey key = buildKey(siteId);

    return build(key);
  }

  /**
   * Builds the {@link DynamoDbKey} for this action.
   *
   * @param siteId the site identifier
   * @return a {@link DynamoDbKey}
   */
  @Override
  public DynamoDbKey buildKey(final String siteId) {
    validateKeyFields();

    String pk = PREFIX_DOCS + documentId;
    String sk = "action" + TAG_DELIMINATOR + index + TAG_DELIMINATOR + type.name();

    DynamoDbKey.Builder b = DynamoDbKey.builder().pk(siteId, pk).sk(sk);

    b = b.gsi1Pk(siteId,
        ActionStatus.IN_QUEUE.equals(status) ? "action#" + type + "#" + queueId : null);
    b.gsi1Sk(ActionStatus.IN_QUEUE.equals(status)
        ? "action#" + documentId + "#" + DateUtil.getIsoDateFormatter().format(new Date())
        : null);

    b.gsi2Pk(siteId,
        !ActionStatus.COMPLETE.equals(this.status) ? "actions#" + this.status + "#" : null);
    b.gsi2Sk(!ActionStatus.COMPLETE.equals(this.status) ? "action#" + this.documentId : null);

    return b.build();
  }

  /**
   * Sets completed date.
   *
   * @param date completed date
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder completedDate(final Date date) {
    this.completedDate = date == null ? null : new Date(date.getTime());
    return this;
  }

  /**
   * Sets document id.
   *
   * @param id document id
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Sets index.
   *
   * @param idx index
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder index(final String idx) {
    this.index = idx;
    return this;
  }

  /**
   * Generates a ULID for {@code index}.
   *
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder indexUlid() {
    return index(ID.ulid());
  }

  /**
   * Sets inserted date.
   *
   * @param date inserted date
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder insertedDate(final Date date) {
    this.insertedDate = date == null ? null : new Date(date.getTime());
    return this;
  }

  /**
   * Sets max retries.
   *
   * @param count max retries
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder maxRetries(final Integer count) {
    this.maxRetries = count;
    return this;
  }

  /**
   * Sets action message.
   *
   * @param actionMessage message
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder message(final String actionMessage) {
    this.message = actionMessage;
    return this;
  }

  /**
   * Sets metadata.
   *
   * @param map metadata map
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder metadata(final Map<String, String> map) {
    this.metadata = map;
    return this;
  }

  /**
   * Sets parameters.
   *
   * @param map parameters map
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder parameters(final Map<String, Object> map) {
    this.parameters = map;
    return this;
  }

  /**
   * Sets queue id.
   *
   * @param id queue id
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder queueId(final String id) {
    this.queueId = id;
    return this;
  }

  /**
   * Sets retry count.
   *
   * @param count retry count
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder retryCount(final Integer count) {
    this.retryCount = count;
    return this;
  }

  /**
   * Sets start date.
   *
   * @param date start date
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder startDate(final Date date) {
    this.startDate = date == null ? null : new Date(date.getTime());
    return this;
  }

  /**
   * Get {@link ActionStatus}.
   *
   * @return {@link ActionStatus}
   */
  public ActionStatus status() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param actionStatus status
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder status(final ActionStatus actionStatus) {
    this.status = actionStatus;
    return this;
  }

  /**
   * Sets type.
   *
   * @param actionType type
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder type(final ActionType actionType) {
    this.type = actionType;
    return this;
  }

  /**
   * Sets user id.
   *
   * @param user user id
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder userId(final String user) {
    this.userId = user;
    return this;
  }

  /**
   * Validates record fields.
   */
  private void validate() {
    ValidationBuilder vb = new ValidationBuilder();

    vb.isRequired("documentId", documentId);
    vb.isRequired("userId", userId);
    vb.isRequired("type", type);
    vb.isRequired("status", status);

    if (isEmpty(index)) {
      vb.isRequired("index", index, "'index' is required");
    }

    if (ActionStatus.IN_QUEUE.equals(status)) {
      vb.isRequired("queueId", queueId);
    }

    vb.check();
  }

  /**
   * Validates fields required to build keys.
   */
  private void validateKeyFields() {
    ValidationBuilder vb = new ValidationBuilder();

    vb.isNotNull("documentId", documentId);
    vb.isNotNull("type", type);
    vb.isNotNull("index", index);

    if (ActionStatus.IN_QUEUE.equals(status)) {
      vb.isNotNull("queueId", queueId);
    }

    vb.check();
  }

  /**
   * Sets workflow id.
   *
   * @param id workflow id
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder workflowId(final String id) {
    this.workflowId = id;
    return this;
  }

  /**
   * Sets workflow last step.
   *
   * @param lastStep last step
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder workflowLastStep(final String lastStep) {
    this.workflowLastStep = lastStep;
    return this;
  }

  /**
   * Sets workflow step id.
   *
   * @param stepId step id
   * @return this {@link ActionBuilder}
   */
  public ActionBuilder workflowStepId(final String stepId) {
    this.workflowStepId = stepId;
    return this;
  }
}
