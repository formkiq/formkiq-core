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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.actions.ActionType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * DynamoDB implemenation of {@link ActionsService}.
 *
 */
public class ActionsServiceDynamoDb implements ActionsService, DbKeys {

  /** Document Table Name. */
  private String documentTableName;

  /**
   * constructor.
   *
   * @param documentsTable {@link String}
   */
  public ActionsServiceDynamoDb(final String documentsTable) {

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.documentTableName = documentsTable;
  }

  @Override
  public Map<String, String> getActionParameters(final DynamoDbClient client, final String siteId,
      final String documentId, final ActionType type) {

    List<Action> actions = Objects.notNull(
        queryActions(client, siteId, documentId, Arrays.asList("type", "parameters"), null));

    Optional<Action> op = actions.stream().filter(a -> a.type().equals(type)).findFirst();
    return op.isPresent() ? op.get().parameters() : null;
  }

  @Override
  public List<Action> getActions(final DynamoDbClient client, final String siteId,
      final String documentId) {
    return queryActions(client, siteId, documentId, null, null);
  }

  /**
   * Get Pk.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @return {@link String}
   */
  private String getPk(final String siteId, final String documentId) {
    return createDatabaseKey(siteId, PREFIX_DOCS + documentId);
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
  public boolean hasActions(final DynamoDbClient client, final String siteId,
      final String documentId) {
    List<Action> actions = queryActions(client, siteId, documentId, Arrays.asList(PK), null);
    return !actions.isEmpty();
  }

  /**
   * Query Document Actions.
   * 
   * @param client {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param projectionExpression {@link List} {@link String}
   * @param limit {@link Integer}
   * @return {@link List} {@link Action}
   */
  private List<Action> queryActions(final DynamoDbClient client, final String siteId,
      final String documentId, final List<String> projectionExpression, final Integer limit) {

    String pk = getPk(siteId, documentId);
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

    QueryResponse result = client.query(q.build());

    AttributeValueToAction transform = new AttributeValueToAction();
    return result.items().stream().map(r -> transform.apply(r)).collect(Collectors.toList());
  }

  @Override
  public List<Map<String, AttributeValue>> saveActions(final DynamoDbClient client,
      final String siteId, final String documentId, final List<Action> actions) {

    int idx = 0;

    List<Map<String, AttributeValue>> values = new ArrayList<>();

    for (Action action : actions) {

      Map<String, AttributeValue> valueMap = new HashMap<>();

      String pk = getPk(siteId, documentId);
      String sk = getSk(action, idx);
      addS(valueMap, PK, pk);
      addS(valueMap, SK, sk);
      addS(valueMap, "type", action.type().name());
      addS(valueMap, "status", action.status().name());
      addS(valueMap, "documentId", documentId);
      addS(valueMap, "userId", action.userId());
      addM(valueMap, "parameters", action.parameters());

      values.add(valueMap);
      idx++;
    }

    List<WriteRequest> list = new ArrayList<>();

    values.forEach(value -> {
      PutRequest put = PutRequest.builder().item(value).build();
      WriteRequest req = WriteRequest.builder().putRequest(put).build();
      list.add(req);
    });

    Map<String, Collection<WriteRequest>> items = Map.of(this.documentTableName, list);

    BatchWriteItemRequest batch = BatchWriteItemRequest.builder().requestItems(items).build();
    client.batchWriteItem(batch);

    return values;
  }

  @Override
  public void updateActionStatus(final DynamoDbClient client, final String siteId,
      final String documentId, final Action action, final int index) {

    String pk = getPk(siteId, documentId);
    String sk = getSk(action, index);

    Map<String, AttributeValue> key = Map.of(PK, AttributeValue.builder().s(pk).build(), SK,
        AttributeValue.builder().s(sk).build());

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    values.put("status", AttributeValueUpdate.builder()
        .value(AttributeValue.builder().s(action.status().name()).build()).build());

    client.updateItem(UpdateItemRequest.builder().tableName(this.documentTableName).key(key)
        .attributeUpdates(values).build());
  }

  @Override
  public List<Action> updateActionStatus(final DynamoDbClient client, final String siteId,
      final String documentId, final ActionType type, final ActionStatus status) {

    int idx = 0;
    NextActionPredicate pred = new NextActionPredicate();
    List<Action> actionlist = getActions(client, siteId, documentId);
    for (Action action : actionlist) {
      if (pred.test(action) && action.type().equals(type)) {
        action.status(status);
        updateActionStatus(client, siteId, documentId, action, idx);
        break;
      }
      idx++;
    }

    return actionlist;
  }
}
