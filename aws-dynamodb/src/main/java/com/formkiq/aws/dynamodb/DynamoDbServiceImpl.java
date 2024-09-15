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
package com.formkiq.aws.dynamodb;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * Implementation of {@link DynamoDbService}.
 *
 */
public final class DynamoDbServiceImpl implements DynamoDbService {

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** Table Name. */
  private final String tableName;

  /**
   * constructor.
   * 
   * @param client {@link DynamoDbClient}
   * @param dynamoDbTableName {@link String}
   */
  public DynamoDbServiceImpl(final DynamoDbClient client, final String dynamoDbTableName) {
    if (dynamoDbTableName == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = client;
    this.tableName = dynamoDbTableName;
  }

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param dynamoDbTableName {@link String}
   */
  public DynamoDbServiceImpl(final DynamoDbConnectionBuilder connection,
      final String dynamoDbTableName) {
    if (dynamoDbTableName == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = connection.build();
    this.tableName = dynamoDbTableName;
  }

  @Override
  public QueryResponse between(final QueryConfig config, final AttributeValue pk,
      final AttributeValue skStart, final AttributeValue skEnd,
      final Map<String, AttributeValue> startkey, final int limit) {

    String gsi = Strings.isEmpty(config.indexName()) ? "" : config.indexName();

    String expression = gsi + PK + " = :pk and " + gsi + SK + " between :start and :end";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", pk);
    values.put(":start", skStart);
    values.put(":end", skEnd);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
            .projectionExpression(config.projectionExpression()).indexName(config.indexName())
            .exclusiveStartKey(startkey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public boolean deleteItem(final AttributeValue pk, final AttributeValue sk) {
    Map<String, AttributeValue> sourceKey = Map.of(PK, pk, SK, sk);
    return deleteItem(sourceKey);
  }

  @Override
  public boolean deleteItem(final Map<String, AttributeValue> key) {
    DeleteItemResponse response = this.dbClient.deleteItem(DeleteItemRequest.builder()
        .tableName(this.tableName).key(key).returnValues(ReturnValue.ALL_OLD).build());
    return !response.attributes().isEmpty();
  }

  @Override
  public boolean deleteItems(final Collection<Map<String, AttributeValue>> attrs) {

    boolean deleted = false;

    if (!attrs.isEmpty()) {

      List<WriteRequest> writes = attrs.stream().map(
          a -> WriteRequest.builder().deleteRequest(DeleteRequest.builder().key(a).build()).build())
          .collect(Collectors.toList());

      WriteRequestBuilder builder = new WriteRequestBuilder().append(this.tableName, writes);
      deleted = builder.batchWriteItem(this.dbClient);
    }

    return deleted;
  }

  @Override
  public boolean deleteItemsBeginsWith(final AttributeValue pk, final AttributeValue sk) {

    final int limit = 100;
    Map<String, AttributeValue> startkey = null;
    List<Map<String, AttributeValue>> list = new ArrayList<>();
    QueryConfig config = new QueryConfig().projectionExpression("PK,SK");

    do {

      QueryResponse response = queryBeginsWith(config, pk, sk, startkey, limit);

      List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    return deleteItems(list);
  }

  @Override
  public boolean deleteItemsBeginsWith(final String indexName, final AttributeValue pk) {

    final int limit = 100;
    List<Map<String, AttributeValue>> list = new ArrayList<>();

    Map<String, AttributeValue> expressionAttributeValues = Map.of(":pkValue", pk);

    String prefix = indexName != null ? indexName : "";
    String filterExpression = "begins_with(" + prefix + "PK, :pkValue)";
    ScanRequest.Builder scanRequest = ScanRequest.builder().tableName(this.tableName).indexName(indexName).limit(limit)
        .filterExpression(filterExpression)
        .expressionAttributeValues(expressionAttributeValues).projectionExpression("PK,SK");

    Map<String, AttributeValue> startkey;

    do {

      ScanResponse response = this.dbClient.scan(scanRequest.build());

      List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();
      scanRequest.exclusiveStartKey(response.lastEvaluatedKey());

    } while (startkey != null && !startkey.isEmpty());

    return deleteItems(list);
  }

  @Override
  public boolean exists(final AttributeValue pk, final AttributeValue sk) {
    GetItemRequest r = GetItemRequest.builder().key(Map.of(PK, pk, SK, sk))
        .tableName(this.tableName).projectionExpression("PK").consistentRead(Boolean.TRUE).build();
    GetItemResponse response = this.dbClient.getItem(r);
    return !response.item().isEmpty();
  }

  @Override
  public Map<String, AttributeValue> get(final AttributeValue pk, final AttributeValue sk) {
    return get(new QueryConfig(), pk, sk);
  }

  @Override
  public Map<String, AttributeValue> get(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk) {
    Map<String, AttributeValue> key = Map.of(PK, pk, SK, sk);
    return this.dbClient.getItem(GetItemRequest.builder().tableName(this.tableName).key(key)
        .projectionExpression(config.projectionExpression())
        .expressionAttributeNames(config.expressionAttributeNames()).consistentRead(Boolean.TRUE)
        .build()).item();
  }

  @Override
  public List<Map<String, AttributeValue>> getBatch(final BatchGetConfig config,
      final List<Map<String, AttributeValue>> attributes) {

    List<Map<String, AttributeValue>> keys =
        attributes.stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    List<Map<String, AttributeValue>> list = Collections.emptyList();

    if (!keys.isEmpty()) {

      ReadRequestBuilder builder = new ReadRequestBuilder();
      builder.append(this.tableName, keys);

      Map<String, List<Map<String, AttributeValue>>> batchReadItems =
          builder.batchReadItems(this.dbClient, config);

      list = batchReadItems.get(this.tableName);

      Map<String, Map<String, AttributeValue>> data =
          list.stream().collect(Collectors.toMap(this::getKey, l -> l));

      list = keys.stream().map(k -> data.get(getKey(k))).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    return list;
  }

  private String getKey(final Map<String, AttributeValue> attr) {
    return attr.get(PK).s() + "#" + attr.get(SK).s();
  }

  @Override
  public String getNextNumber(final Map<String, AttributeValue> keys) {

    UpdateItemRequest updateItemRequest = UpdateItemRequest.builder().tableName(this.tableName)
        .key(keys).updateExpression("ADD #autoIncrement :val")
        .expressionAttributeNames(Map.of("#autoIncrement", "Number"))
        .expressionAttributeValues(Map.of(":val", AttributeValue.builder().n("1").build()))
        .returnValues(ReturnValue.UPDATED_NEW).build();

    UpdateItemResponse response = updateItem(updateItemRequest);

    AttributeValue val = response.attributes().get("Number");
    return val.n();
  }

  @Override
  public String getTableName() {
    return this.tableName;
  }

  @Override
  public boolean moveItems(final Collection<Map<String, AttributeValue>> attrs,
      final MoveAttributeFunction func) {

    List<WriteRequest> writes = new ArrayList<>();

    for (Map<String, AttributeValue> attr : attrs) {

      Map<String, AttributeValue> newAttr = func.transform(attr);

      Map<String, AttributeValue> key = Map.of(PK, attr.get(PK), SK, attr.get(SK));
      WriteRequest del =
          WriteRequest.builder().deleteRequest(DeleteRequest.builder().key(key).build()).build();
      writes.add(del);

      WriteRequest add =
          WriteRequest.builder().putRequest(PutRequest.builder().item(newAttr).build()).build();
      writes.add(add);
    }

    WriteRequestBuilder builder = new WriteRequestBuilder().append(this.tableName, writes);
    return builder.batchWriteItem(this.dbClient);
  }

  @Override
  public void putItem(final Map<String, AttributeValue> attributes) {
    putItem(this.tableName, attributes);
  }

  /**
   * Put Item in DynamoDb.
   * 
   * @param dynamoDbTable {@link String}
   * @param attributes {@link Map}
   */
  private void putItem(final String dynamoDbTable, final Map<String, AttributeValue> attributes) {
    this.dbClient
        .putItem(PutItemRequest.builder().tableName(dynamoDbTable).item(attributes).build());
  }

  @Override
  public void putItems(final List<Map<String, AttributeValue>> attrs) {

    if (!attrs.isEmpty()) {
      WriteRequestBuilder builder = new WriteRequestBuilder();
      for (Map<String, AttributeValue> attr : attrs) {
        builder.append(this.tableName, attr);
      }
      builder.batchWriteItem(this.dbClient);
    }
  }

  @Override
  public QueryResponse query(final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.FALSE);
    return query(config, pk, exclusiveStartKey, limit);
  }

  @Override
  public QueryResponse query(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk, final Map<String, AttributeValue> exclusiveStartKey,
      final int limit) {

    String gsi = Strings.isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk and " + gsi + SK + " = :sk";
    Map<String, AttributeValue> values = Map.of(":pk", pk, ":sk", sk);

    QueryRequest q = QueryRequest.builder().tableName(this.tableName).indexName(config.indexName())
        .expressionAttributeNames(config.expressionAttributeNames())
        .keyConditionExpression(expression).projectionExpression(config.projectionExpression())
        .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
        .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse query(final QueryConfig config, final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String gsi = Strings.isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);

    QueryRequest q = QueryRequest.builder().tableName(this.tableName).indexName(config.indexName())
        .expressionAttributeNames(config.expressionAttributeNames())
        .keyConditionExpression(expression).projectionExpression(config.projectionExpression())
        .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
        .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse queryBeginsWith(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk, final Map<String, AttributeValue> exclusiveStartKey,
      final int limit) {

    String gsi = Strings.isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk and begins_with(" + gsi + SK + ",:sk)";

    if (sk == null) {
      expression = gsi + PK + " = :pk";
    }

    Map<String, AttributeValue> values =
        sk != null ? Map.of(":pk", pk, ":sk", sk) : Map.of(":pk", pk);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
            .projectionExpression(config.projectionExpression()).indexName(config.indexName())
            .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse queryIndex(final String indexName, final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String expression = indexName + PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(Boolean.FALSE).indexName(indexName)
            .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public Map<String, AttributeValue> updateItem(final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValueUpdate> updateValues) {
    Map<String, AttributeValue> dbKey = Map.of(PK, pk, SK, sk);
    return this.dbClient.updateItem(UpdateItemRequest.builder().tableName(this.tableName).key(dbKey)
        .attributeUpdates(updateValues).build()).attributes();
  }

  @Override
  public UpdateItemResponse updateItem(final UpdateItemRequest request) {
    return this.dbClient.updateItem(request);
  }

  @Override
  public Map<String, AttributeValue> updateValues(final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValue> updateValues) {

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    updateValues.forEach(
        (key, value) -> values.put(key, AttributeValueUpdate.builder().value(value).build()));

    return updateItem(pk, sk, values);
  }
}
