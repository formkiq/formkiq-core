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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.schemas.SchemaCompositeKeyRecord;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.LongSupplier;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/** Bounded AND searches using an attribute or composite index and remaining attribute checks. */
public final class DocumentSearchMultiAttributeQuery extends AbstractSearchAttributeQuery {

  /** Maximum candidate batch size. */
  private static final int BATCH_SIZE = 100;
  /** Cursor position within the equality branches. */
  private static final String POSITION = "candidatePosition";
  /** Composite key definitions. */
  private final SchemaService schemaService;
  /** Monotonic clock used to start a fresh fallback deadline per invocation. */
  private final LongSupplier nanoTime;

  /**
   * Position within the driving queries.
   * 
   * @param position query branch
   * @param startKey last evaluated DynamoDB key
   */
  private record CandidateCursor(int position, Map<String, AttributeValue> startKey) {
    private static CandidateCursor decode(final String nextToken) {
      Map<String, AttributeValue> start =
          new HashMap<>(notNull(new StringToMapAttributeValue().apply(nextToken)));
      AttributeValue value = start.remove(POSITION);
      return new CandidateCursor(value != null ? Integer.parseInt(value.s()) : 0, start);
    }

    private CandidateCursor advance(final QueryResponse response) {
      return new CandidateCursor(
          response.lastEvaluatedKey().isEmpty() ? this.position + 1 : this.position,
          response.lastEvaluatedKey());
    }

    private Map<String, AttributeValue> encode(final int end) {
      if (this.position >= end) {
        return null;
      }
      Map<String, AttributeValue> cursor = new HashMap<>(this.startKey);
      cursor.put(POSITION, AttributeValue.fromS(Integer.toString(this.position)));
      return cursor;
    }
  }

  /**
   * Constructor.
   *
   * @param database database service
   * @param dbClient database client
   * @param documents document service
   * @param attributes attribute definitions
   * @param schemas composite key definitions
   */
  public DocumentSearchMultiAttributeQuery(final DynamoDbService database,
      final DynamoDbClient dbClient, final DocumentService documents,
      final AttributeService attributes, final SchemaService schemas) {
    this(database, dbClient, documents, attributes, schemas, System::nanoTime);
  }

  /**
   * Constructor with a controllable clock for deadline tests.
   *
   * @param database database service
   * @param dbClient database client
   * @param documents document service
   * @param attributes attribute definitions
   * @param schemas composite key definitions
   * @param clock monotonic time source
   */
  DocumentSearchMultiAttributeQuery(final DynamoDbService database, final DynamoDbClient dbClient,
      final DocumentService documents, final AttributeService attributes,
      final SchemaService schemas, final LongSupplier clock) {
    super(database, dbClient, documents, attributes);
    this.schemaService = schemas;
    this.nanoTime = clock;
  }

  private Map<String, AttributeValue> addResults(final String siteId,
      final List<Map<String, AttributeValue>> matches, final List<DocumentSearchResult> results,
      final boolean count, final Set<String> countedIds, final int limit) {
    AttributeValueToDocumentArtifact toArtifact = new AttributeValueToDocumentArtifact();
    Map<DocumentArtifact, Map<String, AttributeValue>> byArtifact = matches.stream()
        .collect(Collectors.toMap(toArtifact, item -> item, (first, ignored) -> first));
    Map<String, AttributeValue> lastMatch = null;
    for (DocumentSearchResult result : loadResults(siteId, matches)) {
      DocumentArtifact artifact = result.documentRecord().document();
      if (!count || countedIds.add(artifact.documentId())) {
        results.add(result);
        lastMatch = byArtifact.get(artifact);
        if (results.size() == limit) {
          break;
        }
      }
    }
    return lastMatch;
  }

  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {
    List<SearchAttributeCriteria> attributes = normalizeCriteria(siteId, query);
    SearchAttributeCriteria composite = resolveCompositeCriteria(siteId, attributes);
    if (composite != null) {
      return countAttribute(siteId, composite, query.documentIds(), maxResults);
    }
    Pagination<DocumentSearchResult> results =
        queryWithAttributeFilters(siteId, attributes, query.documentIds(), null, maxResults, true);
    int size = results.getResults().size();
    return new SearchCountResult(Math.min(size, maxResults),
        size > maxResults || results.getNextToken() != null);
  }

  /**
   * Keep attribute index rows whose document or artifact matches every search criterion. Only the
   * first matching value of the driving attribute is retained for each document or artifact,
   * preventing duplicates across index pages and equality branches. Results preserve the input row
   * order.
   *
   * @param siteId site identifier
   * @param attributes search criteria combined with AND
   * @param attributeRows rows returned by the driving attribute index query
   * @param budget deadline shared with the index loop
   * @return matching attribute rows in input order
   */
  private List<Map<String, AttributeValue>> filter(final String siteId,
      final List<SearchAttributeCriteria> attributes,
      final List<Map<String, AttributeValue>> attributeRows, final SearchTimeBudget budget) {

    budget.check();
    AttributeValueToDocumentArtifact toArtifact = new AttributeValueToDocumentArtifact();
    List<DocumentArtifact> artifacts = attributeRows.stream().map(toArtifact).distinct().toList();
    Map<DocumentArtifact, Map<String, AttributeValue>> matches =
        firstMatches(siteId, attributes.getFirst(), artifacts, budget);

    // A multivalued driving attribute can appear on several index pages or eqOr
    // branches. Emit only its first matching row, avoiding a growing token of IDs.
    Set<DynamoDbKey> attributeKeys = attributeRows.stream()
        .map(item -> new DynamoDbKey(item.get(PK), item.get(SK))).collect(Collectors.toSet());
    matches.values()
        .removeIf(item -> !attributeKeys.contains(new DynamoDbKey(item.get(PK), item.get(SK))));

    for (int i = 1; i < attributes.size() && !matches.isEmpty(); i++) {
      budget.check();
      Set<DocumentArtifact> matching =
          firstMatches(siteId, attributes.get(i), new ArrayList<>(matches.keySet()), budget)
              .keySet();
      matches.keySet().retainAll(matching);
    }

    // Preserve the driving query's order even if batch reads arrive out of order.
    List<Map<String, AttributeValue>> ordered = new ArrayList<>();
    for (Map<String, AttributeValue> attributeRow : attributeRows) {
      Map<String, AttributeValue> match = matches.remove(toArtifact.apply(attributeRow));
      if (match != null) {
        ordered.add(match);
      }
    }

    return ordered;
  }

  /**
   * Find one matching attribute row for each supplied document or artifact. When multiple rows
   * match, retain the row with the lexicographically smallest sort key. Omit documents or artifacts
   * without a match and preserve the supplied artifact order in the returned map.
   *
   * @param siteId site identifier
   * @param search attribute criterion to match
   * @param artifacts documents or artifacts to check, in the desired result order
   * @param budget deadline checked before each attribute read
   * @return first matching attribute row per document or artifact, in supplied order
   */
  private Map<DocumentArtifact, Map<String, AttributeValue>> firstMatches(final String siteId,
      final SearchAttributeCriteria search, final List<DocumentArtifact> artifacts,
      final SearchTimeBudget budget) {
    Map<DocumentArtifact, Map<String, AttributeValue>> matches = new LinkedHashMap<>();
    AttributeValueToDocumentArtifact toArtifact = new AttributeValueToDocumentArtifact();
    for (Map<String, AttributeValue> item : findMatchingAttributes(siteId, search, artifacts,
        budget::check)) {
      matches.merge(toArtifact.apply(item), item,
          (first, second) -> first.get(SK).s().compareTo(second.get(SK).s()) <= 0 ? first : second);
    }
    Map<DocumentArtifact, Map<String, AttributeValue>> ordered = new LinkedHashMap<>();
    for (DocumentArtifact artifact : artifacts) {
      if (matches.containsKey(artifact)) {
        ordered.put(artifact, matches.get(artifact));
      }
    }
    return ordered;
  }

  @Override
  public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final String nextToken, final int limit) {
    List<SearchAttributeCriteria> attributes = normalizeCriteria(siteId, query);
    SearchAttributeCriteria composite = resolveCompositeCriteria(siteId, attributes);
    return composite != null
        ? queryAttribute(siteId, composite, query.documentIds(), nextToken, limit)
        : queryWithAttributeFilters(siteId, attributes, query.documentIds(), nextToken, limit,
            false);
  }

  private Pagination<DocumentSearchResult> queryWithAttributeFilters(final String siteId,
      final List<SearchAttributeCriteria> attributes, final Collection<String> documentIds,
      final String nextToken, final int limit, final boolean count) {
    List<String> ids = notNull(documentIds).stream().distinct().toList();
    if (!ids.isEmpty()) {
      return queryWithDocumentIds(siteId, attributes, ids);
    }
    List<SearchAttributeCriteria> criteria = createIndexSearchCriteria(siteId, attributes);
    return isEmpty(nextToken) && hasEmptyAttributeIndex(siteId, criteria)
        ? new Pagination<>(List.of())
        : queryByAttributeIndex(siteId, criteria, nextToken, limit, count);
  }

  private Pagination<DocumentSearchResult> queryByAttributeIndex(final String siteId,
      final List<SearchAttributeCriteria> criteria, final String nextToken, final int limit,
      final boolean count) {
    SearchTimeBudget budget = new SearchTimeBudget(this.nanoTime);
    CandidateCursor cursor = CandidateCursor.decode(nextToken);
    List<QueryRequest> requests = createOrderedAttributeQueries(siteId, criteria.getFirst());
    List<DocumentSearchResult> results = new ArrayList<>();
    Set<String> countedIds = new HashSet<>();
    int examined = 0;

    while (cursor.position() < requests.size() && results.size() < limit
        && examined < DocumentSearchService.MAX_DOCUMENT_SEARCH) {

      int batchSize = Math.min(BATCH_SIZE, DocumentSearchService.MAX_DOCUMENT_SEARCH - examined);
      int batchPosition = cursor.position();
      Map<String, AttributeValue> start = cursor.startKey();

      try {
        budget.check();
        QueryResponse response = this.dbClient.query(requests.get(batchPosition).toBuilder()
            .exclusiveStartKey(start.isEmpty() ? null : start).limit(batchSize).build());

        List<Map<String, AttributeValue>> attributeRows = response.items();
        examined += attributeRows.size();
        CandidateCursor completed = cursor.advance(response);

        List<Map<String, AttributeValue>> matches = filter(siteId, criteria, attributeRows, budget);
        budget.check();
        Map<String, AttributeValue> lastMatch =
            addResults(siteId, matches, results, count, countedIds, limit);
        if (results.size() == limit && lastMatch != null) {
          // Resume after the last emitted candidate when only part of the batch fits.
          completed = resumeAfterMatch(attributeRows, lastMatch, batchPosition, completed);
        }
        cursor = completed;
      } catch (SearchTimeBudgetExceededException e) {
        return new Pagination<>(results, cursor.encode(requests.size()), true);
      }
    }
    Map<String, AttributeValue> nextKey = cursor.encode(requests.size());
    boolean truncated = examined >= DocumentSearchService.MAX_DOCUMENT_SEARCH
        && results.size() < limit && nextKey != null;
    return new Pagination<>(results, nextKey, truncated);
  }

  /**
   * Check equality indexes before reading and filtering batches of documents. An EQ OR criterion is
   * empty only when all of its values are absent. Like the driving GSI query, these checks are
   * eventually consistent; they do not guarantee visibility of recently written attributes.
   *
   * @param siteId site identifier
   * @param attributes normalized index criteria
   * @return whether a required equality criterion has no indexed matches
   */
  private boolean hasEmptyAttributeIndex(final String siteId,
      final List<SearchAttributeCriteria> attributes) {
    return attributes.stream()
        .filter(search -> !isEmpty(search.eq()) || !notNull(search.eqOr()).isEmpty())
        .anyMatch(search -> createAttributeQueries(siteId, search, null).stream()
            .map(request -> request.toBuilder().limit(1).projectionExpression(PK).build())
            .noneMatch(request -> !this.dbClient.query(request).items().isEmpty()));
  }

  private Pagination<DocumentSearchResult> queryWithDocumentIds(final String siteId,
      final List<SearchAttributeCriteria> attributes, final List<String> ids) {
    List<Map<String, AttributeValue>> matches = matchDocumentIds(siteId, attributes, ids);
    return new Pagination<>(loadResults(siteId, matches));
  }

  private List<Map<String, AttributeValue>> matchDocumentIds(final String siteId,
      final List<SearchAttributeCriteria> attributes, final List<String> ids) {
    List<Map<String, AttributeValue>> matches = new ArrayList<>();
    for (String id : ids) {
      DocumentArtifact document = DocumentArtifact.of(id, null);
      Map<String, List<Map<String, AttributeValue>>> values = new HashMap<>();
      for (SearchAttributeCriteria search : attributes) {
        values.put(search.key(), loadRequestedAttribute(siteId, document, search.key()));
      }
      List<Map<String, AttributeValue>> matched = attributes.stream()
          .map(search -> values.get(search.key()).stream()
              .filter(item -> matchesAttribute(search, document, item)).findFirst().orElse(null))
          .filter(Objects::nonNull).toList();
      if (matched.size() == attributes.size()) {
        // Keep the first criterion's matching value as response metadata, not as a driver.
        matches.add(matched.getFirst());
      }
    }
    return matches;
  }

  private List<Map<String, AttributeValue>> loadRequestedAttribute(final String siteId,
      final DocumentArtifact document, final String key) {
    SearchAttributeCriteria keyOnly = new SearchAttributeCriteria(key, null, null, null, null);
    QueryRequest request = createAttributeQueries(siteId, keyOnly, document).getFirst();
    List<Map<String, AttributeValue>> items = new ArrayList<>();
    Map<String, AttributeValue> start = null;
    do {
      QueryResponse response = this.dbClient
          .query(request.toBuilder().exclusiveStartKey(start).limit(BATCH_SIZE).build());
      items.addAll(response.items());
      start = response.lastEvaluatedKey();
    } while (!start.isEmpty());
    return items;
  }

  private boolean matchesAttribute(final SearchAttributeCriteria search,
      final DocumentArtifact document, final Map<String, AttributeValue> item) {
    DocumentAttributeRecord record = new DocumentAttributeRecord().setDocument(document)
        .setKey(search.key()).setValueType(DocumentAttributeValueType.KEY_ONLY);
    String sk = item.get(SK).s();
    Collection<String> equals =
        !isEmpty(search.eq()) ? List.of(search.eq()) : notNull(search.eqOr());
    if (!equals.isEmpty()) {
      return equals.stream().anyMatch(value -> sk.equals(
          record.setValueType(DocumentAttributeValueType.STRING).setStringValue(value).sk()));
    }
    String prefix = record.sk();
    if (search.range() != null) {
      return compareSortKeys(sk, prefix + search.range().start()) >= 0
          && compareSortKeys(sk, prefix + search.range().end()) <= 0;
    }
    return search.beginsWith() == null || sk.startsWith(prefix + search.beginsWith());
  }

  private int compareSortKeys(final String first, final String second) {
    // Match DynamoDB string range ordering, including supplementary Unicode characters.
    return Arrays.compareUnsigned(first.getBytes(StandardCharsets.UTF_8),
        second.getBytes(StandardCharsets.UTF_8));
  }

  private List<QueryRequest> createOrderedAttributeQueries(final String siteId,
      final SearchAttributeCriteria search) {
    List<QueryRequest> requests = new ArrayList<>(createAttributeQueries(siteId, search, null));
    if (requests.size() > 1) {
      // Keep the fallback's EQ OR branch order stable for existing continuation tokens.
      requests.sort(
          Comparator.comparing(request -> request.expressionAttributeValues().get(":SK").s()));
    }
    return requests;
  }

  private CandidateCursor resumeAfterMatch(final List<Map<String, AttributeValue>> candidates,
      final Map<String, AttributeValue> lastMatch, final int batchPosition,
      final CandidateCursor cursor) {
    DynamoDbKey lastMatchKey = new DynamoDbKey(lastMatch.get(PK), lastMatch.get(SK));
    for (int i = 0; i < candidates.size() - 1; i++) {
      Map<String, AttributeValue> candidate = candidates.get(i);
      if (new DynamoDbKey(candidate.get(PK), candidate.get(SK)).equals(lastMatchKey)) {
        return new CandidateCursor(batchPosition, Map.of(PK, candidate.get(PK), SK,
            candidate.get(SK), GSI1_PK, candidate.get(GSI1_PK), GSI1_SK, candidate.get(GSI1_SK)));
      }
    }
    return cursor;
  }

  private SearchAttributeCriteria resolveCompositeCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) {
    SchemaCompositeKeyRecord composite = this.schemaService.getCompositeKeyExactMatch(siteId,
        attributes.stream().map(SearchAttributeCriteria::key).toList());
    if (composite == null) {
      return null;
    }
    validateCompositeCriteria(attributes);
    SearchAttributeCriteria search = new SearchAttributesToCriteria(composite).apply(attributes);
    validate(search);
    return search;
  }

  /**
   * Drive the index search with the largest usable composite key, followed by any uncovered
   * criteria. Composite key order determines which attribute may use a non-equality operator. Ties
   * use key order so pagination consistently selects the same index.
   *
   * @param siteId site identifier
   * @param attributes normalized search criteria
   * @return driving criterion followed by the remaining filters
   */
  private List<SearchAttributeCriteria> createIndexSearchCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) {
    if (attributes.size() <= 2) {
      return attributes;
    }
    SchemaCompositeKeyRecord composite =
        this.schemaService.getCompositeKeyBestMatch(siteId, attributes);
    if (composite == null) {
      return attributes;
    }
    Map<String, SearchAttributeCriteria> byKey = attributes.stream()
        .collect(Collectors.toMap(SearchAttributeCriteria::key, search -> search));
    List<SearchAttributeCriteria> covered = composite.getKeys().stream().map(byKey::get).toList();
    SearchAttributeCriteria search = new SearchAttributesToCriteria(composite).apply(covered);
    validate(search);

    List<SearchAttributeCriteria> criteria = new ArrayList<>(List.of(search));
    attributes.stream().filter(attribute -> !composite.getKeys().contains(attribute.key()))
        .forEach(criteria::add);
    return criteria;
  }

  private void validateCompositeCriteria(final List<SearchAttributeCriteria> attributes)
      throws ValidationException {

    ValidationBuilder vb = new ValidationBuilder();

    attributes.subList(0, attributes.size() - 1).forEach(criteria -> {
      if (!isEmpty(criteria.beginsWith())) {
        vb.addError("beginsWith", "'beginsWith' can only be used on last attribute in list");
      } else if (criteria.range() != null) {
        vb.addError("range", "'range' can only be used on last attribute in list");
      } else if (!notNull(criteria.eqOr()).isEmpty()) {
        vb.addError("eqOr", "'eqOr' is not supported with composite keys");
      }
    });

    vb.check();
  }
}
