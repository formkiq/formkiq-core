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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 *
 * DynamoDB Wrapper Service.
 *
 */
public interface DynamoDbService {

  /**
   * Query Between values.
   *
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param skStart {@link AttributeValue}
   * @param skEnd {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   */
  QueryResponse between(QueryConfig config, AttributeValue pk, AttributeValue skStart,
      AttributeValue skEnd, Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Delete DynamoDb Record.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItem(AttributeValue pk, AttributeValue sk);

  /**
   * Delete DynamoDb Record.
   *
   * @param key {@link Map}
   * @return boolean
   */
  boolean deleteItem(Map<String, AttributeValue> key);

  /**
   * Delete DynamoDb Record.
   *
   * @param key {@link DynamoDbKey}
   * @return boolean
   */
  boolean deleteItem(DynamoDbKey key);

  /**
   * Delete Items.
   *
   * @param attrs {@link Collection} {@link Map} {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItems(Collection<Map<String, AttributeValue>> attrs);

  /**
   * Delete Items.
   *
   * @param tableName {@link String}
   * @param keys {@link Collection} {@link DynamoDbKey}
   * @return boolean
   */
  boolean deleteItems(String tableName, Collection<DynamoDbKey> keys);

  /**
   * Delete all records that beginsWith SK.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItemsBeginsWith(AttributeValue pk, AttributeValue sk);

  /**
   * Delete all records that beginsWith PK.
   *
   * @param indexName {@link String}
   * @param pk {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItemsBeginsWith(String indexName, AttributeValue pk);

  /**
   * Whether Database Record Exist.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return boolean
   */
  boolean exists(AttributeValue pk, AttributeValue sk);

  /**
   * Whether Database Record Exist.
   *
   * @param key {@link DynamoDbKey}
   * @return boolean
   */
  boolean exists(DynamoDbKey key);

  /**
   * Returns {@link DynamoDbKey} that exist.
   *
   * @param key {@link DynamoDbKey}
   * @return {@link Collection} {@link DynamoDbKey}
   */
  Collection<DynamoDbKey> exists(Collection<DynamoDbKey> key);

  /**
   * Whether the {@link QueryRequest} returns a result.
   *
   * @param query {@link QueryRequest}
   * @return boolean
   */
  boolean exists(QueryRequest query);

  /**
   * Gets DynamoDB Record.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return {@link Map}
   */
  Map<String, AttributeValue> get(AttributeValue pk, AttributeValue sk);

  /**
   * Gets DynamoDB Record.
   *
   * @param key {@link DynamoDbKey}
   * @return {@link Map}
   */
  Map<String, AttributeValue> get(DynamoDbKey key);

  /**
   * Gets DynamoDB Record.
   *
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return {@link Map}
   */
  Map<String, AttributeValue> get(QueryConfig config, AttributeValue pk, AttributeValue sk);

  /**
   * Get Single Result from a Query.
   *
   * @param query {@link QueryRequest}
   * @return Map
   */
  Map<String, AttributeValue> getByQuery(QueryRequest query);

  /**
   * Batch Get a number of Keys.
   *
   * @param config {@link BatchGetConfig}
   * @param keys {@link List}
   * @return {@link List}
   */
  List<Map<String, AttributeValue>> getBatch(BatchGetConfig config,
      List<Map<String, AttributeValue>> keys);

  /**
   * Get Next Number.
   *
   * @param keys {@link Map}
   * @return {@link String}
   */
  Long getNextNumber(Map<String, AttributeValue> keys);

  /**
   * Get Table Name.
   *
   * @return {@link String}
   */
  String getTableName();

  /**
   * Move Records.
   *
   * @param attrs {@link Collection} {@link Map}
   * @param func {@link MoveAttributeFunction}
   * @return boolean
   */
  boolean moveItems(Collection<Map<String, AttributeValue>> attrs, MoveAttributeFunction func);

  /**
   * Put DynamoDb Record.
   *
   * @param attr {@link Map} {@link AttributeValue}
   */
  void putItem(Map<String, AttributeValue> attr);

  /**
   * Put DynamoDb Record.
   *
   * @param tableName {@link String}
   * @param attr {@link Map} {@link AttributeValue}
   */
  void putItem(String tableName, Map<String, AttributeValue> attr);

  /**
   * Put DynamoDb Records.
   *
   * @param attrs {@link List} {@link Map} {@link AttributeValue}
   */
  void putItems(List<Map<String, AttributeValue>> attrs);

  /**
   * Query DynamoDB Records.
   *
   * @param pk {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   * @deprecated Use other 'query' with {@link QueryConfig}
   */
  @Deprecated
  QueryResponse query(AttributeValue pk, Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Query DynamoDB Records.
   *
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   */
  QueryResponse query(QueryConfig config, AttributeValue pk, AttributeValue sk,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Query DynamoDB Records.
   *
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   */
  QueryResponse query(QueryConfig config, AttributeValue pk,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Query DynamoDB records.
   *
   * @param q {@link QueryRequest}
   * @return QueryResponse
   */
  QueryResponse query(QueryRequest q);

  /**
   * Query DynamoDB records.
   *
   * @param q {@link QueryRequest}
   * @param fetchAllAttributes If using index, fetch all attributes from non-index
   * @return QueryResponse
   */
  QueryResponse query(QueryRequest q, boolean fetchAllAttributes);

  /**
   * Query DynamoDB Records.
   *
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   */
  QueryResponse queryBeginsWith(QueryConfig config, AttributeValue pk, AttributeValue sk,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Query DynamoDB Index for Records.
   *
   * @param indexName {@link String}
   * @param pk {@link AttributeValue}
   * @param exclusiveStartKey {@link Map}
   * @param limit int
   * @return {@link QueryResponse}
   */
  QueryResponse queryIndex(String indexName, AttributeValue pk,
      Map<String, AttributeValue> exclusiveStartKey, int limit);

  /**
   * Update DynamoDB Record.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @param updateValues {@link Map}
   * @return {@link Map}
   */
  Map<String, AttributeValue> updateItem(AttributeValue pk, AttributeValue sk,
      Map<String, AttributeValueUpdate> updateValues);

  /**
   * Update Item.
   *
   * @param request {@link UpdateItemRequest}
   * @return {@link UpdateItemResponse}
   */
  UpdateItemResponse updateItem(UpdateItemRequest request);

  /**
   * Update DynamoDB Record.
   *
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @param updateValues {@link Map}
   * @return {@link Map}
   */
  Map<String, AttributeValue> updateValues(AttributeValue pk, AttributeValue sk,
      Map<String, AttributeValue> updateValues);

  /**
   * Update DynamoDB Records.
   *
   * @param tableName {@link String}
   * @param keys {@link Collection} {@link DynamoDbKey}
   * @param updateValues {@link Map}
   */
  void updateItems(String tableName, Collection<DynamoDbKey> keys,
      Map<String, AttributeValueUpdate> updateValues);

  /**
   * Aquire Lock.
   *
   * @param key {@link DynamoDbKey}
   * @param aquireLockTimeoutInMs long
   * @param lockExpirationInMs long
   * @return boolean
   */
  boolean acquireLock(DynamoDbKey key, long aquireLockTimeoutInMs, long lockExpirationInMs);

  /**
   * Release Lock.
   *
   * @param key {@link DynamoDbKey}
   * @return boolean
   */
  boolean releaseLock(DynamoDbKey key);

  /**
   * Put in transaction.
   *
   * @param writeRequest {@link WriteRequestBuilder}
   * @deprecated use oth putInTransactio method.
   */
  @Deprecated
  void putInTransaction(WriteRequestBuilder writeRequest);

  /**
   * Put {@link Collection} {@link AttributeValue} in Transaction.
   *
   * @param attributes {@link Collection}
   */
  void putInTransaction(Collection<Map<String, AttributeValue>> attributes);

  /**
   * Get Lock.
   *
   * @param key {@link DynamoDbKey}
   * @return Map
   */
  Map<String, AttributeValue> getAquiredLock(DynamoDbKey key);
}
