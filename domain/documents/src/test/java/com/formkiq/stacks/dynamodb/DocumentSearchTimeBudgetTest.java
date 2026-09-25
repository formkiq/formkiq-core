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

import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchQueryBuilder;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deadline tests use real batch mapping and an advancing clock, without sleeping. */
class DocumentSearchTimeBudgetTest {

  /** Monotonic fake time. */
  private final AtomicLong clock = new AtomicLong();
  /** Synthetic database records. */
  private final Map<Map<String, AttributeValue>, Map<String, AttributeValue>> items =
      new HashMap<>();
  /** Driving index records. */
  private final List<Map<String, AttributeValue>> rows = new ArrayList<>();
  /** Stage at which the second batch runs out of time. */
  private String stopAt;
  /** Start offset of the current batch. */
  private int offset;
  /** Successful driving requests, to verify retries don't skip rows. */
  private final List<Integer> starts = new ArrayList<>();
  /** Documents checked by non-equality attribute queries. */
  private final List<Integer> filterReads = new ArrayList<>();
  /** Number of individual attribute batch requests. */
  private int attributeReads;

  private void add(final Map<String, AttributeValue> item) {
    this.items.put(Map.of("PK", item.get("PK"), "SK", item.get("SK")), item);
  }

  private void fixtures(final boolean matches) {
    for (int i = 0; i < 201; i++) {
      Map<String, AttributeValue> customer = attribute(i, "customer", "123");
      this.rows.add(customer);
      add(customer);
      add(new DocumentRecordBuilder().documentId(Integer.toString(i)).build("site")
          .getAttributes());
      if (matches && (i == 42 || i == 142)) {
        add(attribute(i, "status", "approved"));
      }
    }
  }

  private Map<String, AttributeValue> attribute(final int id, final String key,
      final String value) {
    return new DocumentAttributeRecord()
        .setDocument(DocumentArtifact.of(Integer.toString(id), null)).setKey(key)
        .setValueType(DocumentAttributeValueType.STRING).setStringValue(value)
        .getAttributes("site");
  }

  private void stop(final String stage) {
    if (stage.equals(this.stopAt)) {
      this.clock.addAndGet(Duration.ofSeconds(25).toNanos());
    }
  }

  private QueryResponse query(final QueryRequest request) {
    assertTrue(request.overrideConfiguration().isEmpty());
    if (request.expressionAttributeValues().get(":PK").s().contains("schemas#")) {
      return QueryResponse.builder().build();
    }
    String pk = request.expressionAttributeValues().get(":PK").s();
    if (request.indexName() == null) {
      int id = Integer.parseInt(pk.substring(pk.lastIndexOf('#') + 1));
      this.filterReads.add(id);
      if (id == 105) {
        stop("filterQueries");
      }
      Map<String, AttributeValue> status = attribute(id, "status", "approved");
      boolean present =
          this.items.containsKey(Map.of("PK", status.get("PK"), "SK", status.get("SK")));
      return QueryResponse.builder().items(present ? List.of(status) : List.of()).build();
    }
    if ("PK".equals(request.projectionExpression())) {
      stop("precheck");
      return QueryResponse.builder().items(Map.of("PK", AttributeValue.fromS("exists"))).build();
    }
    this.offset = request.exclusiveStartKey().isEmpty() ? 0
        : Integer.parseInt(request.exclusiveStartKey().get("offset").s());
    this.starts.add(this.offset);
    if (this.offset == 100) {
      stop("index");
    }
    int end = Math.min(this.offset + request.limit(), this.rows.size());
    return QueryResponse.builder().items(this.rows.subList(this.offset, end))
        .lastEvaluatedKey(
            end < this.rows.size() ? Map.of("offset", AttributeValue.fromS(Integer.toString(end)))
                : Map.of())
        .build();
  }

  private BatchGetItemResponse batch(final BatchGetItemRequest request) {
    assertTrue(request.overrideConfiguration().isEmpty());
    var keys = request.requestItems().get("Documents").keys();
    String sk = keys.getFirst().get("SK").s();
    if (sk.startsWith("attr#")) {
      this.attributeReads++;
      stop("attributeBatch");
    }
    if (this.offset == 100) {
      stop(sk.startsWith("attr#") ? "filter" : "documents");
    }
    List<Map<String, AttributeValue>> found =
        keys.stream().map(this.items::get).filter(java.util.Objects::nonNull).toList().reversed();
    return BatchGetItemResponse.builder().responses(Map.of("Documents", found)).build();
  }

  private DocumentSearchQuery search() {
    DynamoDbClient client = stub(DynamoDbClient.class, (method, args) -> switch (method) {
      case "getItem" -> GetItemResponse.builder().build();
      case "query" -> query((QueryRequest) args[0]);
      case "batchGetItem" -> batch((BatchGetItemRequest) args[0]);
      default -> throw new AssertionError(method);
    });
    var db = new DynamoDbServiceImpl(client, "Documents");
    DocumentService documents = stub(DocumentService.class, (method, args) -> {
      assertEquals("findDocuments", method);
      List<?> artifacts = (List<?>) args[1];
      var keys = artifacts.stream().map(DocumentArtifact.class::cast)
          .map(artifact -> new DocumentRecordBuilder().document(artifact).buildKey("site"))
          .toList();
      return db.getBatchByKey(new BatchGetConfig(), keys).stream()
          .map(DocumentRecord::fromAttributeMap).toList();
    });
    return new DocumentSearchMultiAttributeQuery(db, client, documents,
        new AttributeServiceDynamodb(db), new SchemaServiceDynamodb(db), this.clock::get);
  }

  private SearchQuery criteria() {
    return new SearchQueryBuilder()
        .attributes(List.of(new SearchAttributeCriteria("customer", null, "123", null, null),
            new SearchAttributeCriteria("status", null, "approved", null, null)))
        .build();
  }

  private <T> T stub(final Class<T> type, final BiFunction<String, Object[], Object> handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
        (proxy, method, args) -> handler.apply(method.getName(), args)));
  }

  /** An expired budget before the next read retains the preceding completed batch. */
  @Test
  void testUnfinishedBatchResumesWithoutSkippingMatches() {
    for (String stage : List.of("index", "filter")) {
      // given
      this.rows.clear();
      this.starts.clear();
      fixtures(true);
      this.stopAt = stage;

      // when
      var first = search().query("site", criteria(), null, 10);

      // then
      assertTrue(first.isTruncated());
      assertEquals(List.of("42"),
          first.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
      Map<String, AttributeValue> token =
          new StringToMapAttributeValue().apply(first.getNextToken());
      assertEquals("100", token.get("offset").s());

      // given
      this.stopAt = null;

      // when
      var second = search().query("site", criteria(), first.getNextToken(), 10);

      // then
      assertEquals(List.of("142"), second.getResults().stream()
          .map(result -> result.documentRecord().documentId()).toList());
      assertEquals(List.of(0, 100, 100, 200), this.starts);
      assertFalse(second.isTruncated());
      assertNull(second.getNextToken());
    }
  }

  /** The processing budget starts in the index loop after the empty-index prechecks. */
  @Test
  void testBudgetStartsAfterPrechecks() {
    // given
    fixtures(true);
    this.stopAt = "precheck";

    // when
    var page = search().query("site", criteria(), null, 10);

    // then
    assertEquals(Duration.ofSeconds(50).toNanos(), this.clock.get());
    assertEquals(2, page.getResults().size());
    assertFalse(page.isTruncated());
    assertNull(page.getNextToken());
  }

  /** Stop per-document filtering reads at the deadline and retry the unfinished batch. */
  @Test
  void testNonEqualityFilterTimeoutResumes() {
    // given
    fixtures(true);
    this.stopAt = "filterQueries";
    SearchQuery criteria = new SearchQueryBuilder()
        .attributes(List.of(new SearchAttributeCriteria("customer", null, "123", null, null),
            new SearchAttributeCriteria("status", "a", null, null, null)))
        .build();
    DocumentSearchQuery query = search();

    // when
    var first = query.query("site", criteria, null, 10);

    // then
    assertTrue(first.isTruncated());
    assertEquals(List.of("42"),
        first.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
    assertEquals(106, this.filterReads.size());
    assertEquals(105, this.filterReads.getLast());
    assertEquals("100",
        new StringToMapAttributeValue().apply(first.getNextToken()).get("offset").s());

    // given
    this.stopAt = null;
    this.filterReads.clear();

    // when
    var second = query.query("site", criteria, first.getNextToken(), 10);

    // then
    assertEquals(100, this.filterReads.getFirst());
    assertEquals(List.of("142"),
        second.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
    assertFalse(second.isTruncated());
    assertNull(second.getNextToken());
  }

  /** No matches is incomplete when time runs out; its cursor still makes progress. */
  @Test
  void testEmptyTimedPageResumes() {
    // given
    fixtures(false);
    this.stopAt = "index";

    // when
    var first = search().query("site", criteria(), null, 10);

    // then
    assertTrue(first.isTruncated());
    assertTrue(first.getResults().isEmpty());
    assertNotNull(first.getNextToken());

    // given
    this.stopAt = null;

    // when
    var second = search().query("site", criteria(), first.getNextToken(), 10);

    // then
    assertTrue(second.getResults().isEmpty());
    assertFalse(second.isTruncated());
    assertNull(second.getNextToken());
  }

  /** Counts include only completed batches and advertise that they are lower bounds. */
  @Test
  void testPartialCount() {
    // given
    fixtures(true);
    this.stopAt = "filter";

    // when
    SearchCountResult count = search().count("site", criteria(), 10000);

    // then
    assertEquals(new SearchCountResult(1, true), count);
  }

  /** An equality lookup exceeding 100 keys checks the budget before its next batch read. */
  @Test
  void testEqualityBatchStopsBeforeNextRead() {
    // given
    fixtures(true);
    this.stopAt = "attributeBatch";
    SearchQuery query = new SearchQueryBuilder().attributes(
        List.of(new SearchAttributeCriteria("customer", null, null, List.of("123", "456"), null),
            new SearchAttributeCriteria("status", null, "approved", null, null)))
        .build();

    // when
    var page = search().query("site", query, null, 10);

    // then
    assertEquals(1, this.attributeReads);
    assertTrue(page.getResults().isEmpty());
    assertTrue(page.isTruncated());
    assertNotNull(page.getNextToken());
  }

  /** A document read already in progress may finish and commit its verified batch. */
  @Test
  void testInFlightReadCompletesBeforeStopping() {
    // given
    fixtures(true);
    this.stopAt = "documents";
    DocumentSearchQuery query = search();

    // when
    var page = query.query("site", criteria(), null, 10);

    // then
    assertEquals(List.of("42", "142"),
        page.getResults().stream().map(result -> result.documentRecord().documentId()).toList());
    assertTrue(page.isTruncated());
    assertEquals(List.of(0, 100), this.starts);
    assertEquals("200",
        new StringToMapAttributeValue().apply(page.getNextToken()).get("offset").s());

    // given
    this.stopAt = null;

    // when
    var next = query.query("site", criteria(), page.getNextToken(), 10);

    // then
    assertTrue(next.getResults().isEmpty());
    assertFalse(next.isTruncated());
    assertNull(next.getNextToken());
    assertEquals(List.of(0, 100, 200), this.starts);
  }

  /** A reusable query starts a fresh local budget for each fallback invocation. */
  @Test
  void testQueryStartsFreshBudget() {
    // given
    fixtures(true);
    DocumentSearchQuery query = search();
    this.stopAt = "index";

    // when
    var first = query.query("site", criteria(), null, 10);

    // then
    assertTrue(first.isTruncated());
    assertEquals(1, first.getResults().size());

    // given
    this.stopAt = null;

    // when
    var second = query.query("site", criteria(), first.getNextToken(), 10);

    // then
    assertEquals(1, second.getResults().size());
    assertFalse(second.isTruncated());
    assertNull(second.getNextToken());
    // when
    SearchCountResult count = query.count("site", criteria(), 10000);

    // then
    assertEquals(new SearchCountResult(2, false), count);
  }

}
