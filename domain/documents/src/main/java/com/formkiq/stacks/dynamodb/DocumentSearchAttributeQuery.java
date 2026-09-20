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
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.attributes.AttributeDataType;
import com.formkiq.aws.dynamodb.attributes.AttributeKeyReserved;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.attributes.AttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.schemas.SchemaCompositeKeyRecord;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_SK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;

/** Executes document searches using attribute criteria. */
public final class DocumentSearchAttributeQuery implements DocumentSearchQuery {

  private static SearchAttributeCriteria createDateSearchCriteria(
      final SearchAttributeCriteria search) {

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

  /** {@link AttributeService}. */
  private final AttributeService attributeService;
  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private final DocumentService documentService;

  /** {@link SchemaService}. */
  private final SchemaService schemaService;

  /**
   * Constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbClient {@link DynamoDbClient}
   * @param documents {@link DocumentService}
   * @param attributes {@link AttributeService}
   * @param schemas {@link SchemaService}
   */
  public DocumentSearchAttributeQuery(final DynamoDbService dbService,
      final DynamoDbClient dynamoDbClient, final DocumentService documents,
      final AttributeService attributes, final SchemaService schemas) {
    this.db = dbService;
    this.dbClient = dynamoDbClient;
    this.documentService = documents;
    this.attributeService = attributes;
    this.schemaService = schemas;
  }

  private List<DocumentSearchResult> addMatchedAttributes(final String siteId,
      final List<Map<String, AttributeValue>> items, final List<DocumentRecord> documents) {

    List<Map<String, AttributeValue>> keys =
        items.stream().map(item -> Map.of(PK, item.get(PK), SK, item.get(SK))).toList();

    AttributeValueToDocumentAttributeRecord toMatchedAttribute =
        new AttributeValueToDocumentAttributeRecord();

    Map<String, DocumentAttributeRecord> matchesByDocument =
        this.db.getBatch(new BatchGetConfig(), keys).stream()
            .collect(Collectors.toMap(i -> DynamoDbTypes.toString(i.get("documentId")),
                i -> toMatchedAttribute.apply(siteId, i), (first, ignored) -> first));

    return documents.stream().map(document -> new DocumentSearchResult(document,
        matchesByDocument.get(document.documentId()))).toList();
  }

  /**
   * Count documents matching attribute criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   * @throws ValidationException validation exception
   */
  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query, final int maxResults)
      throws ValidationException {

    SearchAttributeCriteria search = resolveCriteria(siteId, query);
    validate(search);

    if (!Objects.isEmpty(query.documentIds())) {
      List<Map<String, AttributeValue>> items = searchDocumentIds(siteId, query, search);
      Set<String> documentIds = items.stream().filter(item -> item.containsKey("documentId"))
          .map(item -> item.get("documentId").s()).collect(Collectors.toSet());
      return countExistingDocuments(siteId, documentIds, maxResults);
    }

    DocumentAttributeRecord record = new DocumentAttributeRecord().setKey(search.key());
    String pk = record.pkGsi1(siteId);
    List<QueryRequest> requests = new ArrayList<>();

    if (!Strings.isEmpty(search.eq())) {
      requests.add(createCountQuery(pk, "eq", search.eq(), null));
    } else if (!Objects.isEmpty(search.eqOr())) {
      search.eqOr().forEach(value -> requests.add(createCountQuery(pk, "eq", value, null)));
    } else if (search.range() != null) {
      requests.add(createCountQuery(pk, "range", search.range().start(), search.range().end()));
    } else if (search.beginsWith() != null) {
      requests.add(createCountQuery(pk, "beginsWith", search.beginsWith(), null));
    } else {
      requests.add(createCountQuery(pk, null, null, null));
    }

    return countDistinctQueries(requests, maxResults);
  }

  private SearchCountResult countDistinctQueries(final List<QueryRequest> requests,
      final int maxResults) {
    return DocumentSearchQuery.countDistinctQueries(this.dbClient::query, requests, "documentId",
        maxResults);
  }

  private SearchCountResult countExistingDocuments(final String siteId,
      final Collection<String> documentIds, final int maxResults) {

    List<Map<String, AttributeValue>> keys = documentIds.stream().limit(maxResults)
        .map(id -> new DocumentRecordBuilder().documentId(id).buildKey(siteId).toMap()).toList();
    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,documentId");
    int count = keys.isEmpty() ? 0 : this.db.getBatch(config, keys).size();
    return new SearchCountResult(count, documentIds.size() > maxResults);
  }

  private QueryRequest createCountQuery(final String pk, final String operation,
      final String firstValue, final String secondValue) {

    String expression = GSI1_PK + " = :pk";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(pk));

    if ("eq".equals(operation)) {
      expression += " and " + GSI1_SK + " = :sk";
      values.put(":sk", AttributeValue.fromS(firstValue));
    } else if ("beginsWith".equals(operation)) {
      expression += " and begins_with(" + GSI1_SK + ", :sk)";
      values.put(":sk", AttributeValue.fromS(firstValue));
    } else if ("range".equals(operation)) {
      expression += " and " + GSI1_SK + " between :start and :end";
      values.put(":start", AttributeValue.fromS(firstValue));
      values.put(":end", AttributeValue.fromS(secondValue));
    }

    return QueryRequest.builder().tableName(this.db.getTableName()).indexName(GSI1)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .scanIndexForward(Boolean.TRUE).limit(1).build();
  }

  private SearchAttributeCriteria createMultiAttributeCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) throws ValidationException {

    List<SearchAttributeCriteria> normalized =
        attributes.stream().map(attribute -> normalize(siteId, attribute)).toList();
    SchemaCompositeKeyRecord compositeKey = validateMultiAttributeCriteria(siteId, normalized);
    return new SearchAttributesToCriteria(compositeKey).apply(normalized);
  }

  private AttributeDataType getAttributeDataType(final String siteId, final String key) {
    AttributeRecord record = this.attributeService.getAttribute(siteId, key);

    if (record != null) {
      return record.getDataType();
    }

    AttributeKeyReserved reserved = AttributeKeyReserved.find(key);
    return reserved != null ? reserved.getDataType() : null;
  }

  private SearchAttributeCriteria normalize(final String siteId,
      final SearchAttributeCriteria search) {

    AttributeDataType dataType = getAttributeDataType(siteId, search.key());
    boolean isDate = AttributeDataType.DATE.equals(dataType)
        || search.range() != null && "date".equalsIgnoreCase(search.range().type());
    return isDate ? createDateSearchCriteria(search) : search;
  }

  /**
   * Search for documents matching attribute criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param nextToken pagination token
   * @param limit maximum number of results
   * @return {@link Pagination} of {@link DocumentSearchResult}
   * @throws ValidationException validation exception
   */
  @Override
  public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final String nextToken, final int limit) throws ValidationException {

    SearchAttributeCriteria search = resolveCriteria(siteId, query);
    validate(search);
    DocumentAttributeRecord record = new DocumentAttributeRecord().setKey(search.key());
    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);
    QueryResponse response = null;
    List<Map<String, AttributeValue>> items;

    if (!Objects.isEmpty(query.documentIds())) {
      items = searchDocumentIds(siteId, query, search);
    } else if (!Strings.isEmpty(search.eq())) {
      response = searchEquals(siteId, search, record, config, startKey, limit);
      items = response.items();
    } else if (!Objects.isEmpty(search.eqOr())) {
      items = searchEqualsAny(siteId, search, record, config, limit);
    } else if (search.range() != null) {
      response = searchRange(siteId, search, record, config, startKey, limit);
      items = response.items();
    } else if (search.beginsWith() != null) {
      response = searchBeginsWith(siteId, search, record, config, startKey, limit);
      items = response.items();
    } else {
      config.indexName(GSI1);
      AttributeValue pk = AttributeValue.fromS(record.pkGsi1(siteId));
      response = this.db.query(config, pk, startKey, limit);
      items = response.items();
    }

    List<DocumentArtifact> artifacts =
        items.stream().map(new AttributeValueToDocumentArtifact()).distinct().toList();
    List<DocumentRecord> documents = this.documentService.findDocuments(siteId, artifacts);
    List<DocumentSearchResult> results = addMatchedAttributes(siteId, items,
        documents != null ? documents : Collections.emptyList());

    return new Pagination<>(results, response != null ? response.lastEvaluatedKey() : null);
  }

  private SearchAttributeCriteria resolveCriteria(final String siteId, final SearchQuery query)
      throws ValidationException {

    if (!notNull(query.attributes()).isEmpty()) {
      Collection<String> keys =
          query.attributes().stream().map(SearchAttributeCriteria::key).collect(Collectors.toSet());

      if (keys.size() != query.attributes().size()) {
        throw new ValidationException(Collections
            .singletonList(new ValidationErrorImpl().error("duplicate attributes in query")));
      }

      return createMultiAttributeCriteria(siteId, query.attributes());
    }

    return normalize(siteId, query.attribute());
  }

  private QueryResponse searchBeginsWith(final String siteId, final SearchAttributeCriteria search,
      final DocumentAttributeRecord record, final QueryConfig config,
      final Map<String, AttributeValue> startKey, final int limit) {

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(record.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(search.beginsWith());
    return this.db.queryBeginsWith(config, pk, sk, startKey, limit);
  }

  private List<Map<String, AttributeValue>> searchDocumentIds(final String siteId,
      final SearchQuery query, final SearchAttributeCriteria search) {

    return !Strings.isEmpty(search.eq()) || !Objects.isEmpty(search.eqOr())
        ? searchDocumentIdsByEquals(siteId, search, query.documentIds())
        : searchDocumentIdsByOther(siteId, search, query.documentIds());
  }

  private List<Map<String, AttributeValue>> searchDocumentIdsByEquals(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    List<String> values = new ArrayList<>();
    if (!Strings.isEmpty(search.eq())) {
      values.add(search.eq());
    }
    values.addAll(Objects.notNull(search.eqOr()));
    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    for (String documentId : documentIds) {
      keys.addAll(values.stream().map(value -> {
        DocumentAttributeRecord record = new DocumentAttributeRecord().setKey(search.key())
            .setValueType(DocumentAttributeValueType.STRING).setStringValue(value)
            .setDocument(DocumentArtifact.of(documentId, null));
        return Map.of(PK, record.fromS(record.pk(siteId)), SK, record.fromS(record.sk()));
      }).toList());
    }

    return this.db.getBatch(new BatchGetConfig(), keys);
  }

  private List<Map<String, AttributeValue>> searchDocumentIdsByOther(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    String key = search.key();
    DocumentAttributeRecord record = new DocumentAttributeRecord().setKey(key);
    QueryConfig config = new QueryConfig();
    List<Map<String, AttributeValue>> results = new ArrayList<>();

    for (String documentId : documentIds) {
      record.setDocument(DocumentArtifact.of(documentId, null));
      AttributeValue pk = record.fromS(record.pk(siteId));
      String skPrefix = ATTR + key + "#";

      if (search.range() != null) {
        AttributeValue start = record.fromS(skPrefix + search.range().start());
        AttributeValue end = record.fromS(skPrefix + search.range().end());
        results.addAll(this.db.between(config, pk, start, end, null, 1).items());
      } else {
        String sk = search.beginsWith() != null ? skPrefix + search.beginsWith() : skPrefix;
        results.addAll(this.db.queryBeginsWith(config, pk, record.fromS(sk), null, 1).items());
      }
    }

    return results;
  }

  private QueryResponse searchEquals(final String siteId, final SearchAttributeCriteria search,
      final DocumentAttributeRecord record, final QueryConfig config,
      final Map<String, AttributeValue> startKey, final int limit) {

    record.setValueType(DocumentAttributeValueType.STRING);
    record.setStringValue(search.eq());
    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(record.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(record.skGsi1());
    return this.db.query(config, pk, sk, startKey, limit);
  }

  private List<Map<String, AttributeValue>> searchEqualsAny(final String siteId,
      final SearchAttributeCriteria search, final DocumentAttributeRecord record,
      final QueryConfig config, final int limit) {

    List<Map<String, AttributeValue>> results = new ArrayList<>();
    for (String value : search.eqOr()) {
      SearchAttributeCriteria criteria =
          new SearchAttributeCriteria(search.key(), null, value, null, null);
      results.addAll(searchEquals(siteId, criteria, record, config, null, limit).items());
    }
    return results;
  }

  private QueryResponse searchRange(final String siteId, final SearchAttributeCriteria search,
      final DocumentAttributeRecord record, final QueryConfig config,
      final Map<String, AttributeValue> startKey, final int limit) {

    SearchTagCriteriaRange range = search.range();
    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(record.pkGsi1(siteId));
    AttributeValue start = AttributeValue.fromS(range.start());
    AttributeValue end = range.end() != null ? AttributeValue.fromS(range.end()) : null;
    return this.db.between(config, pk, start, end, startKey, limit);
  }

  private void validate(final SearchAttributeCriteria search) throws ValidationException {
    Collection<ValidationError> errors = new ArrayList<>();
    SearchTagCriteriaRange range = search.range();

    if (range != null) {
      if (Strings.isEmpty(range.start())) {
        errors.add(new ValidationErrorImpl().key("start").error("'start' is required"));
      }
      if (Strings.isEmpty(range.end())) {
        errors.add(new ValidationErrorImpl().key("end").error("'end' is required"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private SchemaCompositeKeyRecord validateMultiAttributeCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) throws ValidationException {

    SchemaCompositeKeyRecord compositeKey = null;
    List<ValidationError> errors = new ArrayList<>();

    if (attributes.size() > 1) {
      List<String> keys = attributes.stream().map(SearchAttributeCriteria::key).toList();
      compositeKey = this.schemaService.getCompositeKey(siteId, keys);
      if (compositeKey == null) {
        errors.add(new ValidationErrorImpl()
            .error("no composite key found for attributes '" + String.join(",", keys) + "'"));
      }
    }

    for (int i = 0; i < attributes.size() - 1; i++) {
      SearchAttributeCriteria criteria = attributes.get(i);
      if (!isEmpty(criteria.beginsWith())) {
        errors.add(new ValidationErrorImpl().key("beginsWith")
            .error("'beginsWith' can only be used on last attribute in list"));
      } else if (criteria.range() != null) {
        errors.add(new ValidationErrorImpl().key("range")
            .error("'range' can only be used on last attribute in list"));
      } else if (!notNull(criteria.eqOr()).isEmpty()) {
        errors.add(new ValidationErrorImpl().key("eqOr")
            .error("'eqOr' is not supported with composite keys"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return compositeKey;
  }
}
