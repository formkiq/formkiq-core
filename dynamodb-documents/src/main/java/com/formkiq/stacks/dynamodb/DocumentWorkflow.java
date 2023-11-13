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
public class DocumentWorkflow implements DynamodbRecord<DocumentWorkflow>, DbKeys {

  /** Document Workflow Status. */
  @Reflectable
  private String actionPk;
  /** Document Workflow Status. */
  @Reflectable
  private String actionSk;
  /** DocumentId. */
  @Reflectable
  private String documentId;
  /** Document Workflow Status. */
  @Reflectable
  private String status;
  /** DocumentId. */
  @Reflectable
  private String workflowId;

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
   * @return {@link DocumentWorkflow}
   */
  public DocumentWorkflow actionPk(final String pk) {
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
   * @return {@link DocumentWorkflow}
   */
  public DocumentWorkflow actionSk(final String sk) {
    this.actionSk = sk;
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
   * @return {@link DocumentWorkflow}
   */
  public DocumentWorkflow documentId(final String id) {
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
    map.put("status", AttributeValue.fromS(this.status));
    map.put("actionPk", AttributeValue.fromS(this.actionPk));
    map.put("actionSk", AttributeValue.fromS(this.actionSk));

    return map;
  }

  @Override
  public DocumentWorkflow getFromAttributes(final String siteIdParam,
      final Map<String, AttributeValue> attrs) {

    DocumentWorkflow record = new DocumentWorkflow().documentId(ss(attrs, "documentId"))
        .workflowId(ss(attrs, "workflowId")).status(ss(attrs, "status"))
        .actionPk(ss(attrs, "actionPk")).actionSk(ss(attrs, "actionSk"));

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
    return null;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
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
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
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
   * @return {@link DocumentWorkflow}
   */
  public DocumentWorkflow status(final String workflowStatus) {
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
   * @return {@link DocumentWorkflow}
   */
  public DocumentWorkflow workflowId(final String id) {
    this.workflowId = id;
    return this;
  }
}
