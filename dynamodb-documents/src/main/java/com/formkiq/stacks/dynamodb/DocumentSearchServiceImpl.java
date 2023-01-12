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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.plugins.tagschema.DocumentTagSchemaPlugin;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * Implementation {@link DocumentSearchService}.
 *
 */
public class DocumentSearchServiceImpl implements DocumentSearchService {

  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private DocumentService docService;
  /** Documents Table Name. */
  private String documentTableName;
  /** {@link FolderIndexProcessor}. */
  private FolderIndexProcessor folderIndexProcesor;
  /** {@link DocumentTagSchemaPlugin}. */
  private DocumentTagSchemaPlugin tagSchemaPlugin;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentService {@link DocumentService}
   * @param documentsTable {@link String}
   * @param plugin {@link DocumentTagSchemaPlugin}
   */
  public DocumentSearchServiceImpl(final DynamoDbConnectionBuilder connection,
      final DocumentService documentService, final String documentsTable,
      final DocumentTagSchemaPlugin plugin) {

    this.dbClient = connection.build();
    this.docService = documentService;
    this.tagSchemaPlugin = plugin;

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.documentTableName = documentsTable;
    this.folderIndexProcesor = new FolderIndexProcessorImpl(connection, documentsTable);
  }

  private QueryRequest createQueryRequest(final String index, final String expression,
      final Map<String, AttributeValue> values, final PaginationMapToken token,
      final int maxresults, final Boolean scanIndexForward) {
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(index)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .exclusiveStartKey(startkey).scanIndexForward(scanIndexForward)
        .limit(Integer.valueOf(maxresults)).build();
    return q;
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

      }).collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
    }

    return map;
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
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsTagStartWith(final String siteId,
      final SearchQuery query, final String key, final String value, final PaginationMapToken token,
      final int maxresults) {

    String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk",
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAG + key)).build());
    values.put(":sk", AttributeValue.builder().s(value).build());

    QueryRequest q = createQueryRequest(GSI2, expression, values, token, maxresults, Boolean.FALSE);

    return searchForDocuments(q, siteId, query);
  }

  /**
   * Find Document that match tagKey.
   *
   * @param siteId DynamoDB siteId Key
   * @param query {@link SearchQuery}
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTag(final String siteId,
      final SearchQuery query, final String key, final String value, final PaginationMapToken token,
      final int maxresults) {

    String expression = GSI2_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk",
        AttributeValue.builder().s(createDatabaseKey(siteId, PREFIX_TAG + key)).build());

    QueryRequest q = createQueryRequest(GSI2, expression, values, token, maxresults, Boolean.FALSE);
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
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTagAndValue(final String siteId,
      final SearchQuery query, final String key, final String value, final PaginationMapToken token,
      final int maxresults) {

    String expression = GSI1_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder()
        .s(createDatabaseKey(siteId, PREFIX_TAG + key + TAG_DELIMINATOR + value)).build());
    QueryRequest q = createQueryRequest(GSI1, expression, values, token, maxresults, Boolean.FALSE);
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
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTagAndValues(final String siteId,
      final SearchQuery query, final String key, final Collection<String> eqOr,
      final int maxresults) {

    List<DynamicDocumentItem> list = new ArrayList<>();
    for (String eq : eqOr) {
      PaginationResults<DynamicDocumentItem> result =
          findDocumentsWithTagAndValue(siteId, query, key, eq, null, maxresults);
      list.addAll(result.getResults());
    }

    return new PaginationResults<DynamicDocumentItem>(list, null);
  }

  private String getFolderMetaDataKey(final String siteId, final SearchMetaCriteria meta) {
    String eq = meta.eq();

    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;
    if (!StringUtils.isBlank(eq)) {

      try {
        Map<String, String> map = this.folderIndexProcesor.getIndex(siteId, eq + "/");

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

    String value = null;

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
    String key = search.key();
    return key;
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
      final PaginationMapToken token, final int maxresults) {

    SearchMetaCriteria meta = query.meta();
    PaginationResults<DynamicDocumentItem> results = null;

    if (meta != null) {

      if (meta.path() != null) {

        try {
          Map<String, String> map = this.folderIndexProcesor.getIndex(siteId, meta.path());
          String documentId = map.get("documentId");

          DocumentItem item = this.docService.findDocument(siteId, documentId);

          DynamicDocumentItem result = new DocumentItemToDynamicDocumentItem().apply(item);
          results = new PaginationResults<>(Arrays.asList(result), null);

        } catch (IOException e) {
          results = new PaginationResults<>(Collections.emptyList(), null);
        }

      } else {
        updateFolderMetaData(meta);
        results = searchByMeta(siteId, query, meta, token, maxresults);
      }

    } else {
      SearchTagCriteria search = query.tag();
      results = searchByTag(siteId, query, search, token, maxresults);
    }

    return results;
  }

  /**
   * Perform Meta Data search.
   * 
   * @param siteId {@link String}
   * @param query {@link SearchQuery}
   * @param meta {@link SearchMetaCriteria}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> searchByMeta(final String siteId,
      final SearchQuery query, final SearchMetaCriteria meta, final PaginationMapToken token,
      final int maxresults) {

    String value = getMetaDataKey(siteId, meta);

    PaginationResults<DynamicDocumentItem> result = null;

    if (value != null) {

      Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
      values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, value)).build());

      String expression = PK + " = :pk";
      QueryRequest q =
          createQueryRequest(null, expression, values, token, maxresults, Boolean.TRUE);

      result = searchForMetaDocuments(q, siteId, query);

    } else {
      result = new PaginationResults<>(Collections.emptyList(), null);
    }

    return result;
  }

  private PaginationResults<DynamicDocumentItem> searchByTag(final String siteId,
      final SearchQuery query, final SearchTagCriteria csearch, final PaginationMapToken token,
      final int maxresults) {

    SearchTagCriteria search = csearch;

    if (this.tagSchemaPlugin != null) {
      search = this.tagSchemaPlugin.createMultiTagSearch(query);
    }

    String key = getSearchKey(search);

    PaginationResults<DynamicDocumentItem> result = null;

    Collection<String> documentIds = query.documentIds();

    if (!Objects.notNull(documentIds).isEmpty()) {

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

      if (!Objects.notNull(search.eqOr()).isEmpty()) {
        result = findDocumentsWithTagAndValues(siteId, query, key, search.eqOr(), maxresults);
      } else if (search.eq() != null) {
        result = findDocumentsWithTagAndValue(siteId, query, key, search.eq(), token, maxresults);
      } else if (search.beginsWith() != null) {
        result =
            findDocumentsTagStartWith(siteId, query, key, search.beginsWith(), token, maxresults);
      } else {
        result = findDocumentsWithTag(siteId, query, key, null, token, maxresults);
      }
    }

    return result;
  }

  /**
   * Search for Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @return {@link PaginationResults} {@link DocumentItemSearchResult}
   */
  private PaginationResults<DynamicDocumentItem> searchForDocuments(final QueryRequest q,
      final String siteId, final SearchQuery query) {

    QueryResponse result = this.dbClient.query(q);

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

    List<String> documentIds = new ArrayList<>(tags.keySet());

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

    return new PaginationResults<>(results, new QueryResponseToPagination().apply(result));
  }

  /**
   * Search for Meta Data Documents.
   *
   * @param q {@link QueryRequest}
   * @param siteId DynamoDB PK siteId
   * @param query {@link SearchQuery}
   * @return {@link PaginationResults} {@link DocumentItemSearchResult}
   */
  private PaginationResults<DynamicDocumentItem> searchForMetaDocuments(final QueryRequest q,
      final String siteId, final SearchQuery query) {

    QueryResponse result = this.dbClient.query(q);

    List<String> documentIds = result.items().stream().filter(r -> r.containsKey("documentId"))
        .map(r -> r.get("documentId").s()).distinct().collect(Collectors.toList());

    List<DocumentItem> list = this.docService.findDocuments(siteId, documentIds);

    Map<String, DocumentItem> documentMap = list != null
        ? list.stream().collect(Collectors.toMap(DocumentItem::getDocumentId, Function.identity()))
        : Collections.emptyMap();

    AttributeValueToGlobalMetaFolder metaFolder = new AttributeValueToGlobalMetaFolder();
    DocumentItemToDynamicDocumentItem transform = new DocumentItemToDynamicDocumentItem();

    List<DynamicDocumentItem> results = result.items().stream().map(r -> {

      AttributeValue documentId = r.get("documentId");
      boolean isDocument = documentId != null && documentMap.containsKey(documentId.s());

      return isDocument ? transform.apply(documentMap.get(r.get("documentId").s()))
          : new DynamicDocumentItem(metaFolder.apply(r));

    }).collect(Collectors.toList());

    return new PaginationResults<>(results, new QueryResponseToPagination().apply(result));
  }

  private void updateFolderMetaData(final SearchMetaCriteria meta) {

    String folder = meta.folder();

    if (folder != null) {

      if (folder.endsWith("/")) {
        folder = folder.substring(0, folder.length() - 1);
      }

      meta.indexType("folder");
      meta.eq(folder);
      meta.folder(null);

    }
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
}
