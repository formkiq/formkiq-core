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

import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.formkiq.aws.dynamodb.objects.Objects.isEmpty;

/**
 * DynamoDb Query Builder.
 *
 */
public interface DynamoDbShardQuery {
  /**
   * Builds a {@link QueryRequest}.
   *
   * @param tableName DynamoDb Table Name.
   * @param siteId Site Identifier
   * @param nextToken Next Token
   * @param limit int
   * @return {@link List } {@link QueryRequest}
   */
  List<QueryRequest> build(String tableName, String siteId, String nextToken, int limit);

  /**
   * Calculate Last Evaluated Key.
   * 
   * @param queryRequest {@link List} {@link QueryRequest}
   * @param queryResults {@link List} {@link Map}
   * @return {@link Map}
   */
  default Map<String, AttributeValue> calculateLastEvaluatedKey(List<QueryRequest> queryRequest,
      List<Map<String, AttributeValue>> queryResults) {
    String indexName = getIndexName(queryRequest);
    Map<String, AttributeValue> last = Objects.last(queryResults);
    return new DynamodbLastEvaluatedKeyBuilder(last, indexName).build(true);
  }

  /**
   * Get Sort Comparator.
   * 
   * @param scanIndexForward boolean
   * @return {@link Comparator}
   */
  default Comparator<? super Map<String, AttributeValue>> getComparator(
      final boolean scanIndexForward) {
    return null;
  }

  /**
   * Get Index Name.
   * 
   * @param queryRequest {@link List} {@link String}
   * @return {@link String}
   */
  default String getIndexName(final List<QueryRequest> queryRequest) {
    return !isEmpty(queryRequest) ? queryRequest.get(0).indexName() : null;
  }

  /**
   * Is {@link QueryRequest} scan index forward.
   * 
   * @param queryRequest {@link List} {@link QueryRequest}
   * @return boolean
   */
  default boolean isScanIndexForward(List<QueryRequest> queryRequest) {
    return queryRequest.stream().anyMatch(QueryRequest::scanIndexForward);
  }

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

    List<QueryRequest> queryRequest = build(tableName, siteId, nextToken, limit);
    boolean scanIndexForward = isScanIndexForward(queryRequest);

    List<QueryResult> results = queryRequest.parallelStream().map(db::query)
        .map(r -> new QueryResult(r.items(), r.lastEvaluatedKey())).toList();

    Stream<Map<String, AttributeValue>> stream = results.stream().flatMap(r -> r.items().stream());

    Comparator<? super Map<String, AttributeValue>> comparator = getComparator(scanIndexForward);
    if (comparator != null) {
      stream = stream.sorted(comparator);
    }

    List<Map<String, AttributeValue>> list = stream.toList();

    Map<String, AttributeValue> lastEvaluatedKey = calculateLastEvaluatedKey(queryRequest, list);

    return new QueryResult(list, lastEvaluatedKey);
  }
}
