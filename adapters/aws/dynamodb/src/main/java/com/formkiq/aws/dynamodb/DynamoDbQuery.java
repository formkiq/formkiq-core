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
 * Builds and executes DynamoDB queries. The default query implementation returns one page;
 * implementations may override it to load all pages.
 *
 */
public interface DynamoDbQuery {
  /**
   * Builds a {@link QueryRequest} for one page.
   *
   * @param tableName DynamoDb Table Name.
   * @param siteId Site Identifier
   * @param nextToken continuation token for the starting position, or null for the first page
   * @param limit page size requested from DynamoDB
   * @return QueryRequest
   */
  QueryRequest build(String tableName, String siteId, String nextToken, int limit);

  /**
   * Queries one page from the specified table. The default implementation makes one database
   * request and returns its continuation key so callers can request the next page. Implementations
   * that load all pages may override this method.
   *
   * @param db {@link DynamoDbService}
   * @param tableName DynamoDb Table Name.
   * @param siteId Site Identifier
   * @param nextToken continuation token for the starting position, or null for the first page
   * @param limit page size requested from DynamoDB
   * @return the page's items and continuation key, if more results may be available
   */
  default QueryResult query(DynamoDbService db, String tableName, String siteId, String nextToken,
      int limit) {
    QueryRequest queryRequest = build(tableName, siteId, nextToken, limit);
    QueryResponse response = db.query(queryRequest);
    return new QueryResult(response.items(), response.lastEvaluatedKey());
  }

  /**
   * Queries using the service's default table, delegating to
   * {@link #query(DynamoDbService, String, String, String, int)}. That method returns one page by
   * default; implementations may override it to load all pages.
   *
   * @param db {@link DynamoDbService}
   * @param siteId Site Identifier
   * @param nextToken continuation token for the starting position, or null for the first page
   * @param limit page size requested from DynamoDB
   * @return the query's items and continuation key, if more results may be available
   */
  default QueryResult query(DynamoDbService db, String siteId, String nextToken, int limit) {
    return query(db, db.getTableName(), siteId, nextToken, limit);
  }
}
