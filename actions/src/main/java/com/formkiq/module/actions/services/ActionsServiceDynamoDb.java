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
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.WriteRequestBuilder;
import com.formkiq.aws.dynamodb.actions.FindDocumentActionByStatus;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionBuilder;
import com.formkiq.module.actions.ActionIndexComparator;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.github.f4b6a3.ulid.Ulid;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDB implemenation of {@link ActionsService}.
 *
 */
public final class ActionsServiceDynamoDb implements ActionsService, DbKeys {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** Document Table Name. */
  private final String documentTableName;

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public ActionsServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String documentsTable) {

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = connection.build();
    this.documentTableName = documentsTable;
    this.db = new DynamoDbServiceImpl(this.dbClient, documentsTable);
  }

  private void addUpdateActionAttributes(final WriteRequestBuilder writeRequestBuilder,
      final Action action) {
    var attributes = new HashMap<>(action.getAttributes());

    if (!attributes.containsKey(GSI1_PK) || !attributes.containsKey(GSI1_SK)) {
      attributes.put(GSI1_PK, null);
      attributes.put(GSI1_SK, null);
    }

    if (!attributes.containsKey(GSI2_PK) || !attributes.containsKey(GSI2_SK)) {
      attributes.put(GSI2_PK, null);
      attributes.put(GSI2_SK, null);
    }

    writeRequestBuilder.appendUpdate(db.getTableName(), attributes);
  }

  private void deleteAction(final Action action) {
    this.db.deleteItem(action.key());
  }

  @Override
  public void deleteActions(final Collection<Action> actions) {
    var writeRequestBuilder = new WriteRequestBuilder();
    actions.forEach(a -> writeRequestBuilder.appendDelete(db.getTableName(), a.key()));
    writeRequestBuilder.transactWriteItems(dbClient);
  }

  @Override
  public void deleteActions(final String siteId, final String documentId) {

    List<Action> actions =
        queryActions(siteId, documentId, Arrays.asList(PK, SK, "type"), null, null).getResults();

    for (Action action : actions) {
      deleteAction(action);
    }
  }

  @Override
  public Action findActionInQueue(final String siteId, final String documentId,
      final String queueName) {
    String pk = createDatabaseKey(siteId, "action#" + ActionType.QUEUE + "#" + queueName);
    String sk = "action#" + documentId + "#";

    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);
    QueryResponse response = this.db.queryBeginsWith(config, fromS(pk), fromS(sk), null, 1);

    Action action = null;
    if (!response.items().isEmpty()) {
      Map<String, AttributeValue> attrs = response.items().get(0);
      attrs = this.db.get(attrs.get(PK), attrs.get(SK));
      action = Action.fromAttributeMap(attrs);
    }

    return action;
  }

  @Override
  public Pagination<Action> findDocumentsInQueue(final String siteId, final String queueName,
      final String nextToken, final int limit) {

    BatchGetConfig batchConfig = new BatchGetConfig();
    String pk = createDatabaseKey(siteId, "action#" + ActionType.QUEUE + "#" + queueName);
    String sk = "action#";

    Map<String, AttributeValue> exclusiveStartKey =
        new StringToMapAttributeValue().apply(nextToken);

    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);
    QueryResponse response =
        this.db.queryBeginsWith(config, fromS(pk), fromS(sk), exclusiveStartKey, limit);

    List<Map<String, AttributeValue>> keys = response.items().stream()
        .map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).collect(Collectors.toList());

    List<Action> list = this.db.getBatch(batchConfig, keys).stream().map(Action::fromAttributeMap)
        .collect(Collectors.toList());

    return new Pagination<>(list, response.lastEvaluatedKey());
  }

  @Override
  public Pagination<String> findDocumentsWithStatus(final String siteId, final ActionStatus status,
      final String nextToken, final int limit) {

    // DUMMY value
    ActionType type = ActionType.OCR;
    DynamoDbKey key = new ActionBuilder().status(status).documentId("").queueId("").type(type)
        .index("").buildKey(siteId);
    // String pk = a.pkGsi2(siteId);
    String sk = "action#";

    QueryResponse response = null;
    List<String> list = Collections.emptyList();

    if (key.gsi2Pk() != null) {

      QueryRequest q = DynamoDbQueryBuilder.builder().indexName(GSI2, key).beginsWith(sk)
          .scanIndexForward(true).nextToken(nextToken).limit(limit).build(db.getTableName());
      response = this.db.query(q);

      list =
          response.items().stream().map(i -> DynamoDbTypes.toString(i.get("documentId"))).toList();
    }

    return new Pagination<>(list, response != null ? response.lastEvaluatedKey() : null);

  }

  @Override
  public List<Action> getAction(final String siteId, final String documentId,
      final ActionStatus status) {
    var results = new FindDocumentActionByStatus(documentId, status.name()).query(db,
        db.getTableName(), siteId, null, 2);
    return results.items().stream().map(Action::fromAttributeMap).toList();
  }

  @Override
  public List<Action> getActions(final String siteId, final String documentId) {
    return queryActions(siteId, documentId, null, null, null).getResults();
  }

  @Override
  public Pagination<Action> getActions(final String siteId, final String documentId,
      final String nextToken, final int limit) {
    return queryActions(siteId, documentId, null, nextToken, limit);
  }

  @Override
  public boolean hasActions(final String siteId, final String documentId) {
    List<Action> actions =
        queryActions(siteId, documentId, List.of(PK, SK), null, null).getResults();
    return !actions.isEmpty();
  }

  @Override
  public void insertBeforeAction(final String siteId, final Action currentAction,
      final ActionBuilder insertedAction) {

    String index = previousIndex(currentAction.index());
    insertedAction.index(index).insertedDate(new Date());
    saveNewActions(List.of(insertedAction.build(siteId)));
  }

  @Override
  public String previousIndex(final String index) {
    Ulid ulid = Ulid.from(index);

    long msb = ulid.getMostSignificantBits();
    long lsb = ulid.getLeastSignificantBits();

    if (lsb == 0) {
      lsb = -1L;
      msb -= 1;
    } else {
      lsb -= 1;
    }

    if (msb < 0) {
      throw new IllegalArgumentException("No smaller ULID exists");
    }

    return new Ulid(msb, lsb).toString();
  }

  /**
   * Query Document Actions.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param projectionExpression {@link List} {@link String}
   * @param limit {@link Integer}
   * @param nextToken {@link String}
   * @return {@link Pagination} {@link Action}
   */
  private Pagination<Action> queryActions(final String siteId, final String documentId,
      final List<String> projectionExpression, final String nextToken, final Integer limit) {

    // DUMMY VALUE
    var actionType = ActionType.OCR;
    DynamoDbKey key =
        new ActionBuilder().documentId(documentId).indexUlid().type(actionType).buildKey(siteId);
    String sk = "action" + TAG_DELIMINATOR;

    String expression = PK + " = :pk and begins_with(" + SK + ", :sk)";
    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue.builder().s(key.pk()).build(),
        ":sk", AttributeValue.builder().s(sk).build());

    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    Builder q = QueryRequest.builder().tableName(this.documentTableName).exclusiveStartKey(startKey)
        .keyConditionExpression(expression).expressionAttributeValues(values).limit(limit);

    if (!notNull(projectionExpression).isEmpty()) {

      Map<String, String> names = new HashMap<>();
      int i = 1;
      for (String p : projectionExpression) {
        names.put("#" + i, p);
        i++;
      }

      q = q.projectionExpression(String.join(",", names.keySet())).expressionAttributeNames(names);
    }

    QueryResponse response = this.dbClient.query(q.build());

    List<Action> actions = response.items().stream().map(Action::fromAttributeMap)
        .sorted(new ActionIndexComparator()).collect(Collectors.toList());

    return new Pagination<>(actions, response.lastEvaluatedKey());
  }

  @Override
  public List<Map<String, AttributeValue>> saveNewActions(final List<Action> actions) {

    WriteRequestBuilder builder = new WriteRequestBuilder();
    List<Map<String, AttributeValue>> values =
        notNull(actions).stream().map(Action::getAttributes).toList();
    builder.appends(this.documentTableName, values);
    builder.transactWriteItems(dbClient);

    return values;
  }

  @Override
  public void updateAction(final Action action) {
    var writeRequestBuilder = new WriteRequestBuilder();
    addUpdateActionAttributes(writeRequestBuilder, action);
    writeRequestBuilder.transactWriteItems(dbClient);
  }

  @Override
  public void updateActions(final Collection<Action> actions) {
    var writeRequestBuilder = new WriteRequestBuilder();
    actions.forEach(a -> addUpdateActionAttributes(writeRequestBuilder, a));
    writeRequestBuilder.transactWriteItems(dbClient);
  }
}
