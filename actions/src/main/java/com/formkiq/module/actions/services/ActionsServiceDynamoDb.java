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
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.AttributeValuesToWriteRequests;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionIndexComparator;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.DocumentWorkflowRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * DynamoDB implemenation of {@link ActionsService}.
 *
 */
public class ActionsServiceDynamoDb implements ActionsService, DbKeys {

  /** {@link DynamoDbService}. */
  private DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;
  /** Document Table Name. */
  private String documentTableName;

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

  private void deleteAction(final String siteId, final Action action) {
    String pk = action.pk(siteId);
    String sk = action.sk();

    Map<String, AttributeValue> key = Map.of(PK, AttributeValue.builder().s(pk).build(), SK,
        AttributeValue.builder().s(sk).build());
    this.dbClient
        .deleteItem(DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build());
  }

  @Override
  public void deleteActions(final String siteId, final String documentId) {

    List<Action> actions = queryActions(siteId, documentId, Arrays.asList(PK, SK, "type"), null);

    for (Action action : actions) {

      action.documentId(documentId);
      deleteAction(siteId, action);
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
      action = new Action().getFromAttributes(siteId, attrs);
    }

    return action;
  }

  @Override
  public PaginationResults<Action> findDocumentsInQueue(final String siteId, final String queueName,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    BatchGetConfig batchConfig = new BatchGetConfig();
    String pk = createDatabaseKey(siteId, "action#" + ActionType.QUEUE + "#" + queueName);
    String sk = "action#";

    QueryConfig config = new QueryConfig().indexName(GSI1).scanIndexForward(Boolean.TRUE);
    QueryResponse response =
        this.db.queryBeginsWith(config, fromS(pk), fromS(sk), exclusiveStartKey, limit);

    List<Map<String, AttributeValue>> keys = response.items().stream()
        .map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).collect(Collectors.toList());

    List<Action> list = this.db.getBatch(batchConfig, keys).stream()
        .map(a -> new Action().getFromAttributes(siteId, a)).collect(Collectors.toList());

    PaginationMapToken pagination = new QueryResponseToPagination().apply(response);
    return new PaginationResults<>(list, pagination);
  }

  @Override
  public PaginationResults<String> findDocumentsWithStatus(final String siteId,
      final ActionStatus status, final Map<String, AttributeValue> exclusiveStartKey,
      final int limit) {

    Action a = new Action().status(status);
    String pk = a.pkGsi2(siteId);
    String sk = "action#";

    PaginationMapToken pagination = null;
    List<String> list = Collections.emptyList();

    if (pk != null) {

      QueryConfig config = new QueryConfig().indexName(GSI2).scanIndexForward(Boolean.TRUE);
      QueryResponse response =
          this.db.queryBeginsWith(config, fromS(pk), fromS(sk), exclusiveStartKey, limit);

      list =
          response.items().stream().map(i -> i.get("documentId").s()).collect(Collectors.toList());
      pagination = new QueryResponseToPagination().apply(response);
    }

    return new PaginationResults<>(list, pagination);

  }

  @Override
  public List<Action> getActions(final String siteId, final String documentId) {
    return queryActions(siteId, documentId, null, null);
  }

  @Override
  public boolean hasActions(final String siteId, final String documentId) {
    List<Action> actions = queryActions(siteId, documentId, Arrays.asList(PK), null);
    return !actions.isEmpty();
  }

  @Override
  public void insertBeforeAction(final String siteId, final String documentId,
      final List<Action> actions, final Action currentAction, final Action insertedAction) {

    int pos = actions.indexOf(currentAction);

    saveAction(siteId, documentId, insertedAction, pos);

    for (int i = pos; i < actions.size(); i++) {

      Action action = actions.get(i);
      deleteAction(siteId, action);

      pos++;
      saveAction(siteId, documentId, action, pos);
    }
  }

  /**
   * Query Document Actions.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param projectionExpression {@link List} {@link String}
   * @param limit {@link Integer}
   * @return {@link List} {@link Action}
   */
  private List<Action> queryActions(final String siteId, final String documentId,
      final List<String> projectionExpression, final Integer limit) {

    String pk = new Action().documentId(documentId).pk(siteId);
    String sk = "action" + TAG_DELIMINATOR;

    String expression = PK + " = :pk and begins_with(" + SK + ", :sk)";
    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue.builder().s(pk).build(),
        ":sk", AttributeValue.builder().s(sk).build());

    Builder q = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(expression).expressionAttributeValues(values).limit(limit);

    if (!Objects.notNull(projectionExpression).isEmpty()) {

      Map<String, String> names = new HashMap<>();
      int i = 1;
      for (String p : projectionExpression) {
        names.put("#" + i, p);
        i++;
      }

      q = q.projectionExpression(String.join(",", names.keySet())).expressionAttributeNames(names);
    }

    QueryResponse result = this.dbClient.query(q.build());

    return result.items().stream().map(a -> new Action().getFromAttributes(siteId, a))
        .sorted(new ActionIndexComparator()).collect(Collectors.toList());
  }

  @Override
  public void saveAction(final String siteId, final Action action) {

    if (action.insertedDate() == null) {
      action.insertedDate(new Date());
    }

    Map<String, AttributeValue> valueMap = action.getAttributes(siteId);
    this.dbClient
        .putItem(PutItemRequest.builder().tableName(this.documentTableName).item(valueMap).build());
  }

  @Override
  public void saveAction(final String siteId, final String documentId, final Action action,
      final int index) {

    action.documentId(documentId);
    action.index("" + index);

    if (action.insertedDate() == null) {
      action.insertedDate(new Date());
    }

    Map<String, AttributeValue> valueMap = action.getAttributes(siteId);
    this.dbClient
        .putItem(PutItemRequest.builder().tableName(this.documentTableName).item(valueMap).build());
  }

  @Override
  public void saveActions(final String siteId, final List<Action> actions) {

    List<Map<String, AttributeValue>> values = new ArrayList<>();

    for (Action action : actions) {

      Map<String, AttributeValue> valueMap = action.getAttributes(siteId);
      values.add(valueMap);
    }

    if (!values.isEmpty()) {
      Map<String, Collection<WriteRequest>> items =
          new AttributeValuesToWriteRequests(this.documentTableName).apply(values);

      BatchWriteItemRequest batch = BatchWriteItemRequest.builder().requestItems(items).build();
      this.dbClient.batchWriteItem(batch);
    }
  }

  @Override
  public List<Map<String, AttributeValue>> saveNewActions(final String siteId,
      final String documentId, final List<Action> actions) {

    int idx = 0;

    List<Map<String, AttributeValue>> values = new ArrayList<>();

    for (Action action : actions) {

      action.documentId(documentId);
      action.index("" + idx);

      if (action.insertedDate() == null) {
        action.insertedDate(new Date());
      }

      Map<String, AttributeValue> valueMap = action.getAttributes(siteId);

      values.add(valueMap);
      idx++;
    }

    if (!values.isEmpty()) {
      Map<String, Collection<WriteRequest>> items =
          new AttributeValuesToWriteRequests(this.documentTableName).apply(values);

      BatchWriteItemRequest batch = BatchWriteItemRequest.builder().requestItems(items).build();
      this.dbClient.batchWriteItem(batch);
    }

    return values;
  }

  @Override
  public void updateActionStatus(final String siteId, final String documentId,
      final Action action) {

    if (ActionStatus.COMPLETE.equals(action.status())
        || ActionStatus.FAILED.equals(action.status())) {
      action.completedDate(new Date());
    }

    Map<String, AttributeValue> attrs = action.getAttributes(siteId);

    Map<String, AttributeValueUpdate> updates = new HashMap<>();
    updates.put("status", AttributeValueUpdate.builder().value(attrs.get("status")).build());

    if (ActionStatus.RUNNING.equals(action.status())) {
      SimpleDateFormat df = DateUtil.getIsoDateFormatter();
      updates.put("startDate",
          AttributeValueUpdate.builder().value(fromS(df.format(new Date()))).build());
    }

    if (action.message() != null) {
      updates.put("message", AttributeValueUpdate.builder().value(attrs.get("message")).build());
    }

    if (action.completedDate() != null) {
      updates.put("completedDate",
          AttributeValueUpdate.builder().value(attrs.get("completedDate")).build());
    }

    for (String index : Arrays.asList(GSI1, GSI2)) {

      if (attrs.containsKey(index + PK) && attrs.containsKey(index + SK)) {
        updates.put(index + PK,
            AttributeValueUpdate.builder().value(attrs.get(index + PK)).build());
        updates.put(index + SK,
            AttributeValueUpdate.builder().value(attrs.get(index + SK)).build());
      } else {
        updates.put(index + PK,
            AttributeValueUpdate.builder().action(AttributeAction.DELETE).build());
        updates.put(index + SK,
            AttributeValueUpdate.builder().action(AttributeAction.DELETE).build());
      }
    }

    this.db.updateItem(attrs.get(PK), attrs.get(SK), updates);
  }

  @Override
  public void updateDocumentWorkflowStatus(final String siteId, final String documentId,
      final Action action) {
    String workflowId = action.workflowId();
    String stepId = action.workflowStepId();

    String status = !isEmpty(action.workflowLastStep()) ? "COMPLETE" : "IN_PROGRESS";

    if (ActionStatus.FAILED.equals(action.status())) {
      status = "FAILED";
    }

    DocumentWorkflowRecord r =
        new DocumentWorkflowRecord().documentId(documentId).workflowId(workflowId);

    Map<String, AttributeValue> attrs =
        this.db.get(AttributeValue.fromS(r.pk(siteId)), AttributeValue.fromS(r.sk()));

    r = new DocumentWorkflowRecord().getFromAttributes(siteId, attrs).status(status)
        .currentStepId(stepId).actionPk(action.pk(siteId)).actionSk(action.sk());

    this.db.putItem(r.getAttributes(siteId));
  }
}
