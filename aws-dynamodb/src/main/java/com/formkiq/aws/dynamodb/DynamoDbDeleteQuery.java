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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * DynamoDb Delete Builder.
 *
 */
public interface DynamoDbDeleteQuery extends DynamoDbQuery {

  /**
   * Run Delete {@link DynamoDbQuery}.
   * 
   * @param db {@link DynamoDbService}
   * @param siteId {@link String}
   * @return {@link DeleteResults}
   */
  default DeleteResults delete(DynamoDbService db, String siteId) {
    return delete(db, db.getTableName(), siteId);
  }

  /**
   * Run Delete {@link DynamoDbQuery}.
   * 
   * @param db {@link DynamoDbService}
   * @param tableName {@link String}
   * @param siteId {@link String}
   * @return {@link DeleteResults}
   */
  default DeleteResults delete(DynamoDbService db, String tableName, String siteId) {

    final int limit = 1000;
    String nextToken = null;
    boolean hasResults = true;
    boolean deleted = false;

    Collection<Map<String, AttributeValue>> attributes = new ArrayList<>();

    while (hasResults) {

      QueryResult result = query(db, tableName, siteId, nextToken, limit);
      attributes.addAll(result.items());

      if (deleteItems(db, tableName, siteId, result)) {
        deleted = true;
      }

      nextToken = result.toNextToken();
      hasResults = result.hasLastEvaluatedKey();
    }

    return new DeleteResults(deleted, attributes);
  }

  /**
   * Delete Items.
   *
   * @param db {@link DynamoDbService}
   * @param tableName {@link String}
   * @param siteId {@link String}
   * @param result {@link QueryResult}
   * @return boolean
   */
  default boolean deleteItems(final DynamoDbService db, final String tableName, final String siteId,
      final QueryResult result) {
    var keys = result.items().stream().map(DynamoDbKey::fromAttributeMap).toList();
    return db.deleteItems(tableName, keys);
  }
}
