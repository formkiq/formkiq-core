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

import com.formkiq.aws.dynamodb.ApiPermission;
import com.formkiq.aws.dynamodb.BatchGetConfig;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchResponseFields;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.model.SearchTagCriteriaRange;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorImpl;
import com.formkiq.stacks.dynamodb.folders.FolderPermissionAttributePredicate;
import com.formkiq.stacks.dynamodb.schemas.SchemaCompositeKeyRecord;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.GLOBAL_FOLDER_METADATA;
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
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;

/**
 * 
 * Implementation {@link DocumentSearchService}.
 *
 */
public final class DocumentSearchServiceImpl implements DocumentSearchService {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private final DocumentService docService;
  /** Documents Table Name. */
  private final String documentTableName;
  /** {@link FolderIndexProcessor}. */
  private final FolderIndexProcessor folderIndexProcesor;
  /** {@link SchemaService}. */
  private final SchemaService schemaService;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentService {@link DocumentService}
   * @param documentsTable {@link String}
   */
  public DocumentSearchServiceImpl(final DynamoDbConnectionBuilder connection,
      final DocumentService documentService, final String documentsTable) {

    this.dbClient = connection.build();
    this.docService = documentService;

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.documentTableName = documentsTable;
    this.db = new DynamoDbServiceImpl(connection, documentsTable);
    this.folderIndexProcesor = new FolderIndexProcessorImpl(connection, documentsTable);
    this.schemaService = new SchemaServiceDynamodb(this.db);
  }

  private void addMatchAttributes(final List<Map<String, AttributeValue>> items,
      final List<DynamicDocumentItem> results) {

    List<Map<String, AttributeValue>> keys = items.stream()
        .map(r -> Map.of(DbKeys.PK, r.get(DbKeys.PK), DbKeys.SK, r.get(DbKeys.SK))).toList();

    BatchGetConfig config = new BatchGetConfig();
    List<Map<String, AttributeValue>> attributes = this.db.getBatch(config, keys);

    Map<String, List<Map<String, AttributeValue>>> matchedTags =
        attributes.stream().collect(Collectors.groupingBy(r -> r.get("documentId").s()));

    results.forEach(r -> {

      List<Map<String, AttributeValue>> attributeMatches = matchedTags.get(r.getDocumentId());
      Map<String, AttributeValue> attributeMatch = attributeMatches.get(0);

      Map<String, Object> values = getAttributeValuesMap(attributeMatch);
      r.put("matchedAttribute", values);
    });
  }

  /**
   * Add Response Fields to {@link DynamicDocumentItem}.
   * 
   * @param siteId {@link String}
   * @param results {@link List} {@link DynamicDocumentItem}
   * @param searchResponseFields {@link SearchResponseFields}
   */
  private void addResponseFields(final String siteId, final List<DynamicDocumentItem> results,
      final SearchResponseFields searchResponseFields) {

    final int limit = 1000;
    if (searchResponseFields != null) {

      Set<String> keyNames = new HashSet<>(notNull(searchResponseFields.getAttributes()));

      for (DynamicDocumentItem item : results) {

        DocumentAttributeRecord sr = new DocumentAttributeRecord();
        sr.setDocumentId(item.getDocumentId());

        QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE)
            .projectionExpression("#key,valueType,stringValue,numberValue,booleanValue")
            .expressionAttributeNames(Map.of("#key", "key"));

        AttributeValue pk = sr.fromS(sr.pk(siteId));
        AttributeValue sk = sr.fromS(ATTR);
        QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, limit);

        List<DocumentAttributeRecord> records =
            notNull(response.items()).stream().filter(a -> keyNames.contains(a.get("key").s()))
                .map(a -> new DocumentAttributeRecord().getFromAttributes(siteId, a)).toList();

        Collection<Map<String, Object>> attributes =
            new DocumentAttributeRecordToMap(true).apply(records);

        Map<String, Object> attributeFields = new HashMap<>();

        attributes.forEach(a -> {
          if (a.containsKey("stringValue")) {
            a.put("stringValues", List.of(a.get("stringValue")));
            a.remove("stringValue");
          } else if (a.containsKey("numberValue")) {
            a.put("numberValues", List.of(a.get("numberValue")));
            a.remove("numberValue");
          }
        });

        attributes.forEach(a -> {

          DocumentAttributeValueType vt =
              DocumentAttributeValueType.valueOf((String) a.get("valueType"));
          switch (vt) {
            case BOOLEAN -> attributeFields.put((String) a.get("key"),
                Map.of("valueType", a.get("valueType"), "booleanValue", a.get("booleanValue")));
            case NUMBER -> attributeFields.put((String) a.get("key"),
                Map.of("valueType", a.get("valueType"), "numberValues", a.get("numberValues")));
            case STRING, COMPOSITE_STRING, RELATIONSHIPS, CLASSIFICATION, PUBLICATION ->
              attributeFields.put((String) a.get("key"),
                  Map.of("stringValues", a.get("stringValues"), "valueType", a.get("valueType")));
            default ->
              attributeFields.put((String) a.get("key"), Map.of("valueType", a.get("valueType")));
          }
        });

        item.put("attributes", attributeFields);
      }
    }
  }

  private SearchAttributeCriteria createAttributesCriteria(final String siteId,
      final SearchQuery query) throws ValidationException {

    List<SearchAttributeCriteria> attributes = query.getAttributes();
    SchemaCompositeKeyRecord compositeKey = validateSearchAttributeCriteria(siteId, attributes);
    return new SearchAttributesToCriteria(compositeKey).apply(attributes);
  }

  private QueryRequest createQueryRequest(final String index, final String expression,
      final Map<String, AttributeValue> values, final PaginationMapToken token,
      final int maxresults, final Boolean scanIndexForward, final String projectionExpression) {
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    return QueryRequest.builder().tableName(this.documentTableName).indexName(index)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .projectionExpression(projectionExpression).exclusiveStartKey(startkey)
        .scanIndexForward(scanIndexForward).limit(maxresults).build();
  }

  /**
   * Filter {@link AttributeValue} by {@link SearchTagCriteria}.
   * 
   * @param search {@link SearchTagCriteria}
   * @param v {@link AttributeValue}
   * @return boolean
   */
  private boolean filterByValue(final SearchTagCriteria search, final AttributeValue v) {
    boolean filter = false;

    if (search.beginsWith() != null) {
      filter = v.s().startsWith(search.beginsWith());
    } else if (!Objects.notNull(search.eqOr()).isEmpty()) {
      filter = search.eqOr().contains(v.s());
    } else if (search.eq() != null) {
      filter = v.s().equals(search.eq());
    }

    return filter;
  }

  /**
   * Filter Document Tags.
   * 
   * @param docMap {@link Map}
   * @param search {@link SearchTagCriteria}
   * @return {@link Map}
   */
  private Map<String, Map<String, AttributeValue>> filterDocumentTags(
      final Map<String, Map<String, AttributeValue>> docMap, final SearchTagCriteria search) {

    Map<String, Map<String, AttributeValue>> map = docMap;

    if (hasFilter(search)) {

      map = map.entrySet().stream().filter(x -> {

        AttributeValue value = x.getValue().get("tagValue");
        AttributeValue values = x.getValue().get("tagValues");

        boolean result = false;
        if (values != null) {

          Optional<AttributeValue> val =
              values.l().stream().filter(v -> filterByValue(search, v)).findFirst();

          result = val.isPresent();
          if (result) {
            x.getValue().remove("tagValues");
            x.getValue().put("tagValue", val.get());
          }

        } else if (value != null) {
          result = filterByValue(search, value);
        }

        return result;

      }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    return map;
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB siteId.
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param range {@link List} {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsTagRange(final String siteId,
      final SearchQuery query, final String key, final SearchTagCriteriaRange range,
      final PaginationMapToken token, final int maxresults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and " + GSI2_SK + " between :start and :end";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":start", AttributeValue.fromS(range.getStart()));
    values.put(":end", AttributeValue.fromS(range.getEnd()));

    QueryRequest q = createQueryRequest(GSI2, expression, values, token, maxresults, Boolean.TRUE,
        projectionExpression);

    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document Tag records.
   * 
   * @param siteId DynamoDB siteId.
   * @param documentIds {@link Collection} {@link String}
   * @param tagKey {@link String}
   * @return {@link Map}
   */
  private Map<String, Map<String, AttributeValue>> findDocumentsTags(final String siteId,
      final Collection<String> documentIds, final String tagKey) {

    Map<String, Map<String, AttributeValue>> map = new HashMap<>();

    List<Map<String, AttributeValue>> keys = documentIds.stream()
        .map(id -> Map.of(PK,
            AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_DOCS + id)).build(), SK,
            AttributeValue.builder().s(PREFIX_TAGS + tagKey).build()))
        .collect(Collectors.toList());

    Map<String, KeysAndAttributes> items =
        Map.of(this.documentTableName, KeysAndAttributes.builder().keys(keys).build());
    BatchGetItemRequest batchReq = BatchGetItemRequest.builder().requestItems(items).build();
    BatchGetItemResponse batchResponse = this.dbClient.batchGetItem(batchReq);

    Collection<List<Map<String, AttributeValue>>> values = batchResponse.responses().values();

    if (!values.isEmpty()) {
      List<Map<String, AttributeValue>> list = values.iterator().next();

      list.forEach(m -> {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("type", m.get("type"));
        item.put("tagKey", m.get("tagKey"));

        if (m.containsKey("tagValue")) {
          item.put("tagValue", m.get("tagValue"));
        }

        if (m.containsKey("tagValues")) {
          item.put("tagValues", m.get("tagValues"));
        }

        String documentId = m.get("documentId").s();
        map.put(documentId, item);
      });
    }

    return map;
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB siteId.
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsTagStartWith(final String siteId,
      final SearchQuery query, final String key, final String value, final PaginationMapToken token,
      final int maxresults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":sk", AttributeValue.fromS(value));

    QueryRequest q = createQueryRequest(GSI2, expression, values, token, maxresults, Boolean.FALSE,
        projectionExpression);

    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey.
   *
   * @param siteId DynamoDB siteId Key
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTag(final String siteId,
      final SearchQuery query, final String key, final PaginationMapToken token,
      final int maxresults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk",
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAG + key)).build());

    QueryRequest q = createQueryRequest(GSI2, expression, values, token, maxresults, Boolean.FALSE,
        projectionExpression);
    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTagAndValue(final String siteId,
      final SearchQuery query, final String key, final String value, final PaginationMapToken token,
      final int maxresults, final String projectionExpression) {

    String expression = GSI1_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.builder()
        .s(createDatabaseKey(siteId, PREFIX_TAG + key + TAG_DELIMINATOR + value)).build());
    QueryRequest q = createQueryRequest(GSI1, expression, values, token, maxresults, Boolean.FALSE,
        projectionExpression);

    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey & tagValues.
   *
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param eqOr {@link Collection} {@link String}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTagAndValues(final String siteId,
      final SearchQuery query, final String key, final Collection<String> eqOr,
      final int maxresults, final String projectionExpression) {

    List<DynamicDocumentItem> list = new ArrayList<>();
    for (String eq : eqOr) {
      PaginationResults<DynamicDocumentItem> result = findDocumentsWithTagAndValue(siteId, query,
          key, eq, null, maxresults, projectionExpression);
      list.addAll(result.getResults());
    }

    return new PaginationResults<>(list, null);
  }

  @Override
  public PaginationResults<DynamicDocumentItem> findInFolder(final String siteId,
      final String indexKey, final PaginationMapToken token, final int maxresults) {

    String path = this.folderIndexProcesor.toPath(siteId, indexKey);
    DynamicObject o = this.folderIndexProcesor.getIndex(siteId, indexKey, false);

    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;
    if (o != null) {
      value += o.getString("documentId");
    }

    return searchByMeta(siteId, value, null, token, maxresults, path);
  }

  private Map<String, Object> getAttributeValuesMap(
      final Map<String, AttributeValue> attributeMatch) {

    DocumentAttributeValueType type =
        DocumentAttributeValueType.valueOf(attributeMatch.get("valueType").s());

    Map<String, Object> values = new HashMap<>();
    values.put("key", attributeMatch.get("key").s());

    switch (type) {
      case BOOLEAN:
        values.put("booleanValue", attributeMatch.get("booleanValue").bool());
        break;

      case NUMBER:
        Double value = Double.valueOf(attributeMatch.get("numberValue").n());
        values.put("numberValue", value);
        break;

      case STRING:
      case COMPOSITE_STRING:
        values.put("stringValue", attributeMatch.get("stringValue").s());
        break;

      default:
        break;
    }
    return values;
  }

  private String getFolderMetaDataKey(final String siteId, final SearchMetaCriteria meta) {
    String eq = meta.eq();

    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;
    if (!StringUtils.isBlank(eq)) {

      try {
        Map<String, Object> map = this.folderIndexProcesor.getIndex(siteId, eq + "/");

        if (map.containsKey("documentId")) {
          value += map.get("documentId");
        }

      } catch (IOException e) {
        value = null;
      }
    }

    return value;
  }

  private String getMetaDataKey(final String siteId, final SearchMetaCriteria meta) {

    String value;

    if ("folder".equals(meta.indexType())) {
      value = getFolderMetaDataKey(siteId, meta);
    } else {
      value = DbKeys.GLOBAL_FOLDER_TAGS;
    }

    return value;
  }

  /**
   * Get Search Key.
   * 
   * @param search {@link SearchTagCriteria}
   * @return {@link String}
   */
  protected String getSearchKey(final SearchTagCriteria search) {
    return search.key();
  }

  /**
   * {@link SearchTagCriteria} has filter criteria.
   * 
   * @param search {@link SearchTagCriteria}
   * @return boolean
   */
  private boolean hasFilter(final SearchTagCriteria search) {
    return search.eq() != null || search.beginsWith() != null
        || !Objects.notNull(search.eqOr()).isEmpty();
  }

  @Override
  public PaginationResults<DynamicDocumentItem> search(final String siteId, final SearchQuery query,
      final SearchResponseFields searchResponseFields, final PaginationMapToken token,
      final int maxresults) throws ValidationException {

    SearchMetaCriteria meta = query.getMeta();
    PaginationResults<DynamicDocumentItem> results;

    if (meta != null) {

      if (meta.path() != null) {

        try {
          Map<String, Object> map = this.folderIndexProcesor.getIndex(siteId, meta.path());
          String documentId = (String) map.get("documentId");

          DocumentItem item = this.docService.findDocument(siteId, documentId);

          DynamicDocumentItem result = new DocumentItemToDynamicDocumentItem().apply(item);
          results = new PaginationResults<>(Collections.singletonList(result), null);

        } catch (IOException e) {
          results = new PaginationResults<>(Collections.emptyList(), null);
        }

      } else {
        String path = updateFolderMetaData(meta);
        results = searchByMeta(siteId, meta, token, maxresults, path);
      }

    } else if (query.getAttribute() != null || !notNull(query.getAttributes()).isEmpty()) {

      SearchAttributeCriteria search = query.getAttribute();

      if (!notNull(query.getAttributes()).isEmpty()) {

        Collection<String> list = query.getAttributes().stream()
            .map(SearchAttributeCriteria::getKey).collect(Collectors.toSet());

        if (list.size() != query.getAttributes().size()) {
          throw new ValidationException(Collections
              .singletonList(new ValidationErrorImpl().error("duplicate attributes in query")));
        }

        search = createAttributesCriteria(siteId, query);
      }

      results = searchByAttribute(siteId, query, search, token, maxresults);

    } else if (query.getTag() != null) {

      SearchTagCriteria search = query.getTag();
      results = searchByTag(siteId, query, search, token, maxresults, null);

    } else {

      results = searchByDocumentIds(siteId, query.getDocumentIds());
    }

    addResponseFields(siteId, results.getResults(), searchResponseFields);
    return results;
  }

  private PaginationResults<DynamicDocumentItem> searchByDocumentIds(final String siteId,
      final Collection<String> documentIds) {

    List<DocumentItem> list = this.docService.findDocuments(siteId, new ArrayList<>(documentIds));

    List<DynamicDocumentItem> results = list != null
        ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l)).toList()
        : Collections.emptyList();

    return new PaginationResults<>(results, null);
  }

  private QueryResponse searchAttributeBeginsWith(final String siteId,
      final SearchAttributeCriteria search, final DocumentAttributeRecord sr,
      final QueryConfig config, final Map<String, AttributeValue> startkey, final int limit) {

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(search.getBeginsWith());

    return this.db.queryBeginsWith(config, pk, sk, startkey, limit);
  }

  private List<Map<String, AttributeValue>> searchAttributeDocumentIds(final String siteId,
      final SearchQuery query, final SearchAttributeCriteria search) {

    List<Map<String, AttributeValue>> list;
    Collection<String> documentIds = query.getDocumentIds();

    if (!Strings.isEmpty(search.getEq()) || !Objects.isEmpty(search.getEqOr())) {

      list = searchAttributeEqDocumentIds(siteId, search, documentIds);

    } else {

      list = searchAttributeOtherDocumentIds(siteId, search, documentIds);
    }

    return list;
  }

  private QueryResponse searchAttributeEq(final String siteId, final SearchAttributeCriteria search,
      final DocumentAttributeRecord sr, final QueryConfig config,
      final Map<String, AttributeValue> startkey, final int limit) {

    sr.setValueType(DocumentAttributeValueType.STRING);
    sr.setStringValue(search.getEq());

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(sr.skGsi1());

    return this.db.query(config, pk, sk, startkey, limit);
  }

  private List<Map<String, AttributeValue>> searchAttributeEqDocumentIds(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    List<String> eqs = new ArrayList<>();
    if (!Strings.isEmpty(search.getEq())) {
      eqs.add(search.getEq());
    }

    eqs.addAll(Objects.notNull(search.getEqOr()));

    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    String key = search.getKey();

    for (String documentId : documentIds) {

      List<Map<String, AttributeValue>> keyList = eqs.stream().map(eq -> {
        DocumentAttributeRecord sr = new DocumentAttributeRecord().setKey(key)
            .setValueType(DocumentAttributeValueType.STRING).setStringValue(eq)
            .setDocumentId(documentId);
        return Map.of(PK, sr.fromS(sr.pk(siteId)), SK, sr.fromS(sr.sk()));
      }).toList();
      keys.addAll(keyList);
    }

    return this.db.getBatch(new BatchGetConfig(), keys);
  }

  private List<Map<String, AttributeValue>> searchAttributeEqOr(final String siteId,
      final SearchAttributeCriteria search, final DocumentAttributeRecord sr,
      final QueryConfig config, final int limit) {

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    Collection<String> eqOr = search.getEqOr();
    for (String eq : eqOr) {
      search.eq(eq);
      List<Map<String, AttributeValue>> items =
          searchAttributeEq(siteId, search, sr, config, null, limit).items();
      list.addAll(items);
    }

    return list;
  }

  private List<Map<String, AttributeValue>> searchAttributeOtherDocumentIds(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    String key = search.getKey();
    DocumentAttributeRecord sr = new DocumentAttributeRecord().setKey(key);
    QueryConfig config = new QueryConfig();

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    for (String documentId : documentIds) {

      sr.setDocumentId(documentId);

      AttributeValue pk = sr.fromS(sr.pk(siteId));
      String sk = search.getBeginsWith() != null ? ATTR + key + "#" + search.getBeginsWith()
          : ATTR + key + "#";

      QueryResponse response = this.db.queryBeginsWith(config, pk, sr.fromS(sk), null, 1);
      list.addAll(response.items());
    }

    if (search.getRange() != null) {

      String start = search.getRange().getStart();
      String end = search.getRange().getEnd();

      list = list.stream().filter(a -> {

        boolean match = false;

        if (a.containsKey("stringValue")) {
          String s = a.get("stringValue").s();
          match = start.compareTo(s) <= 0 && s.compareTo(end) <= 0;
        }

        return match;
      }).toList();
    }

    return list;
  }

  private QueryResponse searchAttributeRange(final String siteId,
      final SearchAttributeCriteria search, final DocumentAttributeRecord sr,
      final QueryConfig config, final Map<String, AttributeValue> startkey, final int limit) {

    SearchTagCriteriaRange range = search.getRange();

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk0 = AttributeValue.fromS(range.getStart());
    AttributeValue sk1 = range.getEnd() != null ? AttributeValue.fromS(range.getEnd()) : null;

    return this.db.between(config, pk, sk0, sk1, startkey, limit);
  }

  private PaginationResults<DynamicDocumentItem> searchByAttribute(final String siteId,
      final SearchQuery query, final SearchAttributeCriteria search, final PaginationMapToken token,
      final int limit) throws ValidationException {

    validate(search);

    DocumentAttributeRecord sr = new DocumentAttributeRecord().setKey(search.getKey());
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);

    QueryResponse response = null;
    PaginationMapToken pagination = null;
    List<Map<String, AttributeValue>> items;

    if (!Objects.isEmpty(query.getDocumentIds())) {

      items = searchAttributeDocumentIds(siteId, query, search);

    } else if (!Strings.isEmpty(search.getEq())) {

      response = searchAttributeEq(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else if (!Objects.isEmpty(search.getEqOr())) {

      items = searchAttributeEqOr(siteId, search, sr, config, limit);

    } else if (search.getRange() != null) {

      response = searchAttributeRange(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else if (search.getBeginsWith() != null) {

      response = searchAttributeBeginsWith(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else {

      config.indexName(GSI1);
      AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
      response = this.db.query(config, pk, startkey, limit);
      items = response.items();
    }

    if (response != null) {
      pagination = new QueryResponseToPagination().apply(response);
    }

    List<String> documentIds =
        items.stream().map(i -> i.get("documentId").s()).distinct().collect(Collectors.toList());

    List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

    List<DynamicDocumentItem> results =
        list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
            .collect(Collectors.toList()) : Collections.emptyList();

    addMatchAttributes(items, results);

    return new PaginationResults<>(results, pagination);
  }

  /**
   * Perform Meta Data search.
   * 
   * @param siteId {@link String}
   * @param meta {@link SearchMetaCriteria}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param path {@link String}
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> searchByMeta(final String siteId,
      final SearchMetaCriteria meta, final PaginationMapToken token, final int maxresults,
      final String path) {

    String value = getMetaDataKey(siteId, meta);
    PaginationResults<DynamicDocumentItem> results =
        searchByMeta(siteId, value, meta.indexFilterBeginsWith(), token, maxresults, path);

    if ("folder".equals(meta.indexType())) {
      results.getResults().removeIf(r -> r.get("documentId") == null);
    }

    return results;
  }

  private PaginationResults<DynamicDocumentItem> searchByMeta(final String siteId,
      final String value, final String indexFilterBeginsWith, final PaginationMapToken token,
      final int maxresults, final String path) {

    PaginationResults<DynamicDocumentItem> result;

    if (value != null) {

      String expression = PK + " = :pk";
      Map<String, AttributeValue> values = new HashMap<>();
      values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, value)).build());

      if (indexFilterBeginsWith != null) {
        expression = PK + " = :pk and begins_with(" + SK + ", :sk)";
        values.put(":sk", AttributeValue.builder().s(indexFilterBeginsWith).build());
      }

      QueryRequest q =
          createQueryRequest(null, expression, values, token, maxresults, Boolean.TRUE, null);

      result = searchForMetaDocuments(q, siteId, path);

    } else {
      result = new PaginationResults<>(Collections.emptyList(), null);
    }

    return result;
  }

  private PaginationResults<DynamicDocumentItem> searchByTag(final String siteId,
      final SearchQuery query, final SearchTagCriteria csearch, final PaginationMapToken token,
      final int maxresults, final String projectionExpression) {

    SearchTagCriteria search = csearch;

    String key = getSearchKey(search);

    PaginationResults<DynamicDocumentItem> result;

    Collection<String> documentIds = query.getDocumentIds();

    if (!Objects.isEmpty(documentIds)) {

      Map<String, Map<String, AttributeValue>> docs = findDocumentsTags(siteId, documentIds, key);

      Map<String, Map<String, AttributeValue>> filteredDocs = filterDocumentTags(docs, search);

      List<String> fetchDocumentIds = new ArrayList<>(filteredDocs.keySet());

      List<DocumentItem> list = this.docService.findDocuments(siteId, fetchDocumentIds);

      List<DynamicDocumentItem> results =
          list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
              .collect(Collectors.toList()) : Collections.emptyList();

      results.forEach(r -> {
        Map<String, AttributeValue> tagMap = filteredDocs.get(r.getDocumentId());
        DocumentTag tag = new AttributeValueToDocumentTag(siteId).apply(tagMap);
        r.put("matchedTag", new DocumentTagToDynamicDocumentTag().apply(tag));
      });

      result = new PaginationResults<>(results, null);

    } else {

      if (!Objects.isEmpty(search.eqOr())) {
        result = findDocumentsWithTagAndValues(siteId, query, key, search.eqOr(), maxresults,
            projectionExpression);
      } else if (search.eq() != null) {
        result = findDocumentsWithTagAndValue(siteId, query, key, search.eq(), token, maxresults,
            projectionExpression);
      } else if (search.beginsWith() != null) {
        result = findDocumentsTagStartWith(siteId, query, key, search.beginsWith(), token,
            maxresults, projectionExpression);
      } else if (search.range() != null) {
        result = findDocumentsTagRange(siteId, query, key, search.range(), token, maxresults,
            projectionExpression);
      } else {
        result = findDocumentsWithTag(siteId, query, key, token, maxresults, projectionExpression);
      }
    }

    return result;
  }

  @Override
  public PaginationResults<String> searchForDocumentIds(final String siteId,
      final SearchTagCriteria criteria, final PaginationMapToken token, final int maxresults) {

    SearchQuery query = new SearchQuery();
    String projectionExpression = "documentId";
    PaginationResults<DynamicDocumentItem> searchByTag =
        searchByTag(siteId, query, criteria, token, maxresults, projectionExpression);

    List<String> documentIds = searchByTag.getResults().stream().map(m -> m.getString("documentId"))
        .collect(Collectors.toList());

    return new PaginationResults<>(documentIds, searchByTag.getToken());
  }

  /**
   * Search for Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   */
  private PaginationResults<DynamicDocumentItem> searchForDocuments(final QueryRequest q,
      final String siteId, final SearchQuery query) {

    String projectionExpression = q.projectionExpression();
    QueryResponse result = this.dbClient.query(q);

    List<String> documentIds = result.items().stream().map(i -> i.get("documentId").s()).distinct()
        .collect(Collectors.toList());

    Map<String, DocumentTag> tags = transformToDocumentTagMap(result);

    PaginationResults<DynamicDocumentItem> ret;

    if (!"documentId".equals(projectionExpression)) {

      List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

      List<DynamicDocumentItem> results =
          list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
              .collect(Collectors.toList()) : Collections.emptyList();

      results.forEach(r -> {

        DocumentTag tag = tags.get(r.getDocumentId());
        r.put("matchedTag", new DocumentTagToDynamicDocumentTag().apply(tag));

        if (!notNull(query.getTags()).isEmpty()) {
          updateToMatchedTags(query, r);
        }
      });

      ret = new PaginationResults<>(results, new QueryResponseToPagination().apply(result));

    } else {

      List<DynamicDocumentItem> results = documentIds.stream()
          .map(d -> new DynamicDocumentItem(Map.of("documentId", d))).collect(Collectors.toList());

      ret = new PaginationResults<>(results, new QueryResponseToPagination().apply(result));
    }

    return ret;
  }

  /**
   * Search for Meta Data Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param path {@link String}
   * @return {@link PaginationResults} {@link DynamicDocumentItem}
   */
  private PaginationResults<DynamicDocumentItem> searchForMetaDocuments(final QueryRequest q,
      final String siteId, final String path) {

    QueryResponse result = this.dbClient.query(q);

    List<String> documentIds = result.items().stream().filter(r -> r.containsKey("documentId"))
        .map(r -> r.get("documentId").s()).distinct().collect(Collectors.toList());

    List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

    Map<String, DocumentItem> documentMap = list != null
        ? list.stream().collect(Collectors.toMap(DocumentItem::getDocumentId, Function.identity()))
        : Collections.emptyMap();

    AttributeValueToGlobalMetaFolder metaFolder = new AttributeValueToGlobalMetaFolder();
    DocumentItemToDynamicDocumentItem transform = new DocumentItemToDynamicDocumentItem();

    FolderPermissionAttributePredicate pred =
        new FolderPermissionAttributePredicate(db, ApiPermission.READ, path);
    List<Map<String, AttributeValue>> apply = pred.apply(siteId, result.items());
    List<DynamicDocumentItem> results = apply.stream().map(r -> {

      AttributeValue documentId = r.get("documentId");
      boolean isDocument = documentId != null && documentMap.containsKey(documentId.s());

      return isDocument ? transform.apply(documentMap.get(r.get("documentId").s()))
          : new DynamicDocumentItem(metaFolder.apply(r));

    }).collect(Collectors.toList());

    return new PaginationResults<>(results, new QueryResponseToPagination().apply(result));
  }

  /**
   * Transform {@link QueryResponse} to {@link DocumentTag} {@link Map}.
   * 
   * @param result {@link QueryResponse}
   * @return {@link Map} {@link DocumentTag}
   */
  private Map<String, DocumentTag> transformToDocumentTagMap(final QueryResponse result) {

    Map<String, DocumentTag> tags = new HashMap<>();
    result.items().forEach(s -> {

      if (s.containsKey("documentId")) {
        String documentId = s.get("documentId").s();

        String tagKey = s.containsKey("tagKey") ? s.get("tagKey").s() : null;
        String tagValue = s.containsKey("tagValue") ? s.get("tagValue").s() : "";

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

  private String updateFolderMetaData(final SearchMetaCriteria meta) {

    String folder = meta.folder();

    if (folder != null) {

      if (folder.endsWith("/")) {
        folder = folder.substring(0, folder.length() - 1);
      }

      meta.indexType("folder");
      meta.eq(folder);
      meta.folder(null);
    }

    return folder;
  }

  /**
   * Update Search Result from matchedTag to matchedTags.
   * 
   * @param query {@link SearchQuery}
   * @param r {@link DynamicDocumentItem}
   */
  private void updateToMatchedTags(final SearchQuery query, final DynamicDocumentItem r) {

    List<DynamicDocumentItem> matchedTags = new ArrayList<>();

    List<SearchTagCriteria> tags = query.getTags();
    for (SearchTagCriteria c : tags) {
      DynamicDocumentItem tag = new DynamicDocumentItem(new HashMap<>());
      tag.put("key", c.key());
      tag.put("value", c.eq());
      tag.put("type", DocumentTagType.USERDEFINED.name());
      matchedTags.add(tag);
    }

    r.put("matchedTags", matchedTags);
    r.remove("matchedTag");
  }

  private void validate(final SearchAttributeCriteria search) throws ValidationException {

    Collection<ValidationError> errors = new ArrayList<>();

    SearchTagCriteriaRange range = search.getRange();

    if (range != null) {
      if (Strings.isEmpty(range.getStart())) {
        errors.add(new ValidationErrorImpl().key("start").error("'start' is required"));
      }

      if (Strings.isEmpty(range.getEnd())) {
        errors.add(new ValidationErrorImpl().key("end").error("'end' is required"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  private SchemaCompositeKeyRecord validateSearchAttributeCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) throws ValidationException {

    SchemaCompositeKeyRecord compositeKey = null;
    List<ValidationError> errors = new ArrayList<>();

    if (attributes.size() > 1) {
      List<String> attributeKeys =
          attributes.stream().map(SearchAttributeCriteria::getKey).toList();

      compositeKey = this.schemaService.getCompositeKey(siteId, attributeKeys);
      if (compositeKey == null) {
        errors.add(new ValidationErrorImpl().error(
            "no composite key found for attributes '" + String.join(",", attributeKeys) + "'"));
      }
    }

    for (int i = 0; i < attributes.size() - 1; i++) {

      SearchAttributeCriteria c = attributes.get(i);

      if (!isEmpty(c.getBeginsWith())) {
        errors.add(new ValidationErrorImpl().key("beginsWith")
            .error("'beginsWith' can only be used on last attribute in list"));
      } else if (c.getRange() != null) {
        errors.add(new ValidationErrorImpl().key("range")
            .error("'range' can only be used on last attribute in list"));
      } else if (!notNull(c.getEqOr()).isEmpty()) {
        errors.add(new ValidationErrorImpl().key("range")
            .error("'eqOr' is not supported with composite keys"));
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }

    return compositeKey;
  }
}
