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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Action.
 */
public class Action implements DynamodbRecord<Action>, DbKeys {

  /** Record Completed date. */
  @Reflectable
  private Date completedDate;
  /** DocumentId. */
  @Reflectable
  private String documentId;
  /** Index. */
  @Reflectable
  private String index = null;
  /** Record inserted date. */
  @Reflectable
  private Date insertedDate;
  /** Action Message. */
  @Reflectable
  private String message;
  /** Action Metadata. */
  @Reflectable
  private Map<String, String> metadata;
  /** Action Parameters. */
  @Reflectable
  private Map<String, String> parameters;
  /** QueueId. */
  @Reflectable
  private String queueId;
  /** Record Start date. */
  @Reflectable
  private Date startDate;
  /** Is Action Completed. */
  @Reflectable
  private ActionStatus status;
  /** Type of Action. */
  @Reflectable
  private ActionType type;
  /** UserId. */
  @Reflectable
  private String userId;
  /** WorkflowId. */
  @Reflectable
  private String workflowId;
  /** Workflow Last Step. */
  @Reflectable
  private String workflowLastStep;
  /** Workflow Step Id. */
  @Reflectable
  private String workflowStepId;

  /**
   * constructor.
   */
  public Action() {
    this.status = ActionStatus.PENDING;
  }

  /**
   * Get Completed Date.
   * 
   * @return {@link Date}
   */
  public Date completedDate() {
    return this.completedDate;
  }

  /**
   * Set Completed Date.
   * 
   * @param date {@link Date}
   * @return {@link Action}
   */
  public Action completedDate(final Date date) {
    this.completedDate = date;
    return this;
  }

  /**
   * Get DocumentId.
   * 
   * @return {@link String}
   */
  public String documentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return {@link Action}
   */
  public Action documentId(final String id) {
    this.documentId = id;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> attrs =
        new HashMap<>(Map.of(DbKeys.PK, fromS(pk(siteId)), DbKeys.SK, fromS(sk()), "type",
            fromS(this.type.name()), "status", fromS(this.status.name()), "documentId",
            fromS(this.documentId), "userId", fromS(this.userId)));

    addM(attrs, "parameters", this.parameters);
    addM(attrs, "metadata", this.metadata);

    String pkGsi1 = pkGsi1(siteId);
    String skGsi1 = skGsi1();

    if (pkGsi1 != null && skGsi1 != null) {
      attrs.put(GSI1_PK, fromS(pkGsi1));
      attrs.put(GSI1_SK, fromS(skGsi1));
    }

    String pkGsi2 = pkGsi2(siteId);
    String skGsi2 = skGsi2();

    if (pkGsi2 != null && skGsi2 != null) {
      attrs.put(GSI2_PK, fromS(pkGsi2));
      attrs.put(GSI2_SK, fromS(skGsi2));
    }

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    if (this.insertedDate != null) {
      attrs.put("inserteddate", AttributeValue.fromS(df.format(this.insertedDate)));
    }

    if (this.completedDate != null) {
      attrs.put("completedDate", AttributeValue.fromS(df.format(this.completedDate)));
    }

    if (this.startDate != null) {
      attrs.put("startDate", AttributeValue.fromS(df.format(this.startDate)));
    }

    addS(attrs, "message", this.message);
    addS(attrs, "queueId", this.queueId);
    addS(attrs, "workflowId", this.workflowId);
    addS(attrs, "workflowLastStep", this.workflowLastStep);
    addS(attrs, "workflowStepId", this.workflowStepId);

    return attrs;
  }

  private Date getDate(final SimpleDateFormat df, final Map<String, AttributeValue> attrs,
      final String key) {

    Date date = null;

    if (attrs.containsKey(key)) {
      try {
        date = df.parse(ss(attrs, key));
      } catch (ParseException e) {
        // ignore
      }
    }

    return date;
  }

  @Override
  public Action getFromAttributes(final String siteId, final Map<String, AttributeValue> attrs) {

    Action record = new Action().documentId(ss(attrs, "documentId")).userId(ss(attrs, "userId"))
        .message(ss(attrs, "message")).queueId(ss(attrs, "queueId"))
        .workflowId(ss(attrs, "workflowId")).workflowLastStep(ss(attrs, "workflowLastStep"))
        .workflowStepId(ss(attrs, "workflowStepId"));

    if (attrs.containsKey("status")) {
      record.status(ActionStatus.valueOf(ss(attrs, "status")));
    }

    if (attrs.containsKey("type")) {
      record.type(ActionType.valueOf(ss(attrs, "type")));
    }

    if (attrs.containsKey("parameters")) {
      record.parameters(attrs.get("parameters").m().entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s())));
    }

    if (attrs.containsKey("metadata")) {
      record.metadata(attrs.get("metadata").m().entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s())));
    }

    if (attrs.containsKey(SK)) {
      record.index(attrs.get(SK).s().split(TAG_DELIMINATOR)[1]);
    }

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    record = record.insertedDate(getDate(df, attrs, "inserteddate"));
    record = record.completedDate(getDate(df, attrs, "completedDate"));
    record = record.startDate(getDate(df, attrs, "startDate"));

    return record;
  }

  /**
   * Get Index.
   * 
   * @return {@link String}
   */
  public String index() {
    return this.index;
  }

  /**
   * Set Index.
   * 
   * @param idx int
   * @return {@link Action}
   */
  public Action index(final String idx) {
    this.index = idx;
    return this;
  }

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date insertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return {@link Action}
   */
  public Action insertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Get Action Message.
   * 
   * @return {@link String}
   */
  public String message() {
    return this.message;
  }

  /**
   * Set Action Message.
   * 
   * @param actionMessage {@link String}
   * @return {@link Action}
   */
  public Action message(final String actionMessage) {
    this.message = actionMessage;
    return this;
  }

  /**
   * Get Action Metadata.
   * 
   * @return {@link Map}
   */
  public Map<String, String> metadata() {
    return this.metadata;
  }

  /**
   * Set Action Metadata.
   * 
   * @param map {@link Map}
   * @return {@link Action}
   */
  public Action metadata(final Map<String, String> map) {
    this.metadata = map;
    return this;
  }

  /**
   * Get Action parameters {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, String> parameters() {
    return this.parameters;
  }

  /**
   * Set Action parameters {@link Map}.
   * 
   * @param map {@link Map}
   * @return {@link Action}
   */
  public Action parameters(final Map<String, String> map) {
    this.parameters = map;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, PREFIX_DOCS + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {

    String pk = null;

    if (this.status.equals(ActionStatus.IN_QUEUE)) {
      pk = createDatabaseKey(siteId, "action#" + this.type + "#" + this.queueId);
    }

    return pk;
  }

  @Override
  public String pkGsi2(final String siteId) {
    String pk = null;

    if (!ActionStatus.COMPLETE.equals(this.status)) {
      pk = createDatabaseKey(siteId, "actions#" + this.status + "#");
    }
    return pk;
  }

  /**
   * Get Queue Id.
   * 
   * @return {@link String}
   */
  public String queueId() {
    return this.queueId;
  }

  /**
   * Set Queue Id.
   * 
   * @param id {@link String}
   * @return {@link Action}
   */
  public Action queueId(final String id) {
    this.queueId = id;
    return this;
  }

  @Override
  public String sk() {
    if (isEmpty(this.index)) {
      throw new IllegalArgumentException("'index' is required");
    }
    if (this.type == null) {
      throw new IllegalArgumentException("'type' is required");
    }
    return "action" + TAG_DELIMINATOR + this.index + TAG_DELIMINATOR + this.type.name();
  }

  @Override
  public String skGsi1() {

    String sk = null;
    if (this.status.equals(ActionStatus.IN_QUEUE)) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();
      sk = "action#" + this.documentId + "#" + df.format(new Date());
    }

    return sk;
  }

  @Override
  public String skGsi2() {

    String sk = null;

    if (!ActionStatus.COMPLETE.equals(this.status)) {
      sk = "action#" + this.documentId;
    }

    return sk;
  }

  /**
   * Get Start Date.
   * 
   * @return {@link Date}
   */
  public Date startDate() {
    return this.startDate;
  }

  /**
   * Set Start Date.
   * 
   * @param date {@link Date}
   * @return {@link Action}
   */
  public Action startDate(final Date date) {
    this.startDate = date;
    return this;
  }

  /**
   * Get {@link ActionStatus}.
   * 
   * @return {@link ActionStatus}
   */
  public ActionStatus status() {
    return this.status;
  }

  /**
   * Set {@link ActionStatus}.
   * 
   * @param actionStatus {@link ActionStatus}
   * @return {@link Action}
   */
  public Action status(final ActionStatus actionStatus) {
    this.status = actionStatus;
    return this;
  }

  /**
   * Get {@link ActionType}.
   * 
   * @return {@link ActionType}
   */
  public ActionType type() {
    return this.type;
  }

  /**
   * Set {@link ActionType}.
   * 
   * @param actionType {@link ActionType}
   * @return {@link Action}
   */
  public Action type(final ActionType actionType) {
    this.type = actionType;
    return this;
  }

  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String userId() {
    return this.userId;
  }

  /**
   * Set UserId.
   * 
   * @param user {@link String}
   * @return {@link Action}
   */
  public Action userId(final String user) {
    this.userId = user;
    return this;
  }

  /**
   * Get Workflow Id.
   * 
   * @return {@link String}
   */
  public String workflowId() {
    return this.workflowId;
  }

  /**
   * Get Workflow Id.
   * 
   * @param id {@link String}
   * @return {@link Action}
   */
  public Action workflowId(final String id) {
    this.workflowId = id;
    return this;
  }

  /**
   * Get Workflow Last Step.
   * 
   * @return {@link String}
   */
  public String workflowLastStep() {
    return this.workflowLastStep;
  }

  /**
   * Set Workflow Last Step.
   * 
   * @param lastStep {@link String}
   * @return {@link Action}
   */
  public Action workflowLastStep(final String lastStep) {
    this.workflowLastStep = lastStep;
    return this;
  }

  /**
   * Get Workflow Step id.
   * 
   * @return {@link String}
   */
  public String workflowStepId() {
    return this.workflowStepId;
  }

  /**
   * Get Workflow Step id.
   * 
   * @param stepId {@link String}
   * @return {@link Action}
   */
  public Action workflowStepId(final String stepId) {
    this.workflowStepId = stepId;
    return this;
  }
}
