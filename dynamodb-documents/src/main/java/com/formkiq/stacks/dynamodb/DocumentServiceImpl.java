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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.FolderIndexProcessor.INDEX_FILE_SK;
import static software.amazon.awssdk.utils.StringUtils.isEmpty;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResult;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.ReadRequestBuilder;
import com.formkiq.aws.dynamodb.WriteRequestBuilder;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Objects;
import com.formkiq.aws.dynamodb.objects.Strings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/** Implementation of the {@link DocumentService}. */
public class DocumentServiceImpl implements DocumentService, DbKeys {

  /** Maximum number of Records DynamoDb can be queries for at a time. */
  private static final int MAX_QUERY_RECORDS = 100;
  /** {@link DynamoDbClient}. */
  private DynamoDbClient dbClient;
  /** {@link DynamoDbService}. */
  private DynamoDbService dbService;
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Documents Table Name. */
  private String documentTableName;
  /** {@link FolderIndexProcessor}. */
  private FolderIndexProcessor folderIndexProcessor;
  /** {@link GlobalIndexService}. */
  private GlobalIndexService indexWriter;
  /** Last Short Date. */
  private String lastShortDate = null;
  /** {@link DocumentVersionService}. */
  private DocumentVersionService versionsService;
  /** {@link SimpleDateFormat} YYYY-mm-dd format. */
  private SimpleDateFormat yyyymmddFormat;
  /** {@link DateTimeFormatter}. */
  private DateTimeFormatter yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   * @param documentVersionsService {@link DocumentVersionService}
   */
  public DocumentServiceImpl(final DynamoDbConnectionBuilder connection,
      final String documentsTable, final DocumentVersionService documentVersionsService) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("'documentsTable' is null");
    }

    this.indexWriter = new GlobalIndexService(connection, documentsTable);
    this.versionsService = documentVersionsService;
    this.dbClient = connection.build();
    this.documentTableName = documentsTable;
    this.folderIndexProcessor = new FolderIndexProcessorImpl(connection, documentsTable);
    this.dbService = new DynamoDbServiceImpl(connection, documentsTable);

    this.yyyymmddFormat = new SimpleDateFormat("yyyy-MM-dd");

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.yyyymmddFormat.setTimeZone(tz);
  }

  @Override
  public void addFolderIndex(final String siteId, final DocumentItem item) throws IOException {

    List<Map<String, AttributeValue>> folderIndex =
        this.folderIndexProcessor.generateIndex(siteId, item);

    folderIndex = updateFromExistingFolder(folderIndex);

    WriteRequestBuilder writeBuilder =
        new WriteRequestBuilder().appends(this.documentTableName, folderIndex);

    writeBuilder.batchWriteItem(this.dbClient);
  }

  private void addMetadata(final DocumentItem document,
      final Map<String, AttributeValue> pkvalues) {
    notNull(document.getMetadata()).forEach(m -> {
      if (m.getValues() != null) {
        addL(pkvalues, PREFIX_DOCUMENT_METADATA + m.getKey(), m.getValues());
      } else {
        addS(pkvalues, PREFIX_DOCUMENT_METADATA + m.getKey(), m.getValue());
      }
    });
  }

  @Override
  public void addTags(final String siteId, final String documentId,
      final Collection<DocumentTag> tags, final String timeToLive) {

    List<Map<String, AttributeValue>> items =
        getSaveTagsAttributes(siteId, documentId, tags, timeToLive);

    if (!items.isEmpty()) {
      WriteRequestBuilder builder =
          new WriteRequestBuilder().appends(this.documentTableName, items);
      BatchWriteItemRequest batch =
          BatchWriteItemRequest.builder().requestItems(builder.getItems()).build();
      this.dbClient.batchWriteItem(batch);

      List<String> tagKeys = tags.stream().map(t -> t.getKey()).collect(Collectors.toList());

      this.indexWriter.writeTagIndex(siteId, tagKeys);
    }
  }

  /**
   * Build DynamoDB Search Map.
   * 
   * @param siteId {@link String}
   * @param pk {@link String}
   * @param skMin {@link String}
   * @param skMax {@link String}
   * @return {@link Map}
   */
  private Map<String, String> createSearchMap(final String siteId, final String pk,
      final String skMin, final String skMax) {
    Map<String, String> map = new HashMap<>();
    map.put("pk", createDatabaseKey(siteId, pk));
    map.put("skMin", skMin);
    map.put("skMax", skMax);

    return map;
  }

  /**
   * Delete Record.
   * 
   * @param key {@link Map}
   * @return {@link DeleteItemResponse}
   */
  private DeleteItemResponse delete(final Map<String, AttributeValue> key) {
    DeleteItemRequest deleteItemRequest =
        DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

    return this.dbClient.deleteItem(deleteItemRequest);
  }

  @Override
  public boolean deleteDocument(final String siteId, final String documentId) {

    Map<String, AttributeValue> startkey = null;

    this.versionsService.deleteAllVersionIds(this.dbClient, siteId, documentId);

    DocumentItem item = findDocument(siteId, documentId);

    deleteFolderIndex(siteId, item);

    do {
      Map<String, AttributeValue> values =
          queryKeys(keysGeneric(siteId, PREFIX_DOCS + documentId, null));

      QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
          .keyConditionExpression(PK + " = :pk").expressionAttributeValues(values)
          .limit(Integer.valueOf(MAX_RESULTS)).build();

      QueryResponse response = this.dbClient.query(q);
      List<Map<String, AttributeValue>> results = response.items();

      for (Map<String, AttributeValue> map : results) {
        deleteItem(Map.of("PK", map.get("PK"), "SK", map.get("SK")));
      }

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    return deleteItem(keysDocument(siteId, documentId, Optional.empty()));
  }

  @Override
  public void deleteDocumentFormat(final String siteId, final String documentId,
      final String contentType) {
    delete(keysDocumentFormats(siteId, documentId, contentType));
  }

  @Override
  public void deleteDocumentFormats(final String siteId, final String documentId) {

    PaginationMapToken startkey = null;

    do {
      PaginationResults<DocumentFormat> pr =
          findDocumentFormats(siteId, documentId, startkey, MAX_RESULTS);

      for (DocumentFormat format : pr.getResults()) {
        deleteDocumentFormat(siteId, documentId, format.getContentType());
      }

      startkey = pr.getToken();

    } while (startkey != null);
  }

  @Override
  public void deleteDocumentTag(final String siteId, final String documentId, final String tagKey) {
    deleteItem(keysDocumentTag(siteId, documentId, tagKey));
  }

  @Override
  public void deleteDocumentTags(final String siteId, final String documentId) {

    PaginationMapToken startkey = null;

    do {
      PaginationResults<DocumentTag> pr =
          findDocumentTags(siteId, documentId, startkey, MAX_RESULTS);

      for (DocumentTag tag : pr.getResults()) {
        deleteDocumentTag(siteId, documentId, tag.getKey());
      }

      startkey = pr.getToken();

    } while (startkey != null);
  }

  /**
   * Delete Folder Index.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   */
  private void deleteFolderIndex(final String siteId, final DocumentItem item) {
    if (item != null) {

      try {
        Map<String, String> attr = this.folderIndexProcessor.getIndex(siteId, item.getPath());

        if (attr.containsKey("documentId")) {
          deleteItem(Map.of(PK, AttributeValue.builder().s(attr.get(PK)).build(), SK,
              AttributeValue.builder().s(attr.get(SK)).build()));
        }
      } catch (IOException e) {
        // ignore folder doesn't exist
      }
    }
  }

  /**
   * Delete Document Row by Parition / Sort Key. param dbClient {@link DynamoDbClient}
   * 
   * @param key DocumentDb Key {@link Map}
   * @return boolean
   */
  private boolean deleteItem(final Map<String, AttributeValue> key) {

    DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
        .tableName(this.documentTableName).key(key).returnValues(ReturnValue.ALL_OLD).build();

    DeleteItemResponse response = this.dbClient.deleteItem(deleteItemRequest);
    return !response.attributes().isEmpty();
  }

  @Override
  public void deletePreset(final String siteId, final String id) {
    deletePresetTags(siteId, id);
    delete(keysPreset(siteId, id));
  }

  @Override
  public void deletePresets(final String siteId, final String type) {
    PaginationMapToken startkey = null;

    do {
      PaginationResults<Preset> pr = findPresets(siteId, null, type, null, startkey, MAX_RESULTS);

      for (Preset p : pr.getResults()) {
        deletePreset(siteId, p.getId());
      }

      startkey = pr.getToken();

    } while (startkey != null);

  }

  @Override
  public void deletePresetTag(final String siteId, final String id, final String tag) {
    delete(keysPresetTag(siteId, id, tag));
  }

  @Override
  public void deletePresetTags(final String siteId, final String id) {
    PaginationMapToken startkey = null;

    do {
      PaginationResults<PresetTag> pr = findPresetTags(siteId, id, startkey, MAX_RESULTS);

      for (PresetTag tag : pr.getResults()) {
        deletePresetTag(siteId, id, tag.getKey());
      }

      startkey = pr.getToken();

    } while (startkey != null);

  }

  @Override
  public boolean exists(final String siteId, final String documentId) {
    Map<String, AttributeValue> keys = keysDocument(siteId, documentId);
    return this.dbService.exists(keys.get(PK), keys.get(SK));
  }

  /**
   * Get Record.
   * 
   * @param pk {@link String}
   * @param sk {@link String}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private Optional<Map<String, AttributeValue>> find(final String pk, final String sk) {

    Map<String, AttributeValue> keyMap = keysGeneric(pk, sk);
    GetItemRequest r =
        GetItemRequest.builder().key(keyMap).tableName(this.documentTableName).build();

    Map<String, AttributeValue> result = this.dbClient.getItem(r).item();
    return result != null && !result.isEmpty() ? Optional.of(result) : Optional.empty();
  }

  /**
   * Get Records.
   * 
   * @param pk {@link String}
   * @param sk {@link String}
   * @param indexName {@link String}
   * @param token {@link PaginationMapToken}
   * @param scanIndexForward {@link Boolean}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentFormat}
   */
  private PaginationResults<Map<String, AttributeValue>> find(final String pk, final String sk,
      final String indexName, final PaginationMapToken token, final Boolean scanIndexForward,
      final int maxresults) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);
    Map<String, AttributeValue> values = queryKeys(keysGeneric(pk, sk));

    String indexPrefix = indexName != null ? indexName : "";
    String expression = values.containsKey(":sk")
        ? indexPrefix + PK + " = :pk and begins_with(" + indexPrefix + SK + ", :sk)"
        : indexPrefix + PK + " = :pk";

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(indexName)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .scanIndexForward(scanIndexForward).limit(Integer.valueOf(maxresults))
        .exclusiveStartKey(startkey).build();

    QueryResponse result = this.dbClient.query(q);
    return new PaginationResults<>(result.items(), new QueryResponseToPagination().apply(result));
  }

  /**
   * Get Record and transform to object.
   * 
   * @param <T> Type of object
   * @param keys {@link Map} {@link AttributeValue}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param func {@link Function}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private <T> PaginationResults<T> findAndTransform(final Map<String, AttributeValue> keys,
      final PaginationMapToken token, final int maxresults,
      final Function<Map<String, AttributeValue>, T> func) {
    return findAndTransform(PK, SK, keys, token, maxresults, func);
  }

  /**
   * Get Record and transform to object.
   * 
   * @param <T> Type of object
   * @param pkKey {@link String}
   * @param skKey {@link String}
   * @param keys {@link Map} {@link AttributeValue}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @param func {@link Function}
   * @return {@link Optional} {@link Map} {@link AttributeValue}
   */
  private <T> PaginationResults<T> findAndTransform(final String pkKey, final String skKey,
      final Map<String, AttributeValue> keys, final PaginationMapToken token, final int maxresults,
      final Function<Map<String, AttributeValue>, T> func) {
    String pk = keys.get(pkKey).s();
    String sk = keys.containsKey(skKey) ? keys.get(skKey).s() : null;
    String indexName = getIndexName(pkKey);

    PaginationResults<Map<String, AttributeValue>> results =
        find(pk, sk, indexName, token, null, maxresults);

    List<T> list =
        results.getResults().stream().map(s -> func.apply(s)).collect(Collectors.toList());

    return new PaginationResults<T>(list, results.getToken());
  }

  @Override
  public DocumentItem findDocument(final String siteId, final String documentId) {
    return findDocument(siteId, documentId, false, null, 0).getResult();
  }

  @Override
  public PaginationResult<DocumentItem> findDocument(final String siteId, final String documentId,
      final boolean includeChildDocuments, final PaginationMapToken token, final int limit) {

    DocumentItem item = null;
    PaginationMapToken pagination = null;

    GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, documentId))
        .tableName(this.documentTableName).consistentRead(Boolean.TRUE).build();

    Map<String, AttributeValue> result = this.dbClient.getItem(r).item();

    if (result != null && !result.isEmpty()) {

      item = new AttributeValueToDocumentItem().apply(result);

      if (includeChildDocuments) {

        Map<String, AttributeValue> values =
            queryKeys(keysDocument(siteId, documentId, Optional.of("")));

        Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

        QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
            .keyConditionExpression(PK + " = :pk and begins_with(" + SK + ",:sk)")
            .expressionAttributeValues(values).exclusiveStartKey(startkey)
            .limit(Integer.valueOf(limit)).build();

        QueryResponse response = this.dbClient.query(q);
        List<Map<String, AttributeValue>> results = response.items();
        List<String> ids =
            results.stream().map(s -> s.get("documentId").s()).collect(Collectors.toList());

        List<DocumentItem> childDocs = findDocuments(siteId, ids);
        item.setDocuments(childDocs);

        pagination = new QueryResponseToPagination().apply(response);
      }
    }

    return new PaginationResult<>(item, pagination);
  }

  @Override
  public Optional<DocumentFormat> findDocumentFormat(final String siteId, final String documentId,
      final String contentType) {

    Map<String, AttributeValue> keyMap = keysDocumentFormats(siteId, documentId, contentType);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToDocumentFormat format = new AttributeValueToDocumentFormat();
    return result.isPresent() ? Optional.of(format.apply(result.get())) : Optional.empty();
  }

  @Override
  public PaginationResults<DocumentFormat> findDocumentFormats(final String siteId,
      final String documentId, final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysDocumentFormats(siteId, documentId, null);
    return findAndTransform(keys, token, maxresults, new AttributeValueToDocumentFormat());
  }

  @Override
  public List<DocumentItem> findDocuments(final String siteId, final List<String> ids) {

    List<DocumentItem> results = null;

    if (!ids.isEmpty()) {

      List<Map<String, AttributeValue>> keys = ids.stream()
          .map(documentId -> keysDocument(siteId, documentId)).collect(Collectors.toList());

      Collection<List<Map<String, AttributeValue>>> values = getBatch(keys).values();
      List<Map<String, AttributeValue>> result =
          !values.isEmpty() ? values.iterator().next() : Collections.emptyList();

      AttributeValueToDocumentItem toDocumentItem = new AttributeValueToDocumentItem();
      List<DocumentItem> items = result.stream().map(a -> toDocumentItem.apply(Arrays.asList(a)))
          .collect(Collectors.toList());
      items = sortByIds(ids, items);

      results = !items.isEmpty() ? items : null;
    }

    return results;
  }

  @Override
  public PaginationResults<DocumentItem> findDocumentsByDate(final String siteId,
      final ZonedDateTime date, final PaginationMapToken token, final int maxresults) {

    List<Map<String, String>> searchMap = generateSearchCriteria(siteId, date, token);

    PaginationResults<DocumentItem> results =
        findDocumentsBySearchMap(siteId, searchMap, token, maxresults);

    // if number of results == maxresult, check to see if next page has at least 1 record.
    if (results.getResults().size() == maxresults) {
      PaginationMapToken nextToken = results.getToken();
      searchMap = generateSearchCriteria(siteId, date, nextToken);
      PaginationResults<DocumentItem> next =
          findDocumentsBySearchMap(siteId, searchMap, nextToken, 1);

      if (next.getResults().isEmpty()) {
        results = new PaginationResults<DocumentItem>(results.getResults(), null);
      }
    }

    return results;
  }

  /**
   * Find Documents using the Search Map.
   * 
   * @param siteId DynamoDB PK siteId
   * @param searchMap {@link List}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DocumentItem> findDocumentsBySearchMap(final String siteId,
      final List<Map<String, String>> searchMap, final PaginationMapToken token,
      final int maxresults) {

    int max = maxresults;
    PaginationMapToken itemsToken = null;
    PaginationMapToken qtoken = token;
    List<DocumentItem> items = new ArrayList<>();

    for (Map<String, String> map : searchMap) {
      String pk = map.get("pk");
      String skMin = map.get("skMin");
      String skMax = map.get("skMax");
      PaginationResults<DocumentItem> results =
          queryDocuments(siteId, pk, skMin, skMax, qtoken, max);

      items.addAll(results.getResults());
      itemsToken = results.getToken();
      max = max - results.getResults().size();

      if (max < 1) {
        break;
      }

      qtoken = null;
    }

    return new PaginationResults<DocumentItem>(items, itemsToken);
  }

  @Override
  public Map<String, Collection<DocumentTag>> findDocumentsTags(final String siteId,
      final Collection<String> documentIds, final List<String> tags) {

    final Map<String, Collection<DocumentTag>> tagMap = new HashMap<>();
    List<Map<String, AttributeValue>> keys = new ArrayList<>();

    documentIds.forEach(id -> {
      tagMap.put(id, new ArrayList<>());
      tags.forEach(tag -> {
        Map<String, AttributeValue> key = keysDocumentTag(siteId, id, tag);
        keys.add(key);
      });
    });

    List<List<Map<String, AttributeValue>>> paritions = Objects.parition(keys, MAX_QUERY_RECORDS);

    for (List<Map<String, AttributeValue>> partition : paritions) {

      Map<String, KeysAndAttributes> requestedItems =
          Map.of(this.documentTableName, KeysAndAttributes.builder().keys(partition).build());

      BatchGetItemRequest batchReq =
          BatchGetItemRequest.builder().requestItems(requestedItems).build();
      BatchGetItemResponse batchResponse = this.dbClient.batchGetItem(batchReq);

      Collection<List<Map<String, AttributeValue>>> values = batchResponse.responses().values();
      List<Map<String, AttributeValue>> result =
          !values.isEmpty() ? values.iterator().next() : Collections.emptyList();

      AttributeValueToDocumentTag toDocumentTag = new AttributeValueToDocumentTag(siteId);
      List<DocumentTag> list =
          result.stream().map(a -> toDocumentTag.apply(a)).collect(Collectors.toList());

      for (DocumentTag tag : list) {
        tagMap.get(tag.getDocumentId()).add(tag);
      }
    }

    return tagMap;
  }

  @Override
  public DocumentTag findDocumentTag(final String siteId, final String documentId,
      final String tagKey) {

    DocumentTag item = null;
    QueryResponse response =
        findDocumentTagAttributes(siteId, documentId, tagKey, Integer.valueOf(1));
    List<Map<String, AttributeValue>> items = response.items();

    if (!items.isEmpty()) {
      item = new AttributeValueToDocumentTag(siteId).apply(items.get(0));
    }

    return item;
  }

  /**
   * Find Document Tag {@link AttributeValue}.
   * 
   * @param siteId DynamoDB PK siteId
   * @param documentId {@link String}
   * @param tagKey {@link String}
   * @param maxresults {@link Integer}
   * 
   * @return {@link QueryResponse}
   */
  private QueryResponse findDocumentTagAttributes(final String siteId, final String documentId,
      final String tagKey, final Integer maxresults) {

    Map<String, AttributeValue> values = queryKeys(keysDocumentTag(siteId, documentId, tagKey));

    Builder req = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(PK + " = :pk and begins_with(" + SK + ", :sk)")
        .expressionAttributeValues(values);

    if (maxresults != null) {
      req = req.limit(maxresults);
    }

    QueryRequest q = req.build();
    QueryResponse result = this.dbClient.query(q);
    return result;
  }

  @Override
  public Collection<DocumentTag> findDocumentTags(final String siteId, final String documentId,
      final Collection<String> tagKeys) {

    Collection<DocumentTag> tags = new ArrayList<>();
    tagKeys.forEach(tagKey -> {
      DocumentTag tag = findDocumentTag(siteId, documentId, tagKey);
      if (tag != null) {
        tags.add(tag);
      }
    });

    return tags;
  }

  @Override
  public PaginationResults<DocumentTag> findDocumentTags(final String siteId,
      final String documentId, final PaginationMapToken token, final int maxresults) {

    Map<String, AttributeValue> keys = keysDocumentTag(siteId, documentId, null);

    PaginationResults<DocumentTag> tags =
        findAndTransform(keys, token, maxresults, new AttributeValueToDocumentTag(siteId));

    // filter duplicates
    DocumentTag prev = null;
    for (Iterator<DocumentTag> itr = tags.getResults().iterator(); itr.hasNext();) {
      DocumentTag t = itr.next();
      if (prev != null && prev.getKey().equals(t.getKey())) {
        itr.remove();
      } else {
        prev = t;
      }
    }

    return tags;
  }

  @Override
  public ZonedDateTime findMostDocumentDate() {
    ZonedDateTime date = null;
    PaginationResults<Map<String, AttributeValue>> result =
        find(PREFIX_DOCUMENT_DATE, null, null, null, Boolean.FALSE, 1);

    if (!result.getResults().isEmpty()) {
      String dateString = result.getResults().get(0).get(SK).s();
      date = DateUtil.toDateTimeFromString(dateString, null);
    }

    return date;
  }

  @Override
  public Optional<Preset> findPreset(final String siteId, final String id) {
    Map<String, AttributeValue> keyMap = keysPreset(siteId, id);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToPreset format = new AttributeValueToPreset();
    return result.isPresent() ? Optional.of(format.apply(result.get())) : Optional.empty();
  }

  @Override
  public PaginationResults<Preset> findPresets(final String siteId, final String id,
      final String type, final String name, final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysPresetGsi2(siteId, id, type, name);
    return findAndTransform(GSI2_PK, GSI2_SK, keys, token, maxresults,
        new AttributeValueToPreset());
  }

  @Override
  public Optional<PresetTag> findPresetTag(final String siteId, final String id,
      final String tagKey) {
    Map<String, AttributeValue> keyMap = keysPresetTag(siteId, id, tagKey);
    Optional<Map<String, AttributeValue>> result = find(keyMap.get("PK").s(), keyMap.get("SK").s());

    AttributeValueToPresetTag format = new AttributeValueToPresetTag();
    return result.isPresent() ? Optional.of(format.apply(result.get())) : Optional.empty();
  }

  @Override
  public PaginationResults<PresetTag> findPresetTags(final String siteId, final String id,
      final PaginationMapToken token, final int maxresults) {
    Map<String, AttributeValue> keys = keysPresetTag(siteId, id, null);
    return findAndTransform(keys, token, maxresults, new AttributeValueToPresetTag());
  }

  /**
   * Generate DynamoDB PK(s)/SK(s) to search.
   * 
   * @param siteId DynamoDB PK siteId
   * @param date {@link ZonedDateTime}
   * @param token {@link PaginationMapToken}
   * @return {@link List} {@link String}
   */
  private List<Map<String, String>> generateSearchCriteria(final String siteId,
      final ZonedDateTime date, final PaginationMapToken token) {

    List<Map<String, String>> list = new ArrayList<>();

    LocalDateTime startDate = LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    LocalDateTime endDate = startDate.plusDays(1);

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    String pk1 = PREFIX_DOCUMENT_DATE_TS + startDate.format(this.yyyymmddFormatter);
    String pk2 = PREFIX_DOCUMENT_DATE_TS + endDate.format(this.yyyymmddFormatter);
    boolean nextDayPagination = isNextDayPagination(siteId, pk1, startkey);

    if (!nextDayPagination) {
      String skMin = startkey != null ? startkey.get(GSI1_SK).s()
          : this.df.format(Date.from(startDate.toInstant(ZoneOffset.UTC)));
      Map<String, String> map = createSearchMap(siteId, pk1, skMin, null);
      list.add(map);
    }

    if (!pk1.equals(pk2)) {
      String skMin =
          this.df.format(Date.from(endDate.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)));
      String skMax = this.df.format(Date.from(endDate.toInstant(ZoneOffset.UTC)));

      if (startkey != null && nextDayPagination) {
        Map<String, String> map = createSearchMap(siteId, pk2, startkey.get(GSI1_SK).s(), skMax);
        list.add(map);
      } else if (!skMin.equals(skMax)) {
        Map<String, String> map = createSearchMap(siteId, pk2, skMin, skMax);
        list.add(map);
      }
    }

    list.forEach(m -> m.put("pk", resetDatabaseKey(siteId, m.get("pk"))));

    return list;
  }

  /**
   * Get Batch Keys.
   * 
   * @param keys {@link List} {@link Map} {@link AttributeValue}
   * @return {@link Map}
   */
  private Map<String, List<Map<String, AttributeValue>>> getBatch(
      final Collection<Map<String, AttributeValue>> keys) {
    ReadRequestBuilder builder = new ReadRequestBuilder();
    builder.append(this.documentTableName, keys);
    return builder.batchReadItems(this.dbClient);
  }

  /**
   * Get {@link Date} or Now.
   * 
   * @param date {@link Date}
   * @param defaultDate {@link Date}
   * @return {@link Date}
   */
  private Date getDateOrNow(final Date date, final Date defaultDate) {
    return date != null ? date : defaultDate;
  }

  private Date getPreviousInsertedDate(final Map<String, AttributeValue> previous) {

    Date date = null;
    AttributeValue insertedDate = previous.get("inserteddate");
    if (insertedDate != null) {
      try {
        date = this.df.parse(insertedDate.s());
      } catch (ParseException e) {
        // ignore
      }
    }

    return date;
  }

  /**
   * Save {@link DocumentItemDynamoDb}.
   * 
   * @param keys {@link Map}
   * @param siteId DynamoDB PK siteId
   * @param document {@link DocumentItem}
   * @param previous {@link Map}
   * @param options {@link SaveDocumentOptions}
   * @param documentExists boolean
   * @return {@link Map}
   */
  private Map<String, AttributeValue> getSaveDocumentAttributes(
      final Map<String, AttributeValue> keys, final String siteId, final DocumentItem document,
      final Map<String, AttributeValue> previous, final SaveDocumentOptions options,
      final boolean documentExists) {

    Date previousInsertedDate = getPreviousInsertedDate(previous);

    Date insertedDate = getDateOrNow(document.getInsertedDate(), new Date());
    if (previousInsertedDate != null) {
      insertedDate = previousInsertedDate;
    }

    document.setInsertedDate(insertedDate);

    Date lastModifiedDate = getDateOrNow(document.getLastModifiedDate(), insertedDate);
    if (documentExists) {
      lastModifiedDate = new Date();
    }
    document.setLastModifiedDate(lastModifiedDate);

    String shortdate = this.yyyymmddFormat.format(insertedDate);
    String fullInsertedDate = this.df.format(insertedDate);
    String fullLastModifiedDate = this.df.format(lastModifiedDate);

    Map<String, AttributeValue> pkvalues = new HashMap<>(keys);

    if (options.saveDocumentDate()) {
      addS(pkvalues, GSI1_PK, createDatabaseKey(siteId, PREFIX_DOCUMENT_DATE_TS + shortdate));
      addS(pkvalues, GSI1_SK, fullInsertedDate + TAG_DELIMINATOR + document.getDocumentId());
    }

    addS(pkvalues, "documentId", document.getDocumentId());

    if (fullInsertedDate != null) {
      addS(pkvalues, "inserteddate", fullInsertedDate);
      addS(pkvalues, "lastModifiedDate", fullLastModifiedDate);
    }

    addS(pkvalues, "tagSchemaId", document.getTagSchemaId());
    addS(pkvalues, "userId", document.getUserId());
    addS(pkvalues, "path",
        isEmpty(document.getPath()) ? document.getDocumentId() : document.getPath());
    addS(pkvalues, "version", document.getVersion());
    addS(pkvalues, DocumentVersionService.S3VERSION_ATTRIBUTE, document.getS3version());
    addS(pkvalues, "contentType", document.getContentType());


    if (document.getContentLength() != null) {
      addN(pkvalues, "contentLength", "" + document.getContentLength());
    }

    if (document.getChecksum() != null) {
      String etag = document.getChecksum().replaceAll("^\"|\"$", "");
      addS(pkvalues, "checksum", etag);
    }

    addS(pkvalues, "belongsToDocumentId", document.getBelongsToDocumentId());

    addN(pkvalues, "TimeToLive", options.timeToLive());

    addMetadata(document, pkvalues);

    return pkvalues;
  }

  /**
   * Generate Save Tags DynamoDb Keys.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param timeToLive {@link String}
   * @return {@link List} {@link Map}
   */
  private List<Map<String, AttributeValue>> getSaveTagsAttributes(final String siteId,
      final String documentId, final Collection<DocumentTag> tags, final String timeToLive) {

    Predicate<DocumentTag> predicate = tag -> DocumentTagType.SYSTEMDEFINED.equals(tag.getType())
        || !SYSTEM_DEFINED_TAGS.contains(tag.getKey());

    DocumentTagToAttributeValueMap mapper =
        new DocumentTagToAttributeValueMap(this.df, PREFIX_DOCS, siteId, documentId);

    List<Map<String, AttributeValue>> items = notNull(tags).stream().filter(predicate).map(mapper)
        .flatMap(List::stream).collect(Collectors.toList());

    if (timeToLive != null) {
      items.forEach(v -> addN(v, "TimeToLive", timeToLive));
    }

    return items;
  }

  /**
   * Is {@link List} {@link DynamicObject} contain a non generated tag.
   * 
   * @param tags {@link List} {@link DynamicObject}
   * @return boolean
   */
  private boolean isDocumentUserTagged(final List<DynamicObject> tags) {
    return tags != null ? tags.stream().filter(t -> {
      String key = t.getString("key");
      return key != null && !SYSTEM_DEFINED_TAGS.contains(key);
    }).count() > 0 : false;
  }

  @Override
  public boolean isFolderExists(final String siteId, final DocumentItem item) {

    boolean exists = false;
    try {
      Map<String, String> map = this.folderIndexProcessor.getIndex(siteId, item.getPath());

      if ("folder".equals(map.get("type"))) {
        GetItemResponse response =
            this.dbClient.getItem(GetItemRequest.builder().tableName(this.documentTableName)
                .key(Map.of(PK, AttributeValue.builder().s(map.get(PK)).build(), SK,
                    AttributeValue.builder().s(map.get(SK)).build()))
                .build());
        exists = !response.item().isEmpty();
      }
    } catch (IOException e) {
      exists = false;
    }

    return exists;
  }

  /**
   * Checks the {@link Map} of {@link AttributeValue} if the PK in the map matches DateKey. If they
   * do NOT match and map is NOT null. Then we are pagination on the NEXT Day.
   * 
   * @param siteId DynamoDB PK siteId
   * @param dateKey (yyyy-MM-dd) format
   * @param map {@link Map}
   * @return boolean
   */
  private boolean isNextDayPagination(final String siteId, final String dateKey,
      final Map<String, AttributeValue> map) {
    return map != null && !dateKey.equals(resetDatabaseKey(siteId, map.get(GSI1_PK).s()));
  }

  /**
   * Is Document Path Changed.
   * 
   * @param previous {@link Map}
   * @param current {@link Map}
   * @return boolean
   */
  private boolean isPathChanges(final Map<String, AttributeValue> previous,
      final Map<String, AttributeValue> current) {
    String path0 = previous.containsKey("path") ? previous.get("path").s() : "";
    String path1 = current.containsKey("path") ? current.get("path").s() : "";
    return !path1.equals(path0) && !"".equals(path0);
  }

  /**
   * Query Documents by Primary Key.
   * 
   * @param siteId DynamoDB PK siteId
   * @param pk {@link String}
   * @param skMin {@link String}
   * @param skMax {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DocumentItem> queryDocuments(final String siteId, final String pk,
      final String skMin, final String skMax, final PaginationMapToken token,
      final int maxresults) {

    String expr = GSI1_PK + " = :pk";
    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, pk)).build());

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    if (skMax != null) {
      values.put(":sk1", AttributeValue.builder().s(skMin).build());
      values.put(":sk2", AttributeValue.builder().s(skMax).build());
      expr += " and " + GSI1_SK + " between :sk1 and :sk2";
    } else if (skMin != null) {
      values.put(":sk", AttributeValue.builder().s(skMin).build());
      expr += " and " + GSI1_SK + " >= :sk";
    }

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(GSI1)
        .keyConditionExpression(expr).expressionAttributeValues(values)
        .limit(Integer.valueOf(maxresults)).exclusiveStartKey(startkey).build();

    QueryResponse result = this.dbClient.query(q);

    List<DocumentItem> list = result.items().stream().map(s -> {
      String documentId = s.get("documentId").s();
      return new DocumentItemDynamoDb(documentId, null, null);
    }).collect(Collectors.toList());

    if (!list.isEmpty()) {
      List<String> documentIds =
          list.stream().map(s -> s.getDocumentId()).collect(Collectors.toList());

      list = findDocuments(siteId, documentIds);
    }

    return new PaginationResults<>(list, new QueryResponseToPagination().apply(result));
  }

  /**
   * Remove Null Metadata.
   * 
   * @param document {@link DocumentItem}
   * @param documentValues {@link Map}
   */
  private void removeNullMetadata(final DocumentItem document,
      final Map<String, AttributeValue> documentValues) {
    notNull(document.getMetadata()).stream()
        .filter(m -> m.getValues() == null && isEmpty(m.getValue())).collect(Collectors.toList())
        .forEach(m -> documentValues.remove(PREFIX_DOCUMENT_METADATA + m.getKey()));
  }

  @Override
  public boolean removeTag(final String siteId, final String documentId, final String tagKey,
      final String tagValue) {

    QueryResponse response = findDocumentTagAttributes(siteId, documentId, tagKey, null);
    List<Map<String, AttributeValue>> items = response.items();

    List<DeleteItemRequest> deletes = new ArrayList<>();
    List<PutItemRequest> puts = new ArrayList<>();

    items.forEach(i -> {

      String pk = i.get("PK").s();
      String sk = i.get("SK").s();
      String value = i.get("tagValue").s();
      Map<String, AttributeValue> key = keysGeneric(pk, sk);

      if (value.equals(tagValue)) {
        DeleteItemRequest deleteItemRequest =
            DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();
        deletes.add(deleteItemRequest);

      } else if (i.containsKey("tagValues")) {

        List<AttributeValue> avalues = i.get("tagValues").l();
        avalues =
            avalues.stream().filter(v -> !v.s().equals(tagValue)).collect(Collectors.toList());

        Map<String, AttributeValue> m = new HashMap<>(i);
        if (avalues.size() == 1) {
          m.remove("tagValues");
          m.put("tagValue", avalues.get(0));
        } else {
          m.put("tagValues", AttributeValue.builder().l(avalues).build());
        }
        puts.add(PutItemRequest.builder().tableName(this.documentTableName).item(m).build());
      }
    });

    deletes.forEach(i -> this.dbClient.deleteItem(i));
    puts.forEach(i -> this.dbClient.putItem(i));

    return !deletes.isEmpty();
  }

  @Override
  public void removeTags(final String siteId, final String documentId,
      final Collection<String> tags) {

    for (String tag : tags) {

      QueryResponse response = findDocumentTagAttributes(siteId, documentId, tag, null);

      List<Map<String, AttributeValue>> items = response.items();
      items =
          items.stream().filter(i -> tag.equals(i.get("tagKey").s())).collect(Collectors.toList());

      items.forEach(i -> {
        Map<String, AttributeValue> key = keysGeneric(i.get("PK").s(), i.get("SK").s());
        DeleteItemRequest deleteItemRequest =
            DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

        this.dbClient.deleteItem(deleteItemRequest);
      });
    }
  }

  /**
   * Rename a document document.
   * 
   * @param documentId {@link String}
   * @param documentValues {@link Map}
   * @param folderIndex {@link List}
   */
  private void renameDuplicateDocumentIfNeeded(final String documentId,
      final Map<String, AttributeValue> documentValues,
      final List<Map<String, AttributeValue>> folderIndex) {

    if (!folderIndex.isEmpty()) {
      int len = folderIndex.size();
      Map<String, AttributeValue> documentPath = folderIndex.get(len - 1);

      if (this.dbService.exists(documentPath.get(PK), documentPath.get(SK))) {

        String oldPath = documentValues.get("path").s();
        String oldFilename = documentPath.get("path").s();

        String extension = Strings.getExtension(oldFilename);
        String newFilename =
            oldFilename.replaceAll("\\." + extension, " (" + documentId + ")" + "." + extension);
        String newPath = oldPath.replaceAll(oldFilename, newFilename);

        documentValues.put("path", AttributeValue.fromS(newPath));
        documentPath.put(SK, AttributeValue.fromS(INDEX_FILE_SK + newFilename.toLowerCase()));
        documentPath.put("path", AttributeValue.fromS(newFilename));
      }
    }
  }

  /**
   * Save Record.
   * 
   * @param values {@link Map} {@link AttributeValue}
   * @return {@link Map} {@link AttributeValue}
   */
  private Map<String, AttributeValue> save(final Map<String, AttributeValue> values) {
    PutItemRequest put =
        PutItemRequest.builder().tableName(this.documentTableName).item(values).build();

    return this.dbClient.putItem(put).attributes();
  }

  /**
   * Save Child Documents.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param item {@link DocumentItem}
   * @param date {@link Date}
   */
  private void saveChildDocuments(final String siteId, final DynamicDocumentItem doc,
      final DocumentItem item, final Date date) {

    List<DocumentTag> tags;
    Map<String, AttributeValue> keys;
    List<DynamicObject> documents = doc.getList("documents");

    for (DynamicObject subdoc : documents) {

      if (subdoc.getDate("insertedDate") == null) {
        subdoc.put("insertedDate", date);
      }

      DocumentItem document = new DynamicDocumentItem(subdoc);
      document.setBelongsToDocumentId(item.getDocumentId());

      DocumentItem dockey = new DynamicDocumentItem(new HashMap<>());
      dockey.setDocumentId(subdoc.getString("documentId"));
      dockey.setBelongsToDocumentId(item.getDocumentId());

      // save child document
      keys =
          keysDocument(siteId, item.getDocumentId(), Optional.of(subdoc.getString("documentId")));

      SaveDocumentOptions childOptions =
          new SaveDocumentOptions().saveDocumentDate(false).timeToLive(doc.getString("TimeToLive"));
      saveDocument(keys, siteId, dockey, null, childOptions);

      List<DynamicObject> doctags = subdoc.getList("tags");
      tags = doctags.stream().map(t -> {
        DynamicObjectToDocumentTag transformer = new DynamicObjectToDocumentTag(this.df);
        return transformer.apply(t);
      }).collect(Collectors.toList());

      keys = keysDocument(siteId, subdoc.getString("documentId"));

      childOptions =
          new SaveDocumentOptions().saveDocumentDate(false).timeToLive(doc.getString("TimeToLive"));
      saveDocument(keys, siteId, document, tags, childOptions);
    }
  }

  /**
   * Save Document.
   * 
   * @param keys {@link Map}
   * @param siteId {@link String}
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param options {@link SaveDocumentOptions}
   */
  private void saveDocument(final Map<String, AttributeValue> keys, final String siteId,
      final DocumentItem document, final Collection<DocumentTag> tags,
      final SaveDocumentOptions options) {

    boolean documentExists = exists(siteId, document.getDocumentId());

    Map<String, AttributeValue> previous =
        documentExists ? new HashMap<>(this.dbService.get(keys.get(PK), keys.get(SK)))
            : Collections.emptyMap();

    Map<String, AttributeValue> documentValues = new HashMap<>(previous);
    Map<String, AttributeValue> current =
        getSaveDocumentAttributes(keys, siteId, document, previous, options, documentExists);
    documentValues.putAll(current);

    removeNullMetadata(document, documentValues);

    boolean previousSameAsCurrent = previous.equals(current);

    if (!previousSameAsCurrent) {
      this.versionsService.addDocumentVersionAttributes(previous, documentValues);
    }

    Collection<Map<String, AttributeValue>> previousList =
        !previous.isEmpty() ? Arrays.asList(previous) : Collections.emptyList();

    List<Map<String, AttributeValue>> folderIndex =
        this.folderIndexProcessor.generateIndex(siteId, document);
    folderIndex = updateFromExistingFolder(folderIndex);

    if (!documentExists) {
      renameDuplicateDocumentIfNeeded(document.getDocumentId(), documentValues, folderIndex);
    } else if (isPathChanges(previous, documentValues)) {
      this.folderIndexProcessor.deletePath(siteId, document.getDocumentId(),
          previous.get("path").s());
    }

    // update top level directory
    if (folderIndex.size() > 1) {
      int len = folderIndex.size();
      folderIndex.get(len - 2).put("lastModifiedDate", documentValues.get("lastModifiedDate"));
    }

    List<Map<String, AttributeValue>> tagValues =
        getSaveTagsAttributes(siteId, document.getDocumentId(), tags, options.timeToLive());

    WriteRequestBuilder writeBuilder = new WriteRequestBuilder()
        .append(this.documentTableName, documentValues).appends(this.documentTableName, tagValues)
        .appends(this.documentTableName, folderIndex);

    String documentVersionsTableName = this.versionsService.getDocumentVersionsTableName();
    if (documentVersionsTableName != null) {
      writeBuilder = writeBuilder.appends(documentVersionsTableName, previousList);
    }

    if (writeBuilder.batchWriteItem(this.dbClient)) {

      List<String> tagKeys =
          notNull(tags).stream().map(t -> t.getKey()).collect(Collectors.toList());
      this.indexWriter.writeTagIndex(siteId, tagKeys);

      if (options.saveDocumentDate()) {
        saveDocumentDate(document);
      }
    }
  }

  @Override
  public void saveDocument(final String siteId, final DocumentItem document,
      final Collection<DocumentTag> tags) {
    SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(true).timeToLive(null);
    Map<String, AttributeValue> keys = keysDocument(siteId, document.getDocumentId());
    saveDocument(keys, siteId, document, tags, options);
  }

  @Override
  public void saveDocument(final String siteId, final DocumentItem document,
      final Collection<DocumentTag> tags, final SaveDocumentOptions options) {
    Map<String, AttributeValue> keys = keysDocument(siteId, document.getDocumentId());
    saveDocument(keys, siteId, document, tags, options);
  }

  /**
   * Save Document Date record, if it already doesn't exist.
   * 
   * @param document {@link DocumentItem}
   */
  private void saveDocumentDate(final DocumentItem document) {

    Date insertedDate =
        document.getInsertedDate() != null ? document.getInsertedDate() : new Date();
    String shortdate = this.yyyymmddFormat.format(insertedDate);

    if (this.lastShortDate == null || !this.lastShortDate.equals(shortdate)) {

      this.lastShortDate = shortdate;

      Map<String, AttributeValue> values =
          Map.of(PK, AttributeValue.builder().s(PREFIX_DOCUMENT_DATE).build(), SK,
              AttributeValue.builder().s(shortdate).build());
      String conditionExpression = "attribute_not_exists(" + PK + ")";
      PutItemRequest put = PutItemRequest.builder().tableName(this.documentTableName)
          .conditionExpression(conditionExpression).item(values).build();

      try {
        this.dbClient.putItem(put).attributes();
      } catch (ConditionalCheckFailedException e) {
        // Conditional Check Fails on second insert attempt
      }
    }
  }

  @Override
  public DocumentFormat saveDocumentFormat(final String siteId, final DocumentFormat format) {

    Date insertedDate = format.getInsertedDate();
    String fulldate = this.df.format(insertedDate);

    Map<String, AttributeValue> pkvalues =
        keysDocumentFormats(siteId, format.getDocumentId(), format.getContentType());

    addS(pkvalues, "documentId", format.getDocumentId());
    addS(pkvalues, "inserteddate", fulldate);
    addS(pkvalues, "contentType", format.getContentType());
    addS(pkvalues, "userId", format.getUserId());

    save(pkvalues);

    return format;
  }

  /**
   * Generate Tags for {@link DocumentItemWithTags}.
   * 
   * @param siteId {@link String}
   * @param doc {@link DynamicDocumentItem}
   * @param date {@link Date}
   * @param username {@link String}
   * @return {@link List} {@link DocumentTag}
   */
  private List<DocumentTag> saveDocumentItemGenerateTags(final String siteId,
      final DynamicDocumentItem doc, final Date date, final String username) {

    boolean docexists = exists(siteId, doc.getDocumentId());
    List<DynamicObject> doctags = doc.getList("tags");

    List<DocumentTag> tags = doctags.stream().filter(t -> t.containsKey("key")).map(t -> {
      DynamicObjectToDocumentTag transform = new DynamicObjectToDocumentTag(this.df);
      DocumentTag tag = transform.apply(t);
      tag.setInsertedDate(date);
      tag.setUserId(username);
      return tag;
    }).collect(Collectors.toList());

    if (!docexists && tags.isEmpty()) {
      tags.add(
          new DocumentTag(null, "untagged", "true", date, username, DocumentTagType.SYSTEMDEFINED));
    }

    return tags;
  }

  @Override
  public DocumentItem saveDocumentItemWithTag(final String siteId, final DynamicDocumentItem doc) {

    final Date date = new Date();
    String username = doc.getUserId();
    String documentId = resetDatabaseKey(siteId, doc.getDocumentId());

    if (isDocumentUserTagged(doc.getList("tags"))) {
      deleteDocumentTag(siteId, documentId, "untagged");
    }

    DocumentItem item = new DocumentItemDynamoDb(documentId, null, username);

    String path = doc.getPath();

    item.setDocumentId(doc.getDocumentId());
    item.setPath(path);
    item.setContentType(doc.getContentType());
    item.setChecksum(doc.getChecksum());
    item.setContentLength(doc.getContentLength());
    item.setUserId(doc.getUserId());
    item.setBelongsToDocumentId(doc.getBelongsToDocumentId());
    item.setTagSchemaId(doc.getTagSchemaId());
    item.setMetadata(doc.getMetadata());

    List<DocumentTag> tags = saveDocumentItemGenerateTags(siteId, doc, date, username);

    boolean saveGsi1 = doc.getBelongsToDocumentId() == null;
    SaveDocumentOptions options = new SaveDocumentOptions().saveDocumentDate(saveGsi1)
        .timeToLive(doc.getString("TimeToLive"));

    Map<String, AttributeValue> keys = keysDocument(siteId, item.getDocumentId());
    saveDocument(keys, siteId, item, tags, options);

    saveChildDocuments(siteId, doc, item, date);

    return item;
  }

  @Override
  public Preset savePreset(final String siteId, final String id, final String type,
      final Preset preset, final List<PresetTag> tags) {

    if (preset != null) {
      Date insertedDate = preset.getInsertedDate();
      String fulldate = this.df.format(insertedDate);

      Map<String, AttributeValue> pkvalues = keysPreset(siteId, preset.getId());
      addS(pkvalues, "inserteddate", fulldate);
      addS(pkvalues, "tagKey", preset.getName());
      addS(pkvalues, "type", preset.getType());
      addS(pkvalues, "userId", preset.getUserId());
      addS(pkvalues, "documentId", preset.getId());
      pkvalues.putAll(keysPresetGsi2(siteId, id, type, preset.getName()));

      save(pkvalues);
    }

    if (tags != null) {

      for (PresetTag tag : tags) {

        Date insertedDate = tag.getInsertedDate();
        String fulldate = this.df.format(insertedDate);

        Map<String, AttributeValue> pkvalues = keysPresetTag(siteId, id, tag.getKey());
        addS(pkvalues, "inserteddate", fulldate);
        addS(pkvalues, "userId", tag.getUserId());
        addS(pkvalues, "tagKey", tag.getKey());

        save(pkvalues);
      }
    }

    return preset;
  }

  /**
   * Set Last Short Date.
   * 
   * @param date {@link String}
   */
  public void setLastShortDate(final String date) {
    this.lastShortDate = date;
  }

  /**
   * Sort {@link DocumentItem} to match DocumentIds {@link List}.
   * 
   * @param documentIds {@link List} {@link String}
   * @param documents {@link List} {@link DocumentItem}
   * @return {@link List} {@link DocumentItem}
   */
  private List<DocumentItem> sortByIds(final List<String> documentIds,
      final List<DocumentItem> documents) {
    Map<String, DocumentItem> map = documents.stream()
        .collect(Collectors.toMap(DocumentItem::getDocumentId, Function.identity()));
    return documentIds.stream().map(id -> map.get(id)).filter(i -> i != null)
        .collect(Collectors.toList());
  }

  @Override
  public void updateDocument(final String siteId, final String documentId,
      final Map<String, AttributeValue> attributes, final boolean updateVersioning) {

    Map<String, AttributeValue> keys = keysDocument(siteId, documentId);

    if (updateVersioning) {

      String documentVersionsTableName = this.versionsService.getDocumentVersionsTableName();
      if (documentVersionsTableName != null) {

        Map<String, AttributeValue> current =
            new HashMap<>(this.dbService.get(keys.get(PK), keys.get(SK)));
        Map<String, AttributeValue> updated = new HashMap<>(current);
        updated.putAll(attributes);

        String fullLastModifiedDate = this.df.format(new Date());
        addS(updated, "lastModifiedDate", fullLastModifiedDate);

        this.versionsService.addDocumentVersionAttributes(current, updated);

        WriteRequestBuilder writeBuilder = new WriteRequestBuilder()
            .append(this.documentTableName, updated).append(documentVersionsTableName, current);

        writeBuilder.batchWriteItem(this.dbClient);
      }
    }

    this.dbService.updateFields(keys.get(PK), keys.get(SK), attributes);
  }

  /**
   * Update Folder Index from any existing Folder Index.
   * 
   * @param folderIndex {@link List} {@link Map}
   * @return {@link List} {@link Map}
   */
  private List<Map<String, AttributeValue>> updateFromExistingFolder(
      final List<Map<String, AttributeValue>> folderIndex) {

    List<Map<String, AttributeValue>> indexKeys = folderIndex.stream()
        .map(f -> Map.of(PK, f.get(PK), SK, f.get(SK))).collect(Collectors.toList());

    if (!indexKeys.isEmpty()) {

      List<Map<String, AttributeValue>> attrs = getBatch(indexKeys).get(this.documentTableName);

      if (!notNull(attrs).isEmpty()) {

        folderIndex.forEach(f -> {

          Optional<Map<String, AttributeValue>> o = attrs.stream()
              .filter(
                  a -> a.get(PK).s().equals(f.get(PK).s()) && a.get(SK).s().equals(f.get(SK).s()))
              .findFirst();

          if (o.isPresent()) {
            f.put("inserteddate", o.get().get("inserteddate"));
            f.put("lastModifiedDate", o.get().get("lastModifiedDate"));
            f.put("userId", o.get().get("userId"));
          }

        });
      }
    }

    return folderIndex;
  }
}
