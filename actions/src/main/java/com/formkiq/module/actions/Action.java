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
import static com.formkiq.module.actions.ActionParameters.PARAMETER_QUEUE_NAME;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Action.
 */
public class Action implements DynamodbRecord<Action>, DbKeys {

  /** DocumentId. */
  private String documentId;
  /** Index. */
  private String index = null;
  /** Action Metadata. */
  private Map<String, String> metadata;
  /** Action Parameters. */
  private Map<String, String> parameters;
  /** Is Action Completed. */
  private ActionStatus status;
  /** Type of Action. */
  private ActionType type;

  /** UserId. */
  private String userId;

  /**
   * constructor.
   */
  public Action() {
    this.status = ActionStatus.PENDING;
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

    return attrs;
  }

  @Override
  public Action getFromAttributes(final String siteId, final Map<String, AttributeValue> attrs) {

    Action record = new Action().documentId(ss(attrs, "documentId"))
        .status(ActionStatus.valueOf(ss(attrs, "status")))
        .type(ActionType.valueOf(ss(attrs, "type"))).userId(ss(attrs, "userId"));

    record.parameters(attrs.get("parameters").m().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s())));

    record.metadata(attrs.get("metadata").m().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s())));

    this.index = attrs.get(SK).s().split(TAG_DELIMINATOR)[1];

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
      String queueName = this.parameters.get(PARAMETER_QUEUE_NAME);
      pk = createDatabaseKey(siteId, "action#" + this.type + "#" + queueName);
    }

    return pk;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    if (isEmpty(this.index)) {
      throw new IllegalArgumentException("'index' is required");
    }
    return "action" + TAG_DELIMINATOR + this.index + TAG_DELIMINATOR + this.type.name();
  }

  @Override
  public String skGsi1() {

    String sk = null;
    if (this.status.equals(ActionStatus.IN_QUEUE)) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();
      sk = "action#" + df.format(new Date());
    }

    return sk;
  }

  @Override
  public String skGsi2() {
    return null;
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
}
