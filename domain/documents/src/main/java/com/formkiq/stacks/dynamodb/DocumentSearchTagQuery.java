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

import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.Objects;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_TAG;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_TAGS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/** Executes document searches using tag criteria. */
public final class DocumentSearchTagQuery implements DocumentSearchQuery {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private final DocumentService documentService;

  /**
   * Constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbClient {@link DynamoDbClient}
   * @param documents {@link DocumentService}
   */
  public DocumentSearchTagQuery(final DynamoDbService dbService,
      final DynamoDbClient dynamoDbClient, final DocumentService documents) {
    this.db = dbService;
    this.dbClient = dynamoDbClient;
    this.documentService = documents;
  }

  /**
   * Count documents matching tag criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   */
  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {

    SearchTagCriteria search = query.tag();
    String key = search.key();
    if (!Objects.isEmpty(query.documentIds())) {
      Map<String, Map<String, AttributeValue>> documents =
          findDocumentTags(siteId, query.documentIds(), key);
      Collection<String> documentIds = filterDocumentTags(documents, search).keySet();
      return countExistingDocuments(siteId, documentIds, maxResults);
    }

    List<QueryRequest> requests = new ArrayList<>();
    if (!Objects.isEmpty(search.eqOr())) {
      search.eqOr().forEach(value -> requests.add(createTagValueQuery(siteId, key, value)));
    } else if (search.eq() != null) {
      requests.add(createTagValueQuery(siteId, key, search.eq()));
    } else if (search.beginsWith() != null) {
      String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";
      Map<String, AttributeValue> values =
          Map.of(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)), ":sk",
              AttributeValue.fromS(search.beginsWith()));
      requests.add(createQueryRequest(GSI2, expression, values, null, 1, Boolean.FALSE, null));
    } else if (search.range() != null) {
      String expression = GSI2_PK + " = :pk and " + GSI2_SK + " between :start and :end";
      Map<String, AttributeValue> values =
          Map.of(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)), ":start",
              AttributeValue.fromS(search.range().start()), ":end",
              AttributeValue.fromS(search.range().end()));
      requests.add(createQueryRequest(GSI2, expression, values, null, 1, Boolean.TRUE, null));
    } else {
      String expression = GSI2_PK + " = :pk";
      Map<String, AttributeValue> values =
          Map.of(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
      requests.add(createQueryRequest(GSI2, expression, values, null, 1, Boolean.FALSE, null));
    }

    return countQueries(requests, maxResults);
  }

  private SearchCountResult countExistingDocuments(final String siteId,
      final Collection<String> documentIds, final int maxResults) {

    List<Map<String, AttributeValue>> keys = documentIds.stream().limit(maxResults)
        .map(id -> new DocumentRecordBuilder().documentId(id).buildKey(siteId).toMap()).toList();
    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,documentId");
    int count = keys.isEmpty() ? 0 : this.db.getBatch(config, keys).size();
    return new SearchCountResult(count, documentIds.size() > maxResults);
  }

  private SearchCountResult countQueries(final List<QueryRequest> requests, final int maxResults) {
    return DocumentSearchQuery.countQueries(this.dbClient::query, requests, maxResults);
  }

  private QueryRequest createQueryRequest(final String index, final String expression,
      final Map<String, AttributeValue> values, final String nextToken, final int maxResults,
      final Boolean scanIndexForward, final String projectionExpression) {

    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    return QueryRequest.builder().tableName(this.db.getTableName()).indexName(index)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .projectionExpression(projectionExpression).exclusiveStartKey(startKey)
        .scanIndexForward(scanIndexForward).limit(maxResults).build();
  }

  private QueryRequest createTagValueQuery(final String siteId, final String key,
      final String value) {

    String expression = GSI1_PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue
        .fromS(createDatabaseKey(siteId, PREFIX_TAG + key + TAG_DELIMINATOR + value)));
    return createQueryRequest(GSI1, expression, values, null, 1, Boolean.FALSE, null);
  }

  private boolean filterByValue(final SearchTagCriteria search, final AttributeValue value) {
    if (search.beginsWith() != null) {
      return value.s().startsWith(search.beginsWith());
    } else if (!Objects.notNull(search.eqOr()).isEmpty()) {
      return search.eqOr().contains(value.s());
    } else if (search.eq() != null) {
      return value.s().equals(search.eq());
    }
    return false;
  }

  private Map<String, Map<String, AttributeValue>> filterDocumentTags(
      final Map<String, Map<String, AttributeValue>> documents, final SearchTagCriteria search) {

    if (!hasFilter(search)) {
      return documents;
    }

    return documents.entrySet().stream().filter(entry -> {
      AttributeValue value = entry.getValue().get("tagValue");
      AttributeValue values = entry.getValue().get("tagValues");
      boolean match = false;

      if (values != null) {
        Optional<AttributeValue> matchedValue =
            values.l().stream().filter(candidate -> filterByValue(search, candidate)).findFirst();
        match = matchedValue.isPresent();
        if (match) {
          entry.getValue().remove("tagValues");
          entry.getValue().put("tagValue", matchedValue.get());
        }
      } else if (value != null) {
        match = filterByValue(search, value);
      }

      return match;
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Map<String, AttributeValue>> findDocumentTags(final String siteId,
      final Collection<String> documentIds, final String tagKey) {

    Map<String, Map<String, AttributeValue>> results = new HashMap<>();
    List<Map<String, AttributeValue>> keys = documentIds.stream()
        .map(id -> Map.of(PK, AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_DOCS + id)), SK,
            AttributeValue.fromS(PREFIX_TAGS + tagKey)))
        .collect(Collectors.toList());
    Map<String, KeysAndAttributes> items =
        Map.of(this.db.getTableName(), KeysAndAttributes.builder().keys(keys).build());
    BatchGetItemRequest request = BatchGetItemRequest.builder().requestItems(items).build();
    BatchGetItemResponse response = this.dbClient.batchGetItem(request);
    Collection<List<Map<String, AttributeValue>>> values = response.responses().values();

    if (!values.isEmpty()) {
      values.iterator().next().forEach(record -> {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("type", record.get("type"));
        item.put("tagKey", record.get("tagKey"));
        if (record.containsKey("tagValue")) {
          item.put("tagValue", record.get("tagValue"));
        }
        if (record.containsKey("tagValues")) {
          item.put("tagValues", record.get("tagValues"));
        }
        results.put(record.get("documentId").s(), item);
      });
    }

    return results;
  }

  private Pagination<DocumentSearchResult> findDocumentsByRange(final String siteId,
      final SearchQuery query, final String key, final SearchTagCriteriaRange range,
      final String nextToken, final int maxResults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and " + GSI2_SK + " between :start and :end";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":start", AttributeValue.fromS(range.start()));
    values.put(":end", AttributeValue.fromS(range.end()));
    QueryRequest request = createQueryRequest(GSI2, expression, values, nextToken, maxResults,
        Boolean.TRUE, projectionExpression);
    return queryDocuments(siteId, query, request);
  }

  private Pagination<DocumentSearchResult> findDocumentsByValue(final String siteId,
      final SearchQuery query, final String key, final String value, final String nextToken,
      final int maxResults, final String projectionExpression) {

    String expression = GSI1_PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", AttributeValue
        .fromS(createDatabaseKey(siteId, PREFIX_TAG + key + TAG_DELIMINATOR + value)));
    QueryRequest request = createQueryRequest(GSI1, expression, values, nextToken, maxResults,
        Boolean.FALSE, projectionExpression);
    return queryDocuments(siteId, query, request);
  }

  private Pagination<DocumentSearchResult> findDocumentsByValuePrefix(final String siteId,
      final SearchQuery query, final String key, final String value, final String nextToken,
      final int maxResults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":sk", AttributeValue.fromS(value));
    QueryRequest request = createQueryRequest(GSI2, expression, values, nextToken, maxResults,
        Boolean.FALSE, projectionExpression);
    return queryDocuments(siteId, query, request);
  }

  private Pagination<DocumentSearchResult> findDocumentsByValues(final String siteId,
      final SearchQuery query, final String key, final Collection<String> values,
      final int maxResults, final String projectionExpression) {

    List<DocumentSearchResult> results = new ArrayList<>();
    for (String value : values) {
      results.addAll(
          findDocumentsByValue(siteId, query, key, value, null, maxResults, projectionExpression)
              .getResults());
    }
    return new Pagination<>(results);
  }

  private Pagination<DocumentSearchResult> findDocumentsWithTag(final String siteId,
      final SearchQuery query, final String key, final String nextToken, final int maxResults,
      final String projectionExpression) {

    String expression = GSI2_PK + " = :pk";
    Map<String, AttributeValue> values =
        Map.of(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    QueryRequest request = createQueryRequest(GSI2, expression, values, nextToken, maxResults,
        Boolean.FALSE, projectionExpression);
    return queryDocuments(siteId, query, request);
  }

  private boolean hasFilter(final SearchTagCriteria search) {
    return search.eq() != null || search.beginsWith() != null
        || !Objects.notNull(search.eqOr()).isEmpty();
  }

  private Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final SearchTagCriteria search, final String nextToken, final int maxResults,
      final String projectionExpression) {

    String key = search.key();
    Pagination<DocumentSearchResult> results;

    if (!Objects.isEmpty(query.documentIds())) {

      Map<String, Map<String, AttributeValue>> documents =
          findDocumentTags(siteId, query.documentIds(), key);

      Map<String, Map<String, AttributeValue>> filtered = filterDocumentTags(documents, search);
      List<DocumentArtifact> artifacts = filtered.keySet().stream()
          .map(documentId -> DocumentArtifact.of(documentId, null)).toList();

      List<DocumentRecord> documentResults = this.documentService.findDocuments(siteId, artifacts);

      List<DocumentSearchResult> searchResults = new ArrayList<>();
      documentResults.forEach(result -> {
        DocumentTag tag =
            new AttributeValueToDocumentTag(siteId).apply(filtered.get(result.documentId()));
        searchResults.add(new DocumentSearchResult(result, tag, null));
      });

      results = new Pagination<>(searchResults);

    } else if (!Objects.isEmpty(search.eqOr())) {
      results = findDocumentsByValues(siteId, query, key, search.eqOr(), maxResults,
          projectionExpression);
    } else if (search.eq() != null) {
      results = findDocumentsByValue(siteId, query, key, search.eq(), nextToken, maxResults,
          projectionExpression);
    } else if (search.beginsWith() != null) {
      results = findDocumentsByValuePrefix(siteId, query, key, search.beginsWith(), nextToken,
          maxResults, projectionExpression);
    } else if (search.range() != null) {
      results = findDocumentsByRange(siteId, query, key, search.range(), nextToken, maxResults,
          projectionExpression);
    } else {
      results =
          findDocumentsWithTag(siteId, query, key, nextToken, maxResults, projectionExpression);
    }

    return results;
  }

  /**
   * Search for documents matching tag criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param nextToken pagination token
   * @param limit maximum number of results
   * @return {@link Pagination} of {@link DocumentSearchResult}
   */
  @Override
  public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final String nextToken, final int limit) {
    return query(siteId, query, query.tag(), nextToken, limit, null);
  }

  /**
   * Search for document IDs matching tag criteria.
   *
   * @param siteId site identifier
   * @param criteria {@link SearchTagCriteria}
   * @param nextToken pagination token
   * @param limit maximum number of results
   * @return {@link Pagination} of document IDs
   */
  public Pagination<String> queryDocumentIds(final String siteId, final SearchTagCriteria criteria,
      final String nextToken, final int limit) {

    SearchQuery query = new SearchQuery(null, null, null, null, null, null, null, null, null);
    Pagination<DocumentSearchResult> results =
        query(siteId, query, criteria, nextToken, limit, "documentId");
    List<String> documentIds =
        results.getResults().stream().map(item -> item.documentRecord().documentId()).toList();
    return new Pagination<>(documentIds, results.getNextToken());
  }

  private Pagination<DocumentSearchResult> queryDocuments(final String siteId,
      final SearchQuery query, final QueryRequest request) {

    String projectionExpression = request.projectionExpression();
    QueryResponse response = this.dbClient.query(request);
    List<DocumentArtifact> artifacts =
        response.items().stream().map(new AttributeValueToDocumentArtifact())
            .filter(java.util.Objects::nonNull).distinct().toList();
    Map<String, DocumentTag> tags = transformToDocumentTagMap(response);

    if (!"documentId".equals(projectionExpression)) {
      List<DocumentRecord> results = this.documentService.findDocuments(siteId, artifacts);
      List<DocumentSearchResult> searchResults = new ArrayList<>();

      results.forEach(result -> {

        List<DocumentTag> matchedTags = null;
        DocumentTag matchedTag = tags.get(result.documentId());
        if (!notNull(query.tags()).isEmpty()) {
          matchedTags = updateToMatchedTags(query);
        }

        searchResults.add(new DocumentSearchResult(result, matchedTag, matchedTags));
      });

      return new Pagination<>(searchResults, response.lastEvaluatedKey());
    }

    List<DocumentSearchResult> searchResults = artifacts.stream().map(a -> {
      DocumentRecord d = new DocumentRecord(a.documentId(), a.artifactId());
      return new DocumentSearchResult(d);
    }).toList();

    return new Pagination<>(searchResults, response.lastEvaluatedKey());
  }

  private Map<String, DocumentTag> transformToDocumentTagMap(final QueryResponse response) {
    Map<String, DocumentTag> tags = new HashMap<>();
    response.items().forEach(item -> {
      if (item.containsKey("documentId")) {
        String documentId = item.get("documentId").s();
        String tagKey = item.containsKey("tagKey") ? item.get("tagKey").s() : null;
        String tagValue = item.containsKey("tagValue") ? item.get("tagValue").s() : "";
        DocumentTag tag = tags.containsKey(documentId) ? tags.get(documentId)
            : new DocumentTag().setKey(tagKey).setValue(tagValue)
                .setType(DocumentTagType.USERDEFINED);

        if (tags.containsKey(documentId)) {
          if (tag.getValues() == null) {
            tag.setValues(new ArrayList<>());
            tag.getValues().add(tag.getValue());
            tag.setValue(null);
          }
          tag.getValues().add(tagValue);
        } else {
          tags.put(documentId, tag);
        }
      }
    });
    return tags;
  }

  private List<DocumentTag> updateToMatchedTags(final SearchQuery query) {

    List<DocumentTag> matchedTags = new ArrayList<>();
    for (SearchTagCriteria criteria : query.tags()) {
      var tag = new DocumentTag(null, criteria.key(), criteria.eq(), null, null,
          DocumentTagType.USERDEFINED);
      matchedTags.add(tag);
    }

    return matchedTags;
  }
}
