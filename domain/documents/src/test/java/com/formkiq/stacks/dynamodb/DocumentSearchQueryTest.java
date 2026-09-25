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
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for {@link DocumentSearchQuery} count helpers. */
public class DocumentSearchQueryTest {

  /** Implementation inheriting the default count helper. */
  private final DocumentSearchQuery searchQuery = new DocumentSearchQuery() {
    @Override
    public SearchCountResult count(final String siteId, final SearchQuery query,
        final int maxResults) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
        final String nextToken, final int limit) {
      throw new UnsupportedOperationException();
    }
  };

  private Map<String, AttributeValue> item(final String documentId) {
    return Map.of("documentId", AttributeValue.fromS(documentId));
  }

  /** Count all pages until the query is exhausted. */
  @Test
  public void testCount01() {
    // given
    Map<String, AttributeValue> nextKey = Map.of("PK", AttributeValue.fromS("next"));
    Iterator<QueryResponse> responses =
        List.of(QueryResponse.builder().count(3).lastEvaluatedKey(nextKey).build(),
            QueryResponse.builder().count(4).lastEvaluatedKey(Map.of()).build()).iterator();
    List<QueryRequest> executed = new ArrayList<>();
    QueryRequest request = QueryRequest.builder().tableName("documents").build();

    // when
    SearchCountResult result = DocumentSearchQuery.countQueries(query -> {
      executed.add(query);
      return responses.next();
    }, List.of(request), 10);

    // then
    assertEquals(7, result.count());
    assertFalse(result.truncated());
    assertEquals(2, executed.size());
    assertEquals(10, executed.get(0).limit());
    assertEquals(7, executed.get(1).limit());
    assertEquals(nextKey, executed.get(1).exclusiveStartKey());
    assertEquals(Select.COUNT, executed.get(1).select());
  }

  /** Stop at the cap when DynamoDB has another page. */
  @Test
  public void testCount02() {
    // given
    Map<String, AttributeValue> firstKey = Map.of("PK", AttributeValue.fromS("first"));
    Map<String, AttributeValue> secondKey = Map.of("PK", AttributeValue.fromS("second"));
    Iterator<QueryResponse> responses =
        List.of(QueryResponse.builder().count(6).lastEvaluatedKey(firstKey).build(),
            QueryResponse.builder().count(4).lastEvaluatedKey(secondKey).build()).iterator();
    List<QueryRequest> executed = new ArrayList<>();
    QueryRequest request = QueryRequest.builder().tableName("documents").build();

    // when
    SearchCountResult result = DocumentSearchQuery.countQueries(query -> {
      executed.add(query);
      return responses.next();
    }, List.of(request), 10);

    // then
    assertEquals(10, result.count());
    assertTrue(result.truncated());
    assertEquals(2, executed.size());
    assertEquals(4, executed.get(1).limit());
    assertEquals(firstKey, executed.get(1).exclusiveStartKey());
  }

  /** Report truncation when the cap is reached before another logical query. */
  @Test
  public void testCount03() {
    // given
    QueryResponse response = QueryResponse.builder().count(10).lastEvaluatedKey(Map.of()).build();
    QueryRequest request0 = QueryRequest.builder().tableName("documents").build();
    QueryRequest request1 = QueryRequest.builder().tableName("documents").build();

    // when
    SearchCountResult result =
        DocumentSearchQuery.countQueries(query -> response, List.of(request0, request1), 10);

    // then
    assertEquals(10, result.count());
    assertTrue(result.truncated());
  }

  /** Do not report truncation when the final page contains exactly the cap. */
  @Test
  public void testCount04() {
    // given
    QueryResponse response = QueryResponse.builder().count(10).lastEvaluatedKey(Map.of()).build();
    QueryRequest request = QueryRequest.builder().tableName("documents").build();

    // when
    SearchCountResult result =
        DocumentSearchQuery.countQueries(query -> response, List.of(request), 10);

    // then
    assertEquals(10, result.count());
    assertFalse(result.truncated());
  }

  /** Count duplicate values only once across pages and logical queries. */
  @Test
  public void testCountDistinct01() {
    // given
    Map<String, AttributeValue> nextKey = Map.of("PK", AttributeValue.fromS("next"));
    Iterator<QueryResponse> responses =
        List.of(
            QueryResponse.builder().items(item("doc1"), item("doc2")).lastEvaluatedKey(nextKey)
                .build(),
            QueryResponse.builder().items(item("doc1"), item("doc3")).build(),
            QueryResponse.builder().items(item("doc2"), item("doc3")).build()).iterator();
    List<QueryRequest> executed = new ArrayList<>();
    List<QueryRequest> requests = List.of(QueryRequest.builder().tableName("documents").build(),
        QueryRequest.builder().tableName("documents").build());

    // when
    SearchCountResult result = this.searchQuery.countDistinctQueries(query -> {
      executed.add(query);
      return responses.next();
    }, requests, "documentId", 10);

    // then
    assertEquals(3, result.count());
    assertFalse(result.truncated());
    assertEquals(3, executed.size());
    assertEquals(Select.SPECIFIC_ATTRIBUTES, executed.getFirst().select());
    assertEquals("documentId", executed.getFirst().projectionExpression());
    assertEquals(nextKey, executed.get(1).exclusiveStartKey());
  }

  /** Stop after finding one more distinct value than the cap. */
  @Test
  public void testCountDistinct02() {
    // given
    QueryResponse response =
        QueryResponse.builder().items(item("doc1"), item("doc1"), item("doc2")).build();
    QueryRequest request = QueryRequest.builder().tableName("documents").build();

    // when
    SearchCountResult result =
        this.searchQuery.countDistinctQueries(query -> response, List.of(request), "documentId", 1);

    // then
    assertEquals(1, result.count());
    assertTrue(result.truncated());
  }
}
