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
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
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
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * Implementation of {@link DynamoDbService}.
 *
 */
public class DynamoDbServiceImpl implements DynamoDbService {

  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;
  /** Table Name. */
  private String tableName;

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
  public boolean deleteItem(final AttributeValue pk, final AttributeValue sk) {
    Map<String, AttributeValue> sourceKey = Map.of(PK, pk, SK, sk);
    DeleteItemResponse response = this.dbClient.deleteItem(DeleteItemRequest.builder()
        .tableName(this.tableName).key(sourceKey).returnValues(ReturnValue.ALL_OLD).build());
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
      final List<Map<String, AttributeValue>> keys) {

    List<Map<String, AttributeValue>> list = Collections.emptyList();

    if (!keys.isEmpty()) {

      ReadRequestBuilder builder = new ReadRequestBuilder();
      builder.append(this.tableName, keys);

      Map<String, List<Map<String, AttributeValue>>> batchReadItems =
          builder.batchReadItems(this.dbClient, config);

      list = batchReadItems.get(this.tableName);

      Map<String, Map<String, AttributeValue>> data =
          list.stream().collect(Collectors.toMap(l -> getKey(l), l -> l));

      list = keys.stream().map(k -> data.get(getKey(k))).filter(k -> k != null)
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
      Map<String, Collection<WriteRequest>> items =
          new AttributeValuesToWriteRequests(this.tableName).apply(attrs);

      BatchWriteItemRequest batch = BatchWriteItemRequest.builder().requestItems(items).build();
      this.dbClient.batchWriteItem(batch);
    }
  }

  @Override
  public QueryResponse query(final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {
    String expression = PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);
    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(Boolean.FALSE)
            .exclusiveStartKey(exclusiveStartKey).limit(Integer.valueOf(limit)).build();

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
            .exclusiveStartKey(exclusiveStartKey).limit(Integer.valueOf(limit)).build();

    QueryResponse response = this.dbClient.query(q);
    return response;
  }

  @Override
  public QueryResponse queryIndex(final String indexName, final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String expression = indexName + PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(Boolean.FALSE).indexName(indexName)
            .exclusiveStartKey(exclusiveStartKey).limit(Integer.valueOf(limit)).build();

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
    updateValues.forEach((key, value) -> {
      values.put(key, AttributeValueUpdate.builder().value(value).build());
    });

    return updateItem(pk, sk, values);
  }
}
