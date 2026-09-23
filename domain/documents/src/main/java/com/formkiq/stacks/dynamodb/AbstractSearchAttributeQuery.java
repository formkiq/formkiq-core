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
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.validation.ValidationBuilder;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/** Shared validation, attribute reads and result mapping for attribute searches. */
public abstract class AbstractSearchAttributeQuery implements DocumentSearchQuery {

  /** Database service. */
  protected final DynamoDbService db;

  /** Database client. */
  protected final DynamoDbClient dbClient;
  /** Document service. */
  private final DocumentService documentService;
  /** Attribute definitions used for normalization. */
  private final AttributeService attributeService;

  /**
   * Construct the shared attribute-search operations.
   *
   * @param database database service
   * @param client database client
   * @param documents document service
   * @param attributes attribute definitions
   */
  protected AbstractSearchAttributeQuery(final DynamoDbService database,
      final DynamoDbClient client, final DocumentService documents,
      final AttributeService attributes) {
    this.db = database;
    this.dbClient = client;
    this.documentService = documents;
    this.attributeService = attributes;
  }

  /**
   * Count an already resolved single criterion, including a composite criterion.
   * 
   * @param siteId site identifier
   * @param search resolved search criterion
   * @param documentIds optional document IDs to search
   * @param maxResults maximum number of documents to count
   * @return document count and truncation status
   */
  protected final SearchCountResult countAttribute(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds,
      final int maxResults) {
    if (!notNull(documentIds).isEmpty()) {
      Set<String> matchingIds = findMatchingAttributes(siteId, search,
          documentIds.stream().distinct().map(id -> DocumentArtifact.of(id, null)).toList())
          .stream().map(item -> item.get("documentId").s()).collect(Collectors.toSet());
      return countExistingDocuments(siteId, matchingIds, maxResults);
    }
    return countDistinctQueries(this.dbClient::query, createAttributeQueries(siteId, search, null),
        "documentId", maxResults);
  }

  private SearchCountResult countExistingDocuments(final String siteId,
      final Collection<String> documentIds, final int maxResults) {

    List<DynamoDbKey> keys = documentIds.stream().limit(maxResults)
        .map(id -> new DocumentRecordBuilder().documentId(id).buildKey(siteId)).toList();
    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,documentId");
    int count = keys.isEmpty() ? 0 : this.db.getBatchByKey(config, keys).size();
    return new SearchCountResult(count, documentIds.size() > maxResults);
  }

  /**
   * Build index queries, or document-scoped queries when an artifact is supplied.
   * 
   * @param siteId site identifier
   * @param search resolved search criterion
   * @param artifact document or artifact to search, or null to search the index
   * @return requests for the criterion in equality-value order
   */
  protected final List<QueryRequest> createAttributeQueries(final String siteId,
      final SearchAttributeCriteria search, final DocumentArtifact artifact) {
    DocumentAttributeRecord record = new DocumentAttributeRecord().setKey(search.key())
        .setDocument(artifact).setValueType(DocumentAttributeValueType.KEY_ONLY);
    boolean index = artifact == null;
    String prefix = index ? "" : record.sk();
    List<String> equals = equalityValues(search);
    List<QueryRequest> requests = new ArrayList<>();

    if (!equals.isEmpty()) {
      for (String value : equals) {
        record.setValueType(DocumentAttributeValueType.STRING).setStringValue(value);
        requests.add(createAttributeQueryBuilder(siteId, record, index)
            .eq(index ? record.skGsi1() : record.sk()).build(this.db.getTableName()));
      }
    } else {
      DynamoDbQueryBuilder builder = createAttributeQueryBuilder(siteId, record, index);
      if (search.range() != null) {
        builder.betweenSK(prefix + search.range().start(), prefix + search.range().end());
      } else if (search.beginsWith() != null || !index) {
        builder.beginsWith(prefix + (search.beginsWith() != null ? search.beginsWith() : ""));
      }
      requests.add(builder.build(this.db.getTableName()));
    }
    return requests;
  }

  private DynamoDbQueryBuilder createAttributeQueryBuilder(final String siteId,
      final DocumentAttributeRecord record, final boolean index) {
    return DynamoDbQueryBuilder.builder().pk(index ? record.pkGsi1(siteId) : record.pk(siteId))
        .indexName(index ? GSI1 : null).scanIndexForward(true).limit(1);
  }

  private SearchAttributeCriteria createDateSearchCriteria(final SearchAttributeCriteria search) {

    try {
      String eq = !Strings.isEmpty(search.eq()) ? DateUtil.normalizeDateValue(search.eq()) : null;
      Collection<String> eqOr =
          search.eqOr() != null ? search.eqOr().stream().map(DateUtil::normalizeDateValue).toList()
              : null;
      SearchTagCriteriaRange range = search.range();

      if (range != null) {
        String start =
            !Strings.isEmpty(range.start()) ? DateUtil.normalizeDateValue(range.start()) : null;
        String end =
            !Strings.isEmpty(range.end()) ? DateUtil.normalizeDateValue(range.end()) : null;
        range = new SearchTagCriteriaRange(start, end, range.type());
      }

      return new SearchAttributeCriteria(search.key(), search.beginsWith(), eq, eqOr, range);
    } catch (DateTimeException e) {
      throw ValidationException.builder().error(search.key(), "invalid date value").build();
    }
  }

  private List<String> equalityValues(final SearchAttributeCriteria search) {
    return !isEmpty(search.eq()) ? List.of(search.eq())
        : notNull(search.eqOr()).stream().distinct().toList();
  }

  /**
   * Find matching rows for the supplied documents or artifacts.
   * 
   * @param siteId site identifier
   * @param search resolved search criterion
   * @param artifacts documents or artifacts to check
   * @return matching attribute rows
   */
  protected final List<Map<String, AttributeValue>> findMatchingAttributes(final String siteId,
      final SearchAttributeCriteria search, final List<DocumentArtifact> artifacts) {

    if (artifacts.isEmpty()) {
      return List.of();
    }

    List<String> values = equalityValues(search);
    if (!values.isEmpty()) {
      List<DynamoDbKey> keys = new ArrayList<>();
      for (DocumentArtifact artifact : artifacts) {
        for (String value : values) {
          DocumentAttributeRecord record =
              new DocumentAttributeRecord().setDocument(artifact).setKey(search.key())
                  .setValueType(DocumentAttributeValueType.STRING).setStringValue(value);
          keys.add(record.buildKey(siteId));
        }
      }
      return this.db.getBatchByKey(new BatchGetConfig(), keys.stream().distinct().toList());
    }

    List<Map<String, AttributeValue>> results = new ArrayList<>();
    for (DocumentArtifact artifact : artifacts) {
      results.addAll(
          this.dbClient.query(createAttributeQueries(siteId, search, artifact).getFirst()).items());
    }

    return results;
  }

  private AttributeDataType getAttributeDataType(final String siteId, final String key) {
    AttributeRecord record = this.attributeService.getAttribute(siteId, key);

    if (record != null) {
      return record.getDataType();
    }

    AttributeKeyReserved reserved = AttributeKeyReserved.find(key);
    return reserved != null ? reserved.getDataType() : null;
  }

  /**
   * Load existing documents and their matched attributes in candidate order.
   * 
   * @param siteId site identifier
   * @param matches matching attribute rows in candidate order
   * @return results for existing documents and artifacts
   */
  protected final List<DocumentSearchResult> loadResults(final String siteId,
      final List<Map<String, AttributeValue>> matches) {

    if (matches.isEmpty()) {
      return List.of();
    }

    AttributeValueToDocumentArtifact toArtifact = new AttributeValueToDocumentArtifact();
    Map<DocumentArtifact, Map<String, AttributeValue>> attributes = new LinkedHashMap<>();
    matches.forEach(item -> attributes.putIfAbsent(toArtifact.apply(item), item));

    Map<DocumentArtifact, DocumentRecord> documents =
        notNull(this.documentService.findDocuments(siteId, new ArrayList<>(attributes.keySet())))
            .stream().collect(Collectors.toMap(DocumentRecord::document, document -> document,
                (first, ignored) -> first));

    return attributes.entrySet().stream().filter(entry -> documents.get(entry.getKey()) != null)
        .map(entry -> new DocumentSearchResult(documents.get(entry.getKey()),
            new DocumentAttributeRecord().getFromAttributes(siteId, entry.getValue())))
        .toList();
  }

  /**
   * Normalize and validate criteria without selecting an execution strategy.
   * 
   * @param siteId site identifier
   * @param query search query
   * @return normalized and validated criteria
   */
  protected final List<SearchAttributeCriteria> normalizeCriteria(final String siteId,
      final SearchQuery query) {
    List<SearchAttributeCriteria> attributes =
        !notNull(query.attributes()).isEmpty() ? query.attributes() : List.of(query.attribute());
    long distinctKeys = attributes.stream().map(SearchAttributeCriteria::key).distinct().count();
    if (distinctKeys != attributes.size()) {
      throw ValidationException.builder().error("duplicate attributes in query").build();
    }
    List<SearchAttributeCriteria> normalized =
        attributes.stream().map(attribute -> normalizeDateCriteria(siteId, attribute)).toList();
    normalized.forEach(this::validate);
    return normalized;
  }

  private SearchAttributeCriteria normalizeDateCriteria(final String siteId,
      final SearchAttributeCriteria search) {

    AttributeDataType dataType = getAttributeDataType(siteId, search.key());
    boolean isDate = AttributeDataType.DATE.equals(dataType)
        || search.range() != null && "date".equalsIgnoreCase(search.range().type());
    return isDate ? createDateSearchCriteria(search) : search;
  }

  /**
   * Execute an already resolved single criterion, including a composite criterion.
   * 
   * @param siteId site identifier
   * @param search resolved search criterion
   * @param documentIds optional document IDs to search
   * @param nextToken continuation token, or null for the first page
   * @param limit maximum number of rows per index query
   * @return matching documents and continuation token
   */
  protected final Pagination<DocumentSearchResult> queryAttribute(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds,
      final String nextToken, final int limit) {

    List<Map<String, AttributeValue>> items;
    Map<String, AttributeValue> lastKey = null;

    if (!notNull(documentIds).isEmpty()) {
      items = findMatchingAttributes(siteId, search,
          documentIds.stream().distinct().map(id -> DocumentArtifact.of(id, null)).toList());
    } else {

      List<QueryRequest> requests = createAttributeQueries(siteId, search, null);

      if (isEmpty(search.eq()) && !notNull(search.eqOr()).isEmpty()) {
        // Preserve single-criterion EQ OR behavior: each branch has its own limit.
        items = new ArrayList<>();
        for (QueryRequest request : requests) {
          items.addAll(this.dbClient.query(request.toBuilder().limit(limit).build()).items());
        }

      } else {
        Map<String, AttributeValue> start = new StringToMapAttributeValue().apply(nextToken);
        // Only EQ OR produces multiple requests and is handled above; this branch has one.
        QueryResponse response = this.dbClient.query(requests.getFirst().toBuilder()
            .exclusiveStartKey(notNull(start).isEmpty() ? null : start).limit(limit).build());
        items = response.items();
        lastKey = response.lastEvaluatedKey();
      }

      List<DynamoDbKey> keys =
          items.stream().map(item -> new DynamoDbKey(DynamoDbTypes.toString(item.get(PK)),
              DynamoDbTypes.toString(item.get(SK)))).distinct().toList();
      Map<DynamoDbKey, Map<String, AttributeValue>> found =
          this.db.getBatchByKey(new BatchGetConfig(), keys).stream().collect(
              Collectors.toMap(item -> new DynamoDbKey(DynamoDbTypes.toString(item.get(PK)),
                  DynamoDbTypes.toString(item.get(SK))), item -> item));
      items = keys.stream().map(found::get).filter(Objects::nonNull).toList();
    }

    return new Pagination<>(loadResults(siteId, items), lastKey);
  }

  protected final void validate(final SearchAttributeCriteria search) throws ValidationException {
    ValidationBuilder vb = new ValidationBuilder();
    SearchTagCriteriaRange range = search.range();

    if (range != null) {
      vb.isRequired("start", range.start());
      vb.isRequired("end", range.end());
    }

    vb.check();
  }
}
