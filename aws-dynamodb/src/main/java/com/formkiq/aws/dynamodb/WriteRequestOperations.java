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
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Holds batch and transactional write request builders.
 */
public class WriteRequestOperations {

  /** Batch Write Request Builder. */
  private final WriteRequestBuilder batchWriter = new WriteRequestBuilder();
  /** Transaction Write Request Builder. */
  private final WriteRequestBuilder transactionWriter = new WriteRequestBuilder();

  /**
   * Append batch write request.
   *
   * @param tableName {@link String}
   * @param values {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestOperations}
   */
  public WriteRequestOperations appendBatch(final String tableName,
      final Map<String, AttributeValue> values) {
    this.batchWriter.append(tableName, values);
    return this;
  }

  /**
   * Append transaction write request.
   *
   * @param tableName {@link String}
   * @param values {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestOperations}
   */
  public WriteRequestOperations appendTransaction(final String tableName,
      final Map<String, AttributeValue> values) {
    this.transactionWriter.append(tableName, values);
    return this;
  }

  /**
   * Append transaction delete request.
   *
   * @param tableName {@link String}
   * @param key {@link DynamoDbKey}
   * @return {@link WriteRequestOperations}
   */
  public WriteRequestOperations appendTransactionDelete(final String tableName,
      final DynamoDbKey key) {
    this.transactionWriter.appendDelete(tableName, key);
    return this;
  }

  /**
   * Append batch write requests.
   *
   * @param tableName {@link String}
   * @param values {@link Collection} {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestOperations}
   */
  public WriteRequestOperations appendsBatch(final String tableName,
      final Collection<Map<String, AttributeValue>> values) {
    this.batchWriter.appends(tableName, values);
    return this;
  }

  /**
   * Append transaction write requests.
   *
   * @param tableName {@link String}
   * @param values {@link Collection} {@link Map} {@link AttributeValue}
   * @return {@link WriteRequestOperations}
   */
  public WriteRequestOperations appendsTransaction(final String tableName,
      final Collection<Map<String, AttributeValue>> values) {
    this.transactionWriter.appends(tableName, values);
    return this;
  }

  /**
   * Get Batch Writer.
   *
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder batchWriter() {
    return this.batchWriter;
  }

  /**
   * Execute Write Operations.
   *
   * @param dbClient {@link DynamoDbClient}
   * @return boolean whether anything was written
   */
  public boolean execute(final DynamoDbClient dbClient) {
    boolean transactionSaved = this.transactionWriter.transactWriteItems(dbClient);
    boolean batchSaved = this.batchWriter.batchWriteItem(dbClient);
    return transactionSaved || batchSaved;
  }

  /**
   * Are there any requests.
   *
   * @return boolean
   */
  public boolean isEmpty() {
    return this.batchWriter.isEmpty() && this.transactionWriter.isEmpty();
  }

  /**
   * Determine if a transaction write request exists for the key.
   *
   * @param key {@link DynamoDbKey}
   * @return boolean
   */
  public boolean transactionExists(final DynamoDbKey key) {
    return this.transactionWriter.exists(key);
  }

  /**
   * Get Transaction Writer.
   *
   * @return {@link WriteRequestBuilder}
   */
  public WriteRequestBuilder transactionWriter() {
    return this.transactionWriter;
  }
}
