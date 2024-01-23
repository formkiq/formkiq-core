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
   * Delete DynamoDb Record.
   * 
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItem(AttributeValue pk, AttributeValue sk);

  /**
   * Delete Items.
   * 
   * @param attrs {@link Collection} {@link Map} {@link AttributeValue}
   * @return boolean
   */
  boolean deleteItems(Collection<Map<String, AttributeValue>> attrs);

  /**
   * Whether Database Record Exists.
   * 
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return boolean
   */
  boolean exists(AttributeValue pk, AttributeValue sk);

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
   * @param config {@link QueryConfig}
   * @param pk {@link AttributeValue}
   * @param sk {@link AttributeValue}
   * @return {@link Map}
   */
  Map<String, AttributeValue> get(QueryConfig config, AttributeValue pk, AttributeValue sk);

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
   * 
   * @return {@link String}
   */
  String getNextNumber(Map<String, AttributeValue> keys);

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
   */
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
}
