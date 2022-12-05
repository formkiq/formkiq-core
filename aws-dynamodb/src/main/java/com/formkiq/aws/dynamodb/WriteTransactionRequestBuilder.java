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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Helper class for building {@link TransactWriteItem}.
 *
 */
public class WriteTransactionRequestBuilder {

  /** Max Batch Size. */
  private static final int MAX_BATCH_SIZE = 25;
  /** {@link TransactWriteItem}. */
  private Collection<TransactWriteItem> items = new HashSet<>();

  /**
   * constructor.
   */
  public WriteTransactionRequestBuilder() {

  }

  // /**
  // * Collects {@link WriteRequest} and adds to a internal list.
  // *
  // * @param writes {@link Map} {@link WriteRequest}
  // * @return {@link WriteTransactionRequestBuilder}
  // */
  // public WriteTransactionRequestBuilder append(final Map<String, TransactWriteItem> writes) {
  //
  // for (Map.Entry<String, TransactWriteItem> e : writes.entrySet()) {
  // if (this.items.containsKey(e.getKey())) {
  // this.items.get(e.getKey()).add(e.getValue());
  // } else {
  // List<TransactWriteItem> list = new ArrayList<>();
  // list.add(e.getValue());
  // this.items.put(e.getKey(), list);
  // }
  // }
  //
  // return this;
  // }

  /**
   * Append {@link Put} request to transaction.
   * 
   * @param put {@link Put}
   * @return {@link WriteTransactionRequestBuilder}
   */
  public WriteTransactionRequestBuilder append(final Put put) {
    this.items.add(TransactWriteItem.builder().put(put).build());
    return this;
  }

  // /**
  // * Append {@link Map} {@link AttributeValue}.
  // *
  // * @param tableName {@link String}
  // * @param values {@link Map} {@link AttributeValue}
  // * @return {@link WriteTransactionRequestBuilder}
  // */
  // public WriteTransactionRequestBuilder append(final String tableName,
  // final Map<String, AttributeValue> values) {
  // Map<String, TransactWriteItem> writes =
  // new AttributeValueToTransactionWriteRequest(tableName).apply(values);
  // append(writes);
  // return this;
  // }

  // /**
  // * Collects {@link WriteRequest} and adds to a internal list.
  // *
  // * @param writes {@link Map} {@link TransactWriteItem}
  // * @return {@link WriteTransactionRequestBuilder}
  // */
  // public WriteTransactionRequestBuilder appends(final Map<String, List<TransactWriteItem>>
  // writes) {
  //
  // for (Entry<String, List<TransactWriteItem>> e : writes.entrySet()) {
  // if (this.items.containsKey(e.getKey())) {
  // this.items.get(e.getKey()).addAll(e.getValue());
  // } else {
  // this.items.put(e.getKey(), e.getValue());
  // }
  // }
  //
  // return this;
  // }

  // /**
  // * Append List {@link Map} {@link AttributeValue}.
  // *
  // * @param tableName {@link String}
  // * @param values {@link Collection} {@link Map} {@link AttributeValue}
  // * @return {@link WriteTransactionRequestBuilder}
  // */
  // public WriteTransactionRequestBuilder appends(final String tableName,
  // final Collection<Map<String, AttributeValue>> values) {
  //
  // AttributeValueToWriteRequest convert = new AttributeValueToWriteRequest(tableName);
  // for (Map<String, AttributeValue> value : values) {
  // Map<String, TransactWriteItem> writes = convert.apply(value);
  // append(writes);
  // }
  //
  // return this;
  // }

  /**
   * Batch Write Items.
   * 
   * @param dbClient {@link DynamoDbClient}
   * @return boolean
   */
  public boolean batchTransactionWrite(final DynamoDbClient dbClient) {

    boolean write = false;

    Collection<TransactWriteItem> wrs = new HashSet<>(getItems());

    List<List<TransactWriteItem>> parition = Objects.parition(new ArrayList<>(wrs), MAX_BATCH_SIZE);

    for (List<TransactWriteItem> writelist : parition) {

      if (!writelist.isEmpty()) {

        try {
          write = true;
          dbClient.transactWriteItems(
              TransactWriteItemsRequest.builder().transactItems(writelist).build());
        } catch (TransactionCanceledException e) {

          Optional<CancellationReason> o = e.cancellationReasons().stream()
              .filter(f -> f.code().equals("ConditionalCheckFailed")).findAny();

          String message = e.cancellationReasons().stream().map(s -> s.message())
              .collect(Collectors.joining(","));

          if (message != null && message.length() > 0) {
            throw new RuntimeException("unable to write DynamoDb Tx: " + message);
          }

          if (!o.isPresent()) {
            throw e;
          }

        }
      }
    }

    return write;
  }

  /**
   * Get {@link TransactWriteItem} Items.
   * 
   * @return {@link Collection}
   */
  public Collection<TransactWriteItem> getItems() {
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
