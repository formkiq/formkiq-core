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
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Queue object.
 *
 */
@Reflectable
public class Queue implements DynamodbRecord<Queue> {

  /** Queue Document Id. */
  @Reflectable
  private String documentId;
  /** Record inserted date. */
  /** Name of Queue. */
  @Reflectable
  private String name;
  /** Transient Field. */
  @Reflectable
  private String queueId;

  /**
   * constructor.
   */
  public Queue() {

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
   * @param document {@link String}
   * @return {@link Queue}
   */
  public Queue documentId(final String document) {
    this.documentId = document;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = new HashMap<>();

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));
    map.put(DbKeys.GSI1_PK, fromS(pkGsi1(siteId)));
    map.put(DbKeys.GSI1_SK, fromS(skGsi1()));
    map.put("documentId", fromS(this.documentId));
    map.put("name", fromS(this.name));

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    return null;
  }

  @Override
  public Queue getFromAttributes(final String siteId, final Map<String, AttributeValue> attrs) {

    Queue record = null;

    if (!attrs.isEmpty()) {
      record = new Queue().documentId(ss(attrs, "documentId")).name(ss(attrs, "name"));
    }

    return record;
  }

  /**
   * Get Workflow Name.
   * 
   * @return {@link String}
   */
  public String name() {
    return this.name;
  }

  /**
   * Set Workflow Name.
   * 
   * @param workflowName {@link String}
   * @return {@link Queue}
   */
  public Queue name(final String workflowName) {
    this.name = workflowName;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, "queues#" + this.documentId);

  }

  @Override
  public String pkGsi1(final String siteId) {
    return createDatabaseKey(siteId, "queues#");
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
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
   * @return {@link Queue}
   */
  public Queue queueId(final String id) {
    this.queueId = id;
    return this;
  }

  @Override
  public String sk() {
    return "queue";
  }

  @Override
  public String skGsi1() {
    if (this.name == null || this.documentId == null) {
      throw new IllegalArgumentException("'name' and 'documentId' is required");
    }
    return "queue#" + this.name + "#" + this.documentId;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
