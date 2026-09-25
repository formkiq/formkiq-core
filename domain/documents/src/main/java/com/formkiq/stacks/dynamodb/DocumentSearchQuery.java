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
package com.formkiq.stacks.dynamodb;

import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Executes a document search for a supported {@link SearchQuery} criterion. */
public interface DocumentSearchQuery {

  /**
   * Count query results, following each query's continuation key until exhausted or capped.
   *
   * @param queryFunction executes a DynamoDB query
   * @param requests queries to execute
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   */
  static SearchCountResult countQueries(final Function<QueryRequest, QueryResponse> queryFunction,
      final List<QueryRequest> requests, final int maxResults) {

    int count = 0;

    for (int requestIndex = 0; requestIndex < requests.size(); requestIndex++) {
      Map<String, AttributeValue> startKey = null;

      do {
        int remaining = maxResults - count;
        QueryRequest request = requests.get(requestIndex).toBuilder().limit(remaining)
            .exclusiveStartKey(startKey).projectionExpression(null).select(Select.COUNT).build();
        QueryResponse response = queryFunction.apply(request);
        count += response.count();
        startKey = response.lastEvaluatedKey();

        if (count >= maxResults) {
          boolean hasMorePages = startKey != null && !startKey.isEmpty();
          boolean hasMoreQueries = requestIndex < requests.size() - 1;
          return new SearchCountResult(maxResults, hasMorePages || hasMoreQueries);
        }
      } while (startKey != null && !startKey.isEmpty());
    }

    return new SearchCountResult(count, false);
  }

  /**
   * Count matching documents.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   * @throws ValidationException validation exception
   */
  SearchCountResult count(String siteId, SearchQuery query, int maxResults)
      throws ValidationException;

  /**
   * Count distinct query results, following each query until exhausted or capped.
   *
   * @param queryFunction executes a DynamoDB query
   * @param requests queries to execute
   * @param attributeName attribute containing the distinct value
   * @param maxResults maximum number of distinct results to count
   * @return {@link SearchCountResult}
   */
  default SearchCountResult countDistinctQueries(
      final Function<QueryRequest, QueryResponse> queryFunction, final List<QueryRequest> requests,
      final String attributeName, final int maxResults) {

    Set<String> values = new HashSet<>();
    int queryLimit = maxResults + 1;

    for (QueryRequest queryRequest : requests) {
      Map<String, AttributeValue> startKey = null;

      do {
        QueryRequest request =
            queryRequest.toBuilder().limit(queryLimit).exclusiveStartKey(startKey)
                .projectionExpression(attributeName).select(Select.SPECIFIC_ATTRIBUTES).build();
        QueryResponse response = queryFunction.apply(request);
        response.items().stream().map(item -> item.get(attributeName))
            .filter(java.util.Objects::nonNull).map(AttributeValue::s)
            .filter(java.util.Objects::nonNull).forEach(values::add);

        if (values.size() > maxResults) {
          return new SearchCountResult(maxResults, true);
        }

        startKey = response.lastEvaluatedKey();
      } while (startKey != null && !startKey.isEmpty());
    }

    return new SearchCountResult(values.size(), false);
  }

  /**
   * Search for matching documents.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param nextToken pagination token
   * @param limit maximum number of results
   * @return {@link Pagination} of {@link DocumentSearchResult}
   * @throws ValidationException validation exception
   */
  Pagination<DocumentSearchResult> query(String siteId, SearchQuery query, String nextToken,
      int limit) throws ValidationException;
}
