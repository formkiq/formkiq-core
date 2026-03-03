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
import java.util.concurrent.TimeUnit;

import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Helper class for building {@link WriteRequest}.
 *
 */
public class WriteRequestBuilder {

  /** Max Retries. */
  private static final int MAX_RETRIES = 5;
  /** Max Batch Size. */
  private static final int MAX_BATCH_SIZE = 25;

  /** {@link Map} of {@link WriteRequest}. */
  private final Map<String, List<WriteRequest>> items = new HashMap<>();

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
   * Append a delete {@link WriteRequest} for the given table and key.
   *
   * @param tableName table name
   * @param key DynamoDB primary key map (PK/SK etc.)
   * @return this builder
   */
  public WriteRequestBuilder appendDelete(final String tableName, final DynamoDbKey key) {

    WriteRequest wr = WriteRequest.builder()
        .deleteRequest(DeleteRequest.builder().key(key.toMap()).build()).build();

    this.items.computeIfAbsent(tableName, k -> new ArrayList<>()).add(wr);
    return this;
  }

  /**
   * Append a delete {@link WriteRequest} for the given table and key.
   *
   * @param tableName table name
   * @param keys DynamoDB primary key map (PK/SK etc.)
   * @return this builder
   */
  public WriteRequestBuilder appendDeletes(final String tableName,
      final Collection<DynamoDbKey> keys) {
    keys.forEach(key -> appendDelete(tableName, key));
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

  private void backoffSleep(final int retries) {
    try {
      TimeUnit.SECONDS.sleep((long) Math.pow(2, retries));
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", ie);
    }
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

          batchWriteWithRetry(dbClient, Map.of(e.getKey(), writelist));
          write = true;
        }
      }
    }

    return write;
  }

  private void batchWriteWithRetry(final DynamoDbClient dbClient,
      final Map<String, List<WriteRequest>> requestItems) {
    int retries = 0;

    Map<String, List<WriteRequest>> toBeProcessed = requestItems;

    while (retries < MAX_RETRIES) {

      BatchWriteItemRequest batchWriteRequest =
          BatchWriteItemRequest.builder().requestItems(toBeProcessed).build();

      BatchWriteItemResponse response = dbClient.batchWriteItem(batchWriteRequest);
      Map<String, List<WriteRequest>> unprocessedItems = response.unprocessedItems();

      if (unprocessedItems.isEmpty()) {
        return;
      }

      toBeProcessed = unprocessedItems;

      backoffSleep(retries);

      retries++;
    }

    throw new RuntimeException("Some items could not be saved after retries.");
  }

  /**
   * Determine if a WriteRequest already exists for the given key.
   *
   * <p>
   * This checks both PutRequest and DeleteRequest entries.
   *
   * @param key DynamoDB primary key map (e.g., PK/SK)
   * @return true if a WriteRequest exists for this key
   */
  public boolean exists(final DynamoDbKey key) {

    java.util.Objects.requireNonNull(key, "key must not be null");

    final Map<String, AttributeValue> map = key.toMap();
    boolean found = false;

    for (List<WriteRequest> requests : this.items.values()) {
      for (WriteRequest wr : requests) {

        if (wr.deleteRequest() != null && map.equals(wr.deleteRequest().key())) {
          found = true;
        }

        if (!found && wr.putRequest() != null) {
          Map<String, AttributeValue> item = wr.putRequest().item();
          found = map.entrySet().stream().allMatch(e -> e.getValue().equals(item.get(e.getKey())));
        }

        if (found) {
          return true;
        }
      }
    }

    return false;
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

  /**
   * Convert a batch {@link WriteRequest} into a {@link TransactWriteItem}. Only Put and Delete are
   * supported (mirrors BatchWriteItem capabilities).
   *
   * @param tableName table name
   * @param wr write request
   * @return transact write item, or null if request has neither put nor delete
   */
  private TransactWriteItem toTransactWriteItem(final String tableName, final WriteRequest wr) {

    TransactWriteItem tx = null;

    if (wr.putRequest() != null) {
      tx = TransactWriteItem.builder()
          .put(Put.builder().tableName(tableName).item(wr.putRequest().item()).build()).build();
    } else if (wr.deleteRequest() != null) {
      tx = TransactWriteItem.builder()
          .delete(Delete.builder().tableName(tableName).key(wr.deleteRequest().key()).build())
          .build();
    }

    return tx;
  }

  /**
   * Execute the collected requests using DynamoDB {@code TransactWriteItems}.
   *
   * <p>
   * Each {@link WriteRequest} is converted to a {@link TransactWriteItem}:
   * <ul>
   * <li>{@code putRequest} -> {@code Put}</li>
   * <li>{@code deleteRequest} -> {@code Delete}</li>
   * </ul>
   *
   * <p>
   * Requests are chunked into transactions of up to {@value #MAX_BATCH_SIZE} items.
   *
   * @param dbClient dynamodb client
   * @return true if at least one transaction was executed
   */
  public boolean transactWriteItems(final DynamoDbClient dbClient) {

    boolean wrote = false;

    final List<TransactWriteItem> txnItems = new ArrayList<>();

    for (Entry<String, List<WriteRequest>> e : new HashMap<>(getItems()).entrySet()) {
      final String tableName = e.getKey();

      for (WriteRequest wr : e.getValue()) {
        TransactWriteItem t = toTransactWriteItem(tableName, wr);
        if (t != null) {
          txnItems.add(t);
        }
      }
    }

    List<List<TransactWriteItem>> partitions = Objects.parition(txnItems, MAX_BATCH_SIZE);

    for (List<TransactWriteItem> part : partitions) {
      if (!part.isEmpty()) {
        transactWriteWithRetry(dbClient, part);
        wrote = true;
      }
    }

    return wrote;
  }

  private void transactWriteWithRetry(final DynamoDbClient dbClient,
      final List<TransactWriteItem> list) {

    int retries = 0;

    while (retries < MAX_RETRIES) {
      try {
        dbClient
            .transactWriteItems(TransactWriteItemsRequest.builder().transactItems(list).build());
        return;

      } catch (TransactionCanceledException e) {
        // Often not retriable (conditional failures), but could be throttling-related.
        // Keep behavior conservative: retry like batch does; caller can adjust later.
        backoffSleep(retries);
        retries++;

      } catch (DynamoDbException e) {
        // Includes throttling / internal errors
        backoffSleep(retries);
        retries++;
      }
    }

    throw new RuntimeException("Transaction could not be completed after retries.");
  }
}
