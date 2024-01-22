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
import java.util.Map.Entry;
import java.util.Set;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Helper class for building {@link WriteRequest}.
 *
 */
public class WriteRequestBuilder {

  /** Max Batch Size. */
  private static final int MAX_BATCH_SIZE = 25;
  /** {@link Map} of {@link WriteRequest}. */
  private Map<String, List<WriteRequest>> items = new HashMap<>();

  /**
   * constructor.
   */
  public WriteRequestBuilder() {

  }

  /**
   * Collects {@link WriteRequest} and adds to a internal list.
   * 
   * @param writes {@link Map} {@link WriteRequest}
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder append(final Map<String, WriteRequest> writes) {

    for (Map.Entry<String, WriteRequest> e : writes.entrySet()) {
      if (this.items.containsKey(e.getKey())) {
        this.items.get(e.getKey()).add(e.getValue());
      } else {
        List<WriteRequest> list = new ArrayList<>();
        list.add(e.getValue());
        this.items.put(e.getKey(), list);
      }
    }

    return this;
  }

  /**
   * Append {@link WriteRequest}.
   * 
   * @param tableName {@link String}
   * @param writes {@link List} {@link WriteRequest}
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder append(final String tableName, final List<WriteRequest> writes) {
    this.items.put(tableName, writes);
    return this;
  }

  /**
   * Append {@link Map} {@link AttributeValue}.
   * 
   * @param tableName {@link String}
   * @param values {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder append(final String tableName,
      final Map<String, AttributeValue> values) {
    Map<String, WriteRequest> writes = new AttributeValueToWriteRequest(tableName).apply(values);
    append(writes);
    return this;
  }

  /**
   * Collects {@link WriteRequest} and adds to a internal list.
   * 
   * @param writes {@link Map} {@link WriteRequest}
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder appends(final Map<String, List<WriteRequest>> writes) {

    for (Entry<String, List<WriteRequest>> e : writes.entrySet()) {
      if (this.items.containsKey(e.getKey())) {
        this.items.get(e.getKey()).addAll(e.getValue());
      } else {
        this.items.put(e.getKey(), e.getValue());
      }
    }

    return this;
  }

  /**
   * Append List {@link Map} {@link AttributeValue}.
   * 
   * @param tableName {@link String}
   * @param values {@link Collection} {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder appends(final String tableName,
      final Collection<Map<String, AttributeValue>> values) {

    AttributeValueToWriteRequest convert = new AttributeValueToWriteRequest(tableName);
    for (Map<String, AttributeValue> value : values) {
      Map<String, WriteRequest> writes = convert.apply(value);
      append(writes);
    }

    return this;
  }

  /**
   * Batch Write Items.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @return boolean
   */
  public boolean batchWriteItem(final DynamoDbClient dbClient) {

    boolean write = false;

    Map<String, List<WriteRequest>> map = new HashMap<>(getItems());

    for (Map.Entry<String, List<WriteRequest>> e : map.entrySet()) {

      Set<WriteRequest> wrs = new HashSet<>(e.getValue());
      List<List<WriteRequest>> parition = Objects.parition(new ArrayList<>(wrs), MAX_BATCH_SIZE);

      for (List<WriteRequest> writelist : parition) {

        if (!writelist.isEmpty()) {
          BatchWriteItemRequest batch =
              BatchWriteItemRequest.builder().requestItems(Map.of(e.getKey(), writelist)).build();
          dbClient.batchWriteItem(batch);

          write = true;
        }
      }
    }

    return write;
  }

  /**
   * Get {@link WriteRequest} Items.
   * 
   * @return {@link Map}
   */
  public Map<String, List<WriteRequest>> getItems() {
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
