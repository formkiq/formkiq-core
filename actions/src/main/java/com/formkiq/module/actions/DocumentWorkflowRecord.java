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

import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * {@link DynamodbRecord} for Document Workflow.
 *
 */
@Reflectable
public class DocumentWorkflowRecord implements DynamodbRecord<DocumentWorkflowRecord>, DbKeys {

  /** Document Workflow Status. */
  @Reflectable
  private String actionPk;
  /** Document Workflow Status. */
  @Reflectable
  private String actionSk;
  /** Cuurent Workflow Step. */
  @Reflectable
  private String currentStepId;
  /** DocumentId. */
  @Reflectable
  private String documentId;
  /** Document Workflow Status. */
  @Reflectable
  private String status;
  /** DocumentId. */
  @Reflectable
  private String workflowId;
  /** Cuurent Workflow Name. */
  @Reflectable
  private String workflowName;

  /**
   * constructor.
   */
  public DocumentWorkflowRecord() {

  }

  /**
   * Get Action PK.
   * 
   * @return {@link String}
   */
  public String actionPk() {
    return this.actionPk;
  }

  /**
   * Set Action PK.
   * 
   * @param pk {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord actionPk(final String pk) {
    this.actionPk = pk;
    return this;
  }

  /**
   * Get Action SK.
   * 
   * @return {@link String}
   */
  public String actionSk() {
    return this.actionSk;
  }

  /**
   * Set Action SK.
   * 
   * @param sk {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord actionSk(final String sk) {
    this.actionSk = sk;
    return this;
  }

  /**
   * Get Current Step Id.
   * 
   * @return {@link String}
   */
  public String currentStepId() {
    return this.currentStepId;
  }

  /**
   * Set Current Step Id.
   * 
   * @param stepId {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord currentStepId(final String stepId) {
    this.currentStepId = stepId;
    return this;
  }

  /**
   * Get Document Id.
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
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord documentId(final String id) {
    this.documentId = id;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteIdParam) {

    Map<String, AttributeValue> map = new HashMap<>();
    map.put(DbKeys.PK, AttributeValue.fromS(pk(siteIdParam)));
    map.put(DbKeys.SK, AttributeValue.fromS(sk()));
    map.put("documentId", AttributeValue.fromS(this.documentId));
    map.put("workflowId", AttributeValue.fromS(this.workflowId));
    map.put("workflowName", AttributeValue.fromS(this.workflowName));
    map.put("status", AttributeValue.fromS(this.status));
    map.put("actionPk", AttributeValue.fromS(this.actionPk));
    map.put("actionSk", AttributeValue.fromS(this.actionSk));
    map.put("currentStepId", AttributeValue.fromS(this.currentStepId));

    map.put(DbKeys.GSI1_PK, AttributeValue.fromS(pkGsi1(siteIdParam)));
    map.put(DbKeys.GSI1_SK, AttributeValue.fromS(skGsi1()));

    map.put(DbKeys.GSI2_PK, AttributeValue.fromS(pkGsi2(siteIdParam)));
    map.put(DbKeys.GSI2_SK, AttributeValue.fromS(skGsi2()));

    return map;
  }

  @Override
  public DocumentWorkflowRecord getFromAttributes(final String siteIdParam,
      final Map<String, AttributeValue> attrs) {

    DocumentWorkflowRecord record = new DocumentWorkflowRecord().documentId(ss(attrs, "documentId"))
        .workflowId(ss(attrs, "workflowId")).status(ss(attrs, "status"))
        .workflowName(ss(attrs, "workflowName")).actionPk(ss(attrs, "actionPk"))
        .actionSk(ss(attrs, "actionSk")).currentStepId(ss(attrs, "currentStepId"));

    return record;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return "wfdoc#" + this.documentId;
  }

  @Override
  public String pkGsi1(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return "wfdoc#" + this.documentId;
  }

  @Override
  public String pkGsi2(final String siteId) {
    if (this.workflowId == null) {
      throw new IllegalArgumentException("'workflowId' is required");
    }
    return "wf#" + this.workflowId;
  }

  @Override
  public String sk() {
    if (this.workflowId == null) {
      throw new IllegalArgumentException("'workflowId' is required");
    }
    return "wf#" + this.workflowId;
  }

  @Override
  public String skGsi1() {
    if (this.workflowId == null || this.workflowName == null) {
      throw new IllegalArgumentException("'workflowId' and 'workflowName' is required");
    }
    return "wf#" + this.workflowName + "#" + this.workflowId;
  }

  @Override
  public String skGsi2() {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return "wfdoc#" + this.documentId;
  }

  /**
   * Get Status.
   * 
   * @return {@link String}
   */
  public String status() {
    return this.status;
  }

  /**
   * Set Status.
   * 
   * @param workflowStatus {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord status(final String workflowStatus) {
    this.status = workflowStatus;
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
   * Set Workflow Id.
   * 
   * @param id {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord workflowId(final String id) {
    this.workflowId = id;
    return this;
  }

  /**
   * Get Workflow Name.
   * 
   * @return {@link String}
   */
  public String workflowName() {
    return this.workflowName;
  }

  /**
   * Set Workflow Name.
   * 
   * @param name {@link String}
   * @return {@link DocumentWorkflowRecord}
   */
  public DocumentWorkflowRecord workflowName(final String name) {
    this.workflowName = name;
    return this;
  }
}
