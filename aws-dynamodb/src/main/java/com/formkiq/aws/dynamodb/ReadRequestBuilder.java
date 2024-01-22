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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Helper class for building Batch Read Requets.
 *
 */
public class ReadRequestBuilder {

  /** Max Batch Size. */
  private static final int MAX_BATCH_SIZE = 100;
  /** {@link Map} of {@link AttributeValue}. */
  private Map<String, Collection<Map<String, AttributeValue>>> items = new HashMap<>();

  /**
   * constructor.
   */
  public ReadRequestBuilder() {

  }

  /**
   * Collects {@link WriteRequest} and adds to a internal list.
   * 
   * @param tableName {@link String}
   * @param keys {@link Collection}
   * @return {@link ReadRequestBuilder}
   */
  public ReadRequestBuilder append(final String tableName,
      final Collection<Map<String, AttributeValue>> keys) {

    if (this.items.containsKey(tableName)) {
      Collection<Map<String, AttributeValue>> values = this.items.get(tableName);
      values.addAll(keys);
    } else {
      this.items.put(tableName, new HashSet<>(keys));
    }

    return this;
  }

  /**
   * Batch Read Items.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @param config {@link BatchGetConfig}
   * @return {@link BatchGetItemResponse}
   */
  public Map<String, List<Map<String, AttributeValue>>> batchReadItems(
      final DynamoDbClient dbClient, final BatchGetConfig config) {

    Map<String, List<Map<String, AttributeValue>>> map = new HashMap<>();

    for (Map.Entry<String, Collection<Map<String, AttributeValue>>> e : this.items.entrySet()) {

      if (e.getValue().size() > MAX_BATCH_SIZE) {

        Collection<List<Map<String, AttributeValue>>> parition =
            Objects.parition(new ArrayList<>(e.getValue()), MAX_BATCH_SIZE);

        for (List<Map<String, AttributeValue>> list : parition) {

          Map<String, List<Map<String, AttributeValue>>> values =
              batchReadItems(dbClient, e.getKey(), config, list).responses();

          for (Map.Entry<String, List<Map<String, AttributeValue>>> ee : values.entrySet()) {

            if (map.containsKey(ee.getKey())) {
              map.get(ee.getKey()).addAll(ee.getValue());
            } else {
              map.put(ee.getKey(), new ArrayList<>(ee.getValue()));
            }
          }
        }

      } else {

        BatchGetItemResponse response = batchReadItems(dbClient, e.getKey(), config, e.getValue());
        if (response != null) {
          map = response.responses();
        } else {
          map.put(e.getKey(), new ArrayList<>());
        }
      }
    }

    return map;
  }

  private BatchGetItemResponse batchReadItems(final DynamoDbClient dbClient, final String tableName,
      final BatchGetConfig config, final Collection<Map<String, AttributeValue>> keys) {

    BatchGetItemResponse batchResponse = null;

    if (!keys.isEmpty()) {
      Map<String, KeysAndAttributes> requestedItems = Map.of(tableName,
          KeysAndAttributes.builder().keys(keys).projectionExpression(config.projectionExpression())
              .expressionAttributeNames(config.expressionAttributeNames()).build());

      BatchGetItemRequest batchReq =
          BatchGetItemRequest.builder().requestItems(requestedItems).build();
      batchResponse = dbClient.batchGetItem(batchReq);
    }

    return batchResponse;
  }

  /**
   * Get Read Items.
   * 
   * @return {@link Map}
   */
  public Map<String, Collection<Map<String, AttributeValue>>> getItems() {
    return this.items;
  }

  /**
   * Are there any requests.
   * 
   * @return boolean
   */
  public boolean isEmpty() {
    return this.items.isEmpty();
  }
}
