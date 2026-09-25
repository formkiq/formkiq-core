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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests index prechecks and the production search budget without 10,000 database writes. */
public class DocumentSearchMultiAttributeQueryTest {

  /** Equality index requests, separate from driving index pagination. */
  private final List<QueryRequest> indexChecks = new ArrayList<>();
  /** Values absent from the index, including documents outside the driving result set. */
  private final Set<String> missingValues = new HashSet<>();

  private DynamoDbClient createQueryClient(final List<Map<String, AttributeValue>> candidates,
      final AtomicInteger examined,
      final Map<Map<String, AttributeValue>, Map<String, AttributeValue>> items) {
    int size = candidates.size();
    return stub(DynamoDbClient.class, (method, args) -> {
      if ("batchGetItem".equals(method)) {
        var request = (BatchGetItemRequest) args[0];
        var found = request.requestItems().get("Documents").keys().stream().map(items::get)
            .filter(java.util.Objects::nonNull).toList().reversed();
        return BatchGetItemResponse.builder().responses(Map.of("Documents", found)).build();
      }
      if (!"query".equals(method)) {
        throw new AssertionError(method);
      }
      QueryRequest request = (QueryRequest) args[0];
      if ("PK".equals(request.projectionExpression())) {
        assertEquals(1, request.limit());
        assertEquals("GSI1", request.indexName());
        assertTrue(request.exclusiveStartKey().isEmpty());
        this.indexChecks.add(request);
        String value = request.expressionAttributeValues().get(":SK").s();
        // A value can exist globally without matching any of the driving documents.
        return QueryResponse.builder().items(this.missingValues.contains(value) ? List.of()
            : List.of(Map.of("PK", AttributeValue.fromS("site/docs#outside")))).build();
      }
      assertTrue(request.limit() <= 100);
      int start = request.exclusiveStartKey().isEmpty() ? 0
          : Integer.parseInt(request.exclusiveStartKey().get("offset").s());
      int end = Math.min(start + request.limit(), size);
      examined.addAndGet(end - start);
      Map<String, AttributeValue> cursor =
          end < size ? Map.of("offset", AttributeValue.fromS(Integer.toString(end))) : Map.of();
      return QueryResponse.builder().items(candidates.subList(start, end)).lastEvaluatedKey(cursor)
          .build();
    });
  }

  private Map<String, AttributeValue> key(final Map<String, AttributeValue> item) {
    return Map.of("PK", item.get("PK"), "SK", item.get("SK"));
  }

  private SearchQuery query() {
    return new SearchQueryBuilder()
        .attributes(List.of(new SearchAttributeCriteria("customer", null, "123", null, null),
            new SearchAttributeCriteria("status", null, "approved", null, null)))
        .build();
  }

  private Map<String, AttributeValue> record(final int id, final String key, final String value) {
    return new DocumentAttributeRecord()
        .setDocument(DocumentArtifact.of(Integer.toString(id), null)).setKey(key)
        .setValueType(DocumentAttributeValueType.STRING).setStringValue(value)
        .getAttributes("site");
  }

  private DocumentSearchQuery search(final int size, final AtomicInteger examined,
      final boolean matches) {
    List<Map<String, AttributeValue>> candidates = new ArrayList<>();
    Map<Map<String, AttributeValue>, Map<String, AttributeValue>> items = new HashMap<>();
    for (int i = 0; i < size; i++) {
      Map<String, AttributeValue> item = record(i, "customer", "123");
      candidates.add(item);
      items.put(key(item), item);
      var document =
          new DocumentRecordBuilder().documentId(Integer.toString(i)).build("site").getAttributes();
      items.put(key(document), document);
      if (matches && (i == 42 || i == 10000)) {
        Map<String, AttributeValue> status = record(i, "status", "approved");
        items.put(key(status), status);
      }
    }
    DynamoDbClient client = createQueryClient(candidates, examined, items);
    DynamoDbService db = new DynamoDbServiceImpl(client, "Documents");
    DocumentService documents = stub(DocumentService.class, (method, args) -> {
      assertEquals("findDocuments", method);
      List<?> artifacts = (List<?>) args[1];
      return artifacts.stream().map(DocumentArtifact.class::cast)
          .map(artifact -> new DocumentRecordBuilder().document(artifact).build("site")).toList();
    });
    return new DocumentSearchMultiAttributeQuery(db, client, documents,
        stub(AttributeService.class, (method, args) -> null),
        stub(SchemaService.class, (method, args) -> null));
  }

  private DocumentSearchQuery searchDocumentIds(final AtomicInteger examined) {
    DynamoDbClient client = stub(DynamoDbClient.class, (method, args) -> {
      assertEquals("query", method);
      QueryRequest request = (QueryRequest) args[0];
      assertNull(request.indexName());
      assertTrue(request.limit() <= 100);
      String pk = request.expressionAttributeValues().get(":PK").s();
      int id = Integer.parseInt(pk.substring(pk.lastIndexOf('#') + 1));
      String prefix = request.expressionAttributeValues().get(":SK").s();
      List<Map<String, AttributeValue>> items;
      if ("attr#customer#".equals(prefix)) {
        examined.incrementAndGet();
        items = List.of(record(id, "customer", "123"));
      } else {
        assertEquals("attr#status#", prefix);
        items = id == 42 || id == 98 ? List.of(record(id, "status", "approved")) : List.of();
      }
      return QueryResponse.builder().items(items).build();
    });
    DynamoDbService db = stub(DynamoDbService.class, (method, args) -> {
      assertEquals("getTableName", method);
      return "Documents";
    });
    DocumentService documents = stub(DocumentService.class, (method, args) -> {
      assertEquals("findDocuments", method);
      List<?> artifacts = (List<?>) args[1];
      return artifacts.stream()
          .map(a -> new DocumentRecordBuilder().document((DocumentArtifact) a).build("site"))
          .toList().reversed();
    });
    return new DocumentSearchMultiAttributeQuery(db, client, documents,
        stub(AttributeService.class, (method, args) -> null),
        stub(SchemaService.class, (method, args) -> null), () -> {
          throw new AssertionError("Explicit document IDs must not start a time budget");
        });
  }

  private <T> T stub(final Class<T> type, final BiFunction<String, Object[], Object> handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
        (proxy, method, args) -> handler.apply(method.getName(), args)));
  }

  /** A count can be truncated far below the result cap because of the candidate cap. */
  @Test
  public void testCountCandidateBudget() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10001, examined, true);

    // when
    SearchCountResult result = search.count("site", query(), 10000);

    // then
    assertEquals(new SearchCountResult(1, true), result);
    assertEquals(10000, examined.get());
  }

  /** Counts are truncated only when additional matching documents exist. */
  @Test
  public void testDocumentIdsCountLimit() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = searchDocumentIds(examined);
    List<String> ids = IntStream.range(0, 100).mapToObj(Integer::toString).toList();
    SearchQuery query =
        new SearchQueryBuilder().attributes(query().attributes()).documentIds(ids).build();

    // when
    SearchCountResult count = search.count("site", query, 1);

    // then
    assertEquals(new SearchCountResult(1, true), count);
    assertEquals(100, examined.get());

    // when
    count = search.count("site", query, 2);

    // then
    assertEquals(new SearchCountResult(2, false), count);

    // given
    query = new SearchQueryBuilder().attributes(query.attributes()).documentIds(ids.subList(0, 42))
        .build();

    // when
    var results = search.query("site", query, null, 1);

    // then
    assertTrue(results.getResults().isEmpty());
    assertNull(results.getNextToken());
  }

  /** Supplied IDs return all matches regardless of the requested limit or continuation token. */
  @Test
  public void testDocumentIdsIgnoreLimit() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = searchDocumentIds(examined);
    SearchQuery query = new SearchQueryBuilder().attributes(query().attributes())
        .documentIds(IntStream.range(0, 100).mapToObj(Integer::toString).toList()).build();

    // when
    var results = search.query("site", query, "ignored", 1);

    // then
    assertEquals(List.of("42", "98"),
        results.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
    assertNull(results.getNextToken());
    assertEquals(100, examined.get());
    assertFalse(results.isTruncated());
  }

  /** Every alternative must be absent before an EQ OR index is considered empty. */
  @Test
  public void testEmptyEqOrIndex() {
    // given
    this.missingValues.addAll(List.of("missing", "approved"));
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10001, examined, true);
    SearchQuery query = new SearchQueryBuilder().attributes(List.of(query().attributes().getFirst(),
        new SearchAttributeCriteria("status", null, null, List.of("missing", "approved"), null)))
        .build();

    // when
    var results = search.query("site", query, null, 10);

    // then
    assertTrue(results.getResults().isEmpty());
    assertFalse(results.isTruncated());
    assertNull(results.getNextToken());
    assertEquals(3, this.indexChecks.size());
    assertEquals(0, examined.get());
  }

  /** An empty driving or residual equality index avoids all document batch reads. */
  @Test
  public void testEmptyEqualityIndex() {
    for (String value : List.of("123", "approved")) {
      // given
      this.missingValues.clear();
      this.missingValues.add(value);
      this.indexChecks.clear();
      AtomicInteger examined = new AtomicInteger();
      DocumentSearchQuery search = search(10001, examined, true);

      // when
      var results = search.query("site", query(), null, 10);

      // then
      assertTrue(results.getResults().isEmpty());
      assertNull(results.getNextToken());
      assertFalse(results.isTruncated());
      assertEquals(0, examined.get());
      assertEquals("123".equals(value) ? 1 : 2, this.indexChecks.size());

      // when
      SearchCountResult count = search.count("site", query(), 10000);

      // then
      assertEquals(new SearchCountResult(0, false), count);
      assertEquals(0, examined.get());
    }
  }

  /** Empty result pages preserve a cursor and resume after the bounded candidate batch. */
  @Test
  public void testEmptyPageResumes() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10001, examined, false);

    // when
    var first = search.query("site", query(), null, 10);

    // then
    assertTrue(first.getResults().isEmpty());
    assertNotNull(first.getNextToken());
    assertTrue(first.isTruncated());
    assertEquals(10000, examined.get());
    assertEquals(2, this.indexChecks.size());

    // given - continuation pages must not repeat index checks, even if index visibility changes.
    this.missingValues.add("approved");

    // when
    var second = search.query("site", query(), first.getNextToken(), 10);

    // then
    assertTrue(second.getResults().isEmpty());
    assertNull(second.getNextToken());
    assertFalse(second.isTruncated());
    assertEquals(10001, examined.get());
    assertEquals(2, this.indexChecks.size());
  }

  /** A later matching EQ OR alternative allows normal filtering and skips further probes. */
  @Test
  public void testEqOrIndexHasLaterMatch() {
    // given
    this.missingValues.add("missing");
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(100, examined, true);
    SearchQuery query = new SearchQueryBuilder()
        .attributes(List.of(query().attributes().getFirst(), new SearchAttributeCriteria("status",
            null, null, List.of("missing", "approved", "unused"), null)))
        .build();

    // when
    var results = search.query("site", query, null, 10);

    // then
    assertEquals(List.of("42"),
        results.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
    assertEquals(List.of("123", "missing", "approved"), this.indexChecks.stream()
        .map(request -> request.expressionAttributeValues().get(":SK").s()).toList());
    assertEquals(100, examined.get());
  }

  /** Reaching exactly the budget at exhaustion does not mark the count truncated. */
  @Test
  public void testExactCandidateBudget() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10000, examined, true);

    // when
    SearchCountResult result = search.count("site", query(), 10000);

    // then
    assertEquals(new SearchCountResult(1, false), result);
    assertEquals(10000, examined.get());

    // when
    var page = search.query("site", query(), null, 10);

    // then
    assertFalse(page.isTruncated());
    assertNull(page.getNextToken());
  }

  /** Failed index checks propagate instead of being interpreted as an empty result. */
  @Test
  public void testIndexCheckFailure() {
    // given
    DynamoDbClient client = stub(DynamoDbClient.class, (method, args) -> {
      throw new IllegalStateException("query failed");
    });
    DynamoDbService db = stub(DynamoDbService.class, (method, args) -> "Documents");
    DocumentSearchQuery search = new DocumentSearchMultiAttributeQuery(db, client, null,
        stub(AttributeService.class, (method, args) -> null),
        stub(SchemaService.class, (method, args) -> null));

    // when
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> search.query("site", query(), null, 10));

    // then
    assertEquals("query failed", error.getMessage());
  }

  /** Filling the requested page is ordinary pagination, not budget truncation. */
  @Test
  public void testResultLimitIsNotTruncated() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10001, examined, true);

    // when
    var page = search.query("site", query(), null, 1);

    // then
    assertEquals(1, page.getResults().size());
    assertNotNull(page.getNextToken());
    assertFalse(page.isTruncated());
  }

  /** A short result page resumes to matching candidates beyond the first request's budget. */
  @Test
  public void testShortPageResumesToMatch() {
    // given
    AtomicInteger examined = new AtomicInteger();
    DocumentSearchQuery search = search(10001, examined, true);

    // when
    var first = search.query("site", query(), null, 10);

    // then
    assertEquals("42", first.getResults().getFirst().documentRecord().documentId());
    assertEquals(1, first.getResults().size());
    assertNotNull(first.getNextToken());
    assertTrue(first.isTruncated());

    // when
    var second = search.query("site", query(), first.getNextToken(), 10);

    // then
    assertEquals("10000", second.getResults().getFirst().documentRecord().documentId());
    assertNull(second.getNextToken());
    assertFalse(second.isTruncated());
  }

}
