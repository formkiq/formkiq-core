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
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.folders.GetFolderFilesByName;
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
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.stacks.dynamodb.base64.Pagination;
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

      Set<String> keyNames = new HashSet<>(notNull(searchResponseFields.attributes()));

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

    List<SearchAttributeCriteria> attributes = query.attributes();
    SchemaCompositeKeyRecord compositeKey = validateSearchAttributeCriteria(siteId, attributes);
    return new SearchAttributesToCriteria(compositeKey).apply(attributes);
  }

  private QueryRequest createQueryRequest(final String index, final String expression,
      final Map<String, AttributeValue> values, final String nextToken, final int maxresults,
      final Boolean scanIndexForward, final String projectionExpression) {

    Map<String, AttributeValue> startkey = new StringToMapAttributeValue().apply(nextToken);

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
   * @param nextToken {@link String}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> findDocumentsTagRange(final String siteId,
      final SearchQuery query, final String key, final SearchTagCriteriaRange range,
      final String nextToken, final int maxresults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and " + GSI2_SK + " between :start and :end";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":start", AttributeValue.fromS(range.start()));
    values.put(":end", AttributeValue.fromS(range.end()));

    QueryRequest q = createQueryRequest(GSI2, expression, values, nextToken, maxresults,
        Boolean.TRUE, projectionExpression);

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
   * @param nextToken {@link String}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> findDocumentsTagStartWith(final String siteId,
      final SearchQuery query, final String key, final String value, final String nextToken,
      final int maxresults, final String projectionExpression) {

    String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(createDatabaseKey(siteId, PREFIX_TAG + key)));
    values.put(":sk", AttributeValue.fromS(value));

    QueryRequest q = createQueryRequest(GSI2, expression, values, nextToken, maxresults,
        Boolean.FALSE, projectionExpression);

    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey.
   *
   * @param siteId DynamoDB siteId Key
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param nextToken {@link PaginationMapToken}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> findDocumentsWithTag(final String siteId,
      final SearchQuery query, final String key, final String nextToken, final int maxresults,
      final String projectionExpression) {

    String expression = GSI2_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk",
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAG + key)).build());

    QueryRequest q = createQueryRequest(GSI2, expression, values, nextToken, maxresults,
        Boolean.FALSE, projectionExpression);
    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param value {@link String}
   * @param nextToken {@link String}
   * @param maxresults int
   * @param projectionExpression {@link String}
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> findDocumentsWithTagAndValue(final String siteId,
      final SearchQuery query, final String key, final String value, final String nextToken,
      final int maxresults, final String projectionExpression) {

    String expression = GSI1_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.builder()
        .s(createDatabaseKey(siteId, PREFIX_TAG + key + TAG_DELIMINATOR + value)).build());
    QueryRequest q = createQueryRequest(GSI1, expression, values, nextToken, maxresults,
        Boolean.FALSE, projectionExpression);

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
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> findDocumentsWithTagAndValues(final String siteId,
      final SearchQuery query, final String key, final Collection<String> eqOr,
      final int maxresults, final String projectionExpression) {

    List<DynamicDocumentItem> list = new ArrayList<>();
    for (String eq : eqOr) {
      Pagination<DynamicDocumentItem> result = findDocumentsWithTagAndValue(siteId, query, key, eq,
          null, maxresults, projectionExpression);
      list.addAll(result.getResults());
    }

    return new Pagination<>(list);
  }

  @Override
  public Pagination<DynamicDocumentItem> findInFolder(final String siteId, final String indexKey,
      final String nextToken, final int maxresults) {

    String path = this.folderIndexProcesor.toPath(siteId, indexKey);
    DynamicObject o = this.folderIndexProcesor.getIndex(siteId, indexKey, false);

    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;
    if (o != null) {
      value += o.getString("documentId");
    }

    return searchByMeta(siteId, value, null, nextToken, maxresults, path);
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
  public Pagination<DynamicDocumentItem> search(final String siteId, final SearchQuery query,
      final SearchResponseFields searchResponseFields, final String nextToken, final int limit)
      throws ValidationException {

    SearchMetaCriteria meta = query.meta();
    Pagination<DynamicDocumentItem> results;

    if (meta != null) {

      if (meta.path() != null) {

        try {
          Map<String, Object> map = this.folderIndexProcesor.getIndex(siteId, meta.path());
          String documentId = (String) map.get("documentId");

          DocumentItem item = this.docService.findDocument(siteId, documentId);

          DynamicDocumentItem result = new DocumentItemToDynamicDocumentItem().apply(item);
          results = new Pagination<>(Collections.singletonList(result));

        } catch (IOException e) {
          results = new Pagination<>(Collections.emptyList());
        }

      } else {
        meta = updateFolderMetaData(meta);
        String path = meta.eq();
        results = searchByMeta(siteId, meta, nextToken, limit, path);
      }

    } else if (query.filename() != null) {

      QueryResult result = new GetFolderFilesByName(query.filename().beginsWith()).query(db,
          db.getTableName(), siteId, nextToken, limit);

      List<String> documentIds =
          result.items().stream().map(i -> DynamoDbTypes.toString(i.get("documentId")))
              .filter(java.util.Objects::nonNull).toList();

      DocumentItemToDynamicDocumentItem transform = new DocumentItemToDynamicDocumentItem();
      var items = docService.findDocuments(siteId, documentIds).stream().map(transform).toList();

      results = new Pagination<>(items, result.lastEvaluatedKey());

    } else if (query.attribute() != null || !notNull(query.attributes()).isEmpty()) {

      SearchAttributeCriteria search = query.attribute();

      if (!notNull(query.attributes()).isEmpty()) {

        Collection<String> list = query.attributes().stream().map(SearchAttributeCriteria::key)
            .collect(Collectors.toSet());

        if (list.size() != query.attributes().size()) {
          throw new ValidationException(Collections
              .singletonList(new ValidationErrorImpl().error("duplicate attributes in query")));
        }

        search = createAttributesCriteria(siteId, query);
      }

      results = searchByAttribute(siteId, query, search, nextToken, limit);

    } else if (query.tag() != null) {

      SearchTagCriteria search = query.tag();
      results = searchByTag(siteId, query, search, nextToken, limit, null);

    } else {

      results = searchByDocumentIds(siteId, query.documentIds());
    }

    addResponseFields(siteId, results.getResults(), searchResponseFields);
    return results;
  }

  private Pagination<DynamicDocumentItem> searchByDocumentIds(final String siteId,
      final Collection<String> documentIds) {

    List<DocumentItem> list = this.docService.findDocuments(siteId, new ArrayList<>(documentIds));

    List<DynamicDocumentItem> results = list != null
        ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l)).toList()
        : Collections.emptyList();

    return new Pagination<>(results);
  }

  private QueryResponse searchAttributeBeginsWith(final String siteId,
      final SearchAttributeCriteria search, final DocumentAttributeRecord sr,
      final QueryConfig config, final Map<String, AttributeValue> startkey, final int limit) {

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(search.beginsWith());

    return this.db.queryBeginsWith(config, pk, sk, startkey, limit);
  }

  private List<Map<String, AttributeValue>> searchAttributeDocumentIds(final String siteId,
      final SearchQuery query, final SearchAttributeCriteria search) {

    List<Map<String, AttributeValue>> list;
    Collection<String> documentIds = query.documentIds();

    if (!Strings.isEmpty(search.eq()) || !Objects.isEmpty(search.eqOr())) {

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
    sr.setStringValue(search.eq());

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk = AttributeValue.fromS(sr.skGsi1());

    return this.db.query(config, pk, sk, startkey, limit);
  }

  private List<Map<String, AttributeValue>> searchAttributeEqDocumentIds(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    List<String> eqs = new ArrayList<>();
    if (!Strings.isEmpty(search.eq())) {
      eqs.add(search.eq());
    }

    eqs.addAll(Objects.notNull(search.eqOr()));

    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    String key = search.key();

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

    Collection<String> eqOr = search.eqOr();
    for (String eq : eqOr) {
      SearchAttributeCriteria c = new SearchAttributeCriteria(search.key(), null, eq, null, null);
      List<Map<String, AttributeValue>> items =
          searchAttributeEq(siteId, c, sr, config, null, limit).items();
      list.addAll(items);
    }

    return list;
  }

  private List<Map<String, AttributeValue>> searchAttributeOtherDocumentIds(final String siteId,
      final SearchAttributeCriteria search, final Collection<String> documentIds) {

    String key = search.key();
    DocumentAttributeRecord sr = new DocumentAttributeRecord().setKey(key);
    QueryConfig config = new QueryConfig();

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    for (String documentId : documentIds) {

      sr.setDocumentId(documentId);

      AttributeValue pk = sr.fromS(sr.pk(siteId));
      String sk =
          search.beginsWith() != null ? ATTR + key + "#" + search.beginsWith() : ATTR + key + "#";

      QueryResponse response = this.db.queryBeginsWith(config, pk, sr.fromS(sk), null, 1);
      list.addAll(response.items());
    }

    if (search.range() != null) {

      String start = search.range().start();
      String end = search.range().end();

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

    SearchTagCriteriaRange range = search.range();

    config.indexName(GSI1);
    AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
    AttributeValue sk0 = AttributeValue.fromS(range.start());
    AttributeValue sk1 = range.end() != null ? AttributeValue.fromS(range.end()) : null;

    return this.db.between(config, pk, sk0, sk1, startkey, limit);
  }

  private Pagination<DynamicDocumentItem> searchByAttribute(final String siteId,
      final SearchQuery query, final SearchAttributeCriteria search, final String nextToken,
      final int limit) throws ValidationException {

    validate(search);

    DocumentAttributeRecord sr = new DocumentAttributeRecord().setKey(search.key());
    Map<String, AttributeValue> startkey = new StringToMapAttributeValue().apply(nextToken);

    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE);

    QueryResponse response = null;
    PaginationMapToken pagination = null;
    List<Map<String, AttributeValue>> items;

    if (!Objects.isEmpty(query.documentIds())) {

      items = searchAttributeDocumentIds(siteId, query, search);

    } else if (!Strings.isEmpty(search.eq())) {

      response = searchAttributeEq(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else if (!Objects.isEmpty(search.eqOr())) {

      items = searchAttributeEqOr(siteId, search, sr, config, limit);

    } else if (search.range() != null) {

      response = searchAttributeRange(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else if (search.beginsWith() != null) {

      response = searchAttributeBeginsWith(siteId, search, sr, config, startkey, limit);
      items = response.items();

    } else {

      config.indexName(GSI1);
      AttributeValue pk = AttributeValue.fromS(sr.pkGsi1(siteId));
      response = this.db.query(config, pk, startkey, limit);
      items = response.items();
    }

    List<String> documentIds =
        items.stream().map(i -> i.get("documentId").s()).distinct().collect(Collectors.toList());

    List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

    List<DynamicDocumentItem> results =
        list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
            .collect(Collectors.toList()) : Collections.emptyList();

    addMatchAttributes(items, results);

    return new Pagination<>(results, response != null ? response.lastEvaluatedKey() : null);
  }

  /**
   * Perform Meta Data search.
   * 
   * @param siteId {@link String}
   * @param meta {@link SearchMetaCriteria}
   * @param nextToken {@link String}
   * @param maxresults int
   * @param path {@link String}
   * @return {@link Pagination}
   */
  private Pagination<DynamicDocumentItem> searchByMeta(final String siteId,
      final SearchMetaCriteria meta, final String nextToken, final int maxresults,
      final String path) {

    String value = getMetaDataKey(siteId, meta);
    Pagination<DynamicDocumentItem> results =
        searchByMeta(siteId, value, meta.indexFilterBeginsWith(), nextToken, maxresults, path);

    if ("folder".equals(meta.indexType())) {
      results.getResults().removeIf(r -> r.get("documentId") == null);
    }

    return results;
  }

  private Pagination<DynamicDocumentItem> searchByMeta(final String siteId, final String value,
      final String indexFilterBeginsWith, final String nextToken, final int maxresults,
      final String path) {

    Pagination<DynamicDocumentItem> result;

    if (value != null) {

      String expression = PK + " = :pk";
      Map<String, AttributeValue> values = new HashMap<>();
      values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, value)).build());

      if (indexFilterBeginsWith != null) {
        expression = PK + " = :pk and begins_with(" + SK + ", :sk)";
        values.put(":sk", AttributeValue.builder().s(indexFilterBeginsWith).build());
      }

      QueryRequest q =
          createQueryRequest(null, expression, values, nextToken, maxresults, Boolean.TRUE, null);

      result = searchForMetaDocuments(q, siteId, path);

    } else {
      result = new Pagination<>(Collections.emptyList());
    }

    return result;
  }

  private Pagination<DynamicDocumentItem> searchByTag(final String siteId, final SearchQuery query,
      final SearchTagCriteria search, final String nextToken, final int maxresults,
      final String projectionExpression) {

    String key = getSearchKey(search);

    Pagination<DynamicDocumentItem> result;

    Collection<String> documentIds = query.documentIds();

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

      result = new Pagination<>(results);

    } else {

      if (!Objects.isEmpty(search.eqOr())) {
        result = findDocumentsWithTagAndValues(siteId, query, key, search.eqOr(), maxresults,
            projectionExpression);
      } else if (search.eq() != null) {
        result = findDocumentsWithTagAndValue(siteId, query, key, search.eq(), nextToken,
            maxresults, projectionExpression);
      } else if (search.beginsWith() != null) {
        result = findDocumentsTagStartWith(siteId, query, key, search.beginsWith(), nextToken,
            maxresults, projectionExpression);
      } else if (search.range() != null) {
        result = findDocumentsTagRange(siteId, query, key, search.range(), nextToken, maxresults,
            projectionExpression);
      } else {
        result =
            findDocumentsWithTag(siteId, query, key, nextToken, maxresults, projectionExpression);
      }
    }

    return result;
  }

  @Override
  public Pagination<String> searchForDocumentIds(final String siteId,
      final SearchTagCriteria criteria, final String nextToken, final int maxresults) {

    SearchQuery query = new SearchQuery(null, null, null, null, null, null, null, null, null);
    String projectionExpression = "documentId";
    Pagination<DynamicDocumentItem> searchByTag =
        searchByTag(siteId, query, criteria, nextToken, maxresults, projectionExpression);

    List<String> documentIds = searchByTag.getResults().stream().map(m -> m.getString("documentId"))
        .collect(Collectors.toList());

    return new Pagination<>(documentIds, searchByTag.getNextToken());
  }

  /**
   * Search for Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @return {@link Pagination} {@link DynamicDocumentItem}
   */
  private Pagination<DynamicDocumentItem> searchForDocuments(final QueryRequest q,
      final String siteId, final SearchQuery query) {

    String projectionExpression = q.projectionExpression();
    QueryResponse result = this.dbClient.query(q);

    List<String> documentIds = result.items().stream().map(i -> i.get("documentId").s()).distinct()
        .collect(Collectors.toList());

    Map<String, DocumentTag> tags = transformToDocumentTagMap(result);

    Pagination<DynamicDocumentItem> ret;

    if (!"documentId".equals(projectionExpression)) {

      List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

      List<DynamicDocumentItem> results =
          list != null ? list.stream().map(l -> new DocumentItemToDynamicDocumentItem().apply(l))
              .collect(Collectors.toList()) : Collections.emptyList();

      results.forEach(r -> {

        DocumentTag tag = tags.get(r.getDocumentId());
        r.put("matchedTag", new DocumentTagToDynamicDocumentTag().apply(tag));

        if (!notNull(query.tags()).isEmpty()) {
          updateToMatchedTags(query, r);
        }
      });

      ret = new Pagination<>(results, result.lastEvaluatedKey());

    } else {

      List<DynamicDocumentItem> results = documentIds.stream()
          .map(d -> new DynamicDocumentItem(Map.of("documentId", d))).collect(Collectors.toList());

      ret = new Pagination<>(results, result.lastEvaluatedKey());
    }

    return ret;
  }

  /**
   * Search for Meta Data Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param path {@link String}
   * @return {@link Pagination} {@link DynamicDocumentItem}
   */
  private Pagination<DynamicDocumentItem> searchForMetaDocuments(final QueryRequest q,
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

    return new Pagination<>(results, result.lastEvaluatedKey());
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

  private SearchMetaCriteria updateFolderMetaData(final SearchMetaCriteria meta) {

    String folder = meta.folder();

    if (folder != null) {

      if (folder.endsWith("/")) {
        folder = folder.substring(0, folder.length() - 1);
      }

      return new SearchMetaCriteria(folder, null, meta.indexFilterBeginsWith(), "folder",
          meta.path());
      // meta.indexType("folder");
      // meta.eq(folder);
      // meta.folder(null);
    }

    // return folder;
    return meta;
  }

  /**
   * Update Search Result from matchedTag to matchedTags.
   * 
   * @param query {@link SearchQuery}
   * @param r {@link DynamicDocumentItem}
   */
  private void updateToMatchedTags(final SearchQuery query, final DynamicDocumentItem r) {

    List<DynamicDocumentItem> matchedTags = new ArrayList<>();

    List<SearchTagCriteria> tags = query.tags();
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

  private SchemaCompositeKeyRecord validateSearchAttributeCriteria(final String siteId,
      final List<SearchAttributeCriteria> attributes) throws ValidationException {

    SchemaCompositeKeyRecord compositeKey = null;
    List<ValidationError> errors = new ArrayList<>();

    if (attributes.size() > 1) {
      List<String> attributeKeys = attributes.stream().map(SearchAttributeCriteria::key).toList();

      compositeKey = this.schemaService.getCompositeKey(siteId, attributeKeys);
      if (compositeKey == null) {
        errors.add(new ValidationErrorImpl().error(
            "no composite key found for attributes '" + String.join(",", attributeKeys) + "'"));
      }
    }

    for (int i = 0; i < attributes.size() - 1; i++) {

      SearchAttributeCriteria c = attributes.get(i);

      if (!isEmpty(c.beginsWith())) {
        errors.add(new ValidationErrorImpl().key("beginsWith")
            .error("'beginsWith' can only be used on last attribute in list"));
      } else if (c.range() != null) {
        errors.add(new ValidationErrorImpl().key("range")
            .error("'range' can only be used on last attribute in list"));
      } else if (!notNull(c.eqOr()).isEmpty()) {
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
