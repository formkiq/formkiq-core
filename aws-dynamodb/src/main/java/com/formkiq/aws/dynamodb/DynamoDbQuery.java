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

import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * DynamoDb Query Builder.
 *
 */
public interface DynamoDbQuery {
  /**
   * Builds a {@link QueryRequest}.
   *
   * @param tableName DynamoDb Table Name.
   * @param siteId Site Identifier
   * @param nextToken Next Token
   * @param limit int
   * @return QueryRequest
   */
  QueryRequest build(String tableName, String siteId, String nextToken, int limit);

  /**
   * Find the first record to match {@link QueryRequest}.
   *
   * @param db {@link DynamoDbService}
   * @param tableName DynamoDb Table Name.
   * @param siteId Site Identifier
   * @param nextToken Next Token
   * @param limit int
   * @return QueryResult
   */
  default QueryResult query(DynamoDbService db, String tableName, String siteId, String nextToken,
      int limit) {
    QueryRequest queryRequest = build(tableName, siteId, nextToken, limit);
    QueryResponse response = db.query(queryRequest);
    return new QueryResult(response.items(), response.lastEvaluatedKey());
  }
}
