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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

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
  public void deleteItem(final AttributeValue pk, final AttributeValue sk) {
    Map<String, AttributeValue> sourceKey = Map.of(PK, pk, SK, sk);
    this.dbClient
        .deleteItem(DeleteItemRequest.builder().tableName(this.tableName).key(sourceKey).build());
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
    Map<String, AttributeValue> key = Map.of(PK, pk, SK, sk);
    return this.dbClient.getItem(GetItemRequest.builder().tableName(this.tableName).key(key)
        .consistentRead(Boolean.TRUE).build()).item();
  }

  @Override
  public List<Map<String, AttributeValue>> getBatch(
      final Collection<Map<String, AttributeValue>> keys) {

    Map<String, KeysAndAttributes> items =
        Map.of(this.tableName, KeysAndAttributes.builder().keys(keys).build());

    BatchGetItemResponse response =
        this.dbClient.batchGetItem(BatchGetItemRequest.builder().requestItems(items).build());

    List<Map<String, AttributeValue>> list = response.responses().get(this.tableName);
    return list;
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

    String expression = PK + " = :pk and begins_with(" + SK + ",:sk)";

    Map<String, AttributeValue> values = Map.of(":pk", pk, ":sk", sk);

    QueryRequest q = QueryRequest.builder().tableName(this.tableName)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .scanIndexForward(Boolean.FALSE).projectionExpression(config.projectionExpression())
        .exclusiveStartKey(exclusiveStartKey).limit(Integer.valueOf(limit)).build();

    QueryResponse response = this.dbClient.query(q);
    return response;
  }

  @Override
  public Map<String, AttributeValue> updateFields(final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValue> updateValues) {
    return updateFields(this.tableName, pk, sk, updateValues);
  }

  private Map<String, AttributeValue> updateFields(final String dynamoDbTable,
      final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValue> updateValues) {

    Map<String, AttributeValue> dbKey = Map.of(PK, pk, SK, sk);

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    updateValues.forEach((key, value) -> {
      values.put(key, AttributeValueUpdate.builder().value(value).build());
    });

    return this.dbClient.updateItem(UpdateItemRequest.builder().tableName(dynamoDbTable).key(dbKey)
        .attributeUpdates(values).build()).attributes();
  }
}
