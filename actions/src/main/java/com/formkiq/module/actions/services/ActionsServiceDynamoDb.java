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
package com.formkiq.module.actions.services;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * DynamoDB implemenation of {@link ActionsService}.
 *
 */
public class ActionsServiceDynamoDb implements ActionsService, DbKeys {

  /** MilliSeconds per Second. */
  private static final int MILLISECONDS = 1000;
  /** Document Table Name. */
  private String documentTableName;
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dynamoDB;

  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ActionsServiceDynamoDb(final DynamoDbConnectionBuilder builder,
      final String documentsTable) {

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;
  }

  @Override
  public List<Action> getActions(final String siteId, final String documentId) {
    return queryActions(siteId, documentId, null, null);
  }

  /**
   * Get Pk.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link String}
   */
  private String getPk(final String siteId, final String documentId) {
    return createDatabaseKey(siteId, "docactions#" + documentId);
  }

  /**
   * Get Sk.
   * 
   * @param action {@link Action}
   * @param idx int
   * @return {@link String}
   */
  private String getSk(final Action action, final int idx) {
    return "action" + TAG_DELIMINATOR + idx + TAG_DELIMINATOR + action.type().name();
  }

  @Override
  public boolean hasActions(final String siteId, final String documentId) {
    List<Action> actions = queryActions(siteId, documentId, PK, null);
    return !actions.isEmpty();
  }

  /**
   * Query Document Actions.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param projectionExpression {@link String}
   * @param limit {@link Integer}
   * @return {@link List} {@link Action}
   */
  private List<Action> queryActions(final String siteId, final String documentId,
      final String projectionExpression, final Integer limit) {

    String pk = getPk(siteId, documentId);
    String sk = "action" + TAG_DELIMINATOR;

    String expression = PK + " = :pk and begins_with(" + SK + ", :sk)";
    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue.builder().s(pk).build(),
        ":sk", AttributeValue.builder().s(sk).build());

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .projectionExpression(projectionExpression).limit(limit).build();

    QueryResponse result = this.dynamoDB.query(q);

    AttributeValueToAction transform = new AttributeValueToAction();
    return result.items().stream().map(r -> transform.apply(r)).collect(Collectors.toList());
  }

  @Override
  public void saveActions(final String siteId, final String documentId,
      final List<Action> actions) {

    List<WriteRequest> list = new ArrayList<>();

    ZonedDateTime tomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
    Date tomorrowttl = Date.from(tomorrow.toInstant());
    String ttl = String.valueOf(tomorrowttl.getTime() / MILLISECONDS);
    int idx = 0;

    for (Action action : actions) {

      Map<String, AttributeValue> values = new HashMap<>();

      String pk = getPk(siteId, documentId);
      String sk = getSk(action, idx);
      addS(values, PK, pk);
      addS(values, SK, sk);
      addS(values, "type", action.type().name());
      addS(values, "status", action.status().name());
      addS(values, "documentId", documentId);
      addS(values, "userId", action.userId());
      addM(values, "parameters", action.parameters());
      addN(values, "TimeToLive", ttl);

      PutRequest put = PutRequest.builder().item(values).build();
      WriteRequest req = WriteRequest.builder().putRequest(put).build();
      list.add(req);

      idx++;
    }

    Map<String, Collection<WriteRequest>> items = Map.of(this.documentTableName, list);

    BatchWriteItemRequest batch = BatchWriteItemRequest.builder().requestItems(items).build();
    this.dynamoDB.batchWriteItem(batch);
  }

  @Override
  public void updateActionStatus(final String siteId, final String documentId, final Action action,
      final int index) {

    String pk = getPk(siteId, documentId);
    String sk = getSk(action, index);

    Map<String, AttributeValue> key = Map.of(PK, AttributeValue.builder().s(pk).build(), SK,
        AttributeValue.builder().s(sk).build());

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    values.put("status", AttributeValueUpdate.builder()
        .value(AttributeValue.builder().s(action.status().name()).build()).build());

    this.dynamoDB.updateItem(UpdateItemRequest.builder().tableName(this.documentTableName).key(key)
        .attributeUpdates(values).build());
  }

  @Override
  public List<Action> updateActionStatus(final String siteId, final String documentId,
      final ActionType type, final ActionStatus status) {
    
    int idx = 0;
    NextActionPredicate pred = new NextActionPredicate();
    List<Action> actionlist = getActions(siteId, documentId);
    for (Action action : actionlist) {
      if (pred.test(action) && action.type().equals(type)) {
        action.status(ActionStatus.COMPLETE);
        updateActionStatus(siteId, documentId, action, idx);
        break;
      }
      idx++;
    }
    
    return actionlist;
  }
}
