/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.stacks.dynamodb;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.resetDatabaseKey;
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
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.dynamodb.model.DocumentTagType;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest.Builder;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

/** Implementation of the {@link DocumentService}. */
public class DocumentServiceImpl implements DocumentService, DbKeys {

  /** {@link DateTimeFormatter}. */
  private DateTimeFormatter yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  /** {@link SimpleDateFormat} YYYY-mm-dd format. */
  private SimpleDateFormat yyyymmddFormat;

  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();

  /** Documents Table Name. */
  private String documentTableName;

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dynamoDB;

  /**
   * constructor.
   *
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public DocumentServiceImpl(final DynamoDbConnectionBuilder builder, final String documentsTable) {
    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;

    this.yyyymmddFormat = new SimpleDateFormat("yyyy-MM-dd");

    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.yyyymmddFormat.setTimeZone(tz);
  }

  @Override
  public void addTags(final String siteId, final String documentId,
      final Collection<DocumentTag> tags, final String timeToLive) {

    if (tags != null) {
      Predicate<DocumentTag> predicate = tag -> DocumentTagType.SYSTEMDEFINED.equals(tag.getType())
          || !SYSTEM_DEFINED_TAGS.contains(tag.getKey());

      DocumentTagToAttributeValueMap mapper =
          new DocumentTagToAttributeValueMap(this.df, PREFIX_DOCS, siteId, documentId);

      List<Map<String, AttributeValue>> valueList = tags.stream().filter(predicate).map(mapper)
          .flatMap(List::stream).collect(Collectors.toList());

      if (timeToLive != null) {
        valueList.forEach(v -> addN(v, "TimeToLive", timeToLive));
      }
      
      List<Put> putitems = valueList.stream()
          .map(values -> Put.builder().tableName(this.documentTableName).item(values).build())
          .collect(Collectors.toList());

      List<TransactWriteItem> writes = putitems.stream()
          .map(i -> TransactWriteItem.builder().put(i).build()).collect(Collectors.toList());

      if (!writes.isEmpty()) {
        this.dynamoDB
            .transactWriteItems(TransactWriteItemsRequest.builder().transactItems(writes).build());
      }
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

    return this.dynamoDB.deleteItem(deleteItemRequest);
  }

  @Override
  public void deleteDocument(final String siteId, final String documentId) {

    Map<String, AttributeValue> startkey = null;

    do {
      Map<String, AttributeValue> values =
          queryKeys(keysGeneric(siteId, PREFIX_DOCS + documentId, null));
      
      QueryRequest q = QueryRequest.builder().tableName(this.documentTableName)
          .keyConditionExpression(PK + " = :pk")
          .expressionAttributeValues(values).limit(Integer.valueOf(MAX_RESULTS)).build();

      QueryResponse response = this.dynamoDB.query(q);
      List<Map<String, AttributeValue>> results = response.items();
      
      for (Map<String, AttributeValue> map : results) {
        deleteItem(Map.of("PK", map.get("PK"), "SK", map.get("SK")));
      }
      
      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());
    
    deleteItem(keysDocument(siteId, documentId, Optional.empty()));
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
   * Delete Document Row by Parition / Sort Key.
   * 
   * @param key DocumentDb Key {@link Map}
   */
  private void deleteItem(final Map<String, AttributeValue> key) {

    DeleteItemRequest deleteItemRequest =
        DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

    this.dynamoDB.deleteItem(deleteItemRequest);
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
    GetItemRequest r = GetItemRequest.builder().key(keysDocument(siteId, documentId))
        .tableName(this.documentTableName).projectionExpression("PK").build();
    return this.dynamoDB.getItem(r).hasItem();
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

    Map<String, AttributeValue> result = this.dynamoDB.getItem(r).item();
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

    QueryResponse result = this.dynamoDB.query(q);
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
        .tableName(this.documentTableName).build();
    
    Map<String, AttributeValue> result = this.dynamoDB.getItem(r).item();

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

        QueryResponse response = this.dynamoDB.query(q);
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

      Map<String, KeysAndAttributes> requestedItems =
          Map.of(this.documentTableName, KeysAndAttributes.builder().keys(keys).build());

      BatchGetItemRequest batchReq =
          BatchGetItemRequest.builder().requestItems(requestedItems).build();
      BatchGetItemResponse batchResponse = this.dynamoDB.batchGetItem(batchReq);

      Collection<List<Map<String, AttributeValue>>> values = batchResponse.responses().values();
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
  private QueryResponse findDocumentTagAttributes(final String siteId,
      final String documentId, final String tagKey, final Integer maxresults) {

    Map<String, AttributeValue> values = queryKeys(keysDocumentTag(siteId, documentId, tagKey));

    Builder req = QueryRequest.builder().tableName(this.documentTableName)
        .keyConditionExpression(PK + " = :pk and begins_with(" + SK + ", :sk)")
        .expressionAttributeValues(values);
    
    if (maxresults != null) {
      req = req.limit(maxresults);
    }
    
    QueryRequest q = req.build();        
    QueryResponse result = this.dynamoDB.query(q);
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
   * Get {@link DynamoDbClient}.
   *
   * @return {@link DynamoDbClient}
   */
  public DynamoDbClient getDynamoDB() {
    return this.dynamoDB;
  }

  /**
   * Is {@link List} {@link DynamicObject} contain a non generated tag.
   * 
   * @param tags {@link List} {@link DynamicObject}
   * @return boolean
   */
  private boolean isDocumentUserTagged(final List<DynamicObject> tags) {
    return tags != null
        ? tags.stream().filter(t -> !SYSTEM_DEFINED_TAGS.contains(t.getString("key"))).count() > 0
        : false;
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
    values.put(":pk", AttributeValue.builder()
        .s(createDatabaseKey(siteId, pk)).build());

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

    QueryResponse result = this.dynamoDB.query(q);

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
    
    deletes.forEach(i -> this.dynamoDB.deleteItem(i));
    puts.forEach(i -> this.dynamoDB.putItem(i));
    
    return !deletes.isEmpty();
  }
  
  @Override
  public void removeTags(final String siteId, final String documentId,
      final Collection<String> tags) {

    for (String tag : tags) {
      QueryResponse response = findDocumentTagAttributes(siteId, documentId, tag, null);
      List<Map<String, AttributeValue>> items = response.items();
      items.forEach(i -> {
        Map<String, AttributeValue> key = keysGeneric(i.get("PK").s(), i.get("SK").s());
        DeleteItemRequest deleteItemRequest =
            DeleteItemRequest.builder().tableName(this.documentTableName).key(key).build();

        this.dynamoDB.deleteItem(deleteItemRequest);
      });
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

    return this.dynamoDB.putItem(put).attributes();
  }
  
  /**
   * Save {@link DocumentItemDynamoDb}.
   * 
   * @param keys {@link Map}
   * @param siteId DynamoDB PK siteId
   * @param document {@link DocumentItem}
   * @param saveGsi1 boolean
   * @param timeToLive {@link String}
   */
  private void saveDocument(final Map<String, AttributeValue> keys, final String siteId,
      final DocumentItem document, final boolean saveGsi1, final String timeToLive) {

    Date insertedDate = document.getInsertedDate();
    String shortdate = insertedDate != null ? this.yyyymmddFormat.format(insertedDate) : null;
    String fulldate = insertedDate != null ? this.df.format(insertedDate) : null;

    Map<String, AttributeValue> pkvalues = new HashMap<>(keys);

    if (saveGsi1) {
      addS(pkvalues, GSI1_PK, createDatabaseKey(siteId, PREFIX_DOCUMENT_DATE_TS + shortdate));
      addS(pkvalues, GSI1_SK, fulldate + TAG_DELIMINATOR + document.getDocumentId());
    }

    addS(pkvalues, "documentId", document.getDocumentId());
    
    if (fulldate != null) {
      addS(pkvalues, "inserteddate", fulldate);
    }

    addS(pkvalues, "tagSchemaId", document.getTagSchemaId());
    addS(pkvalues, "userId", document.getUserId());
    addS(pkvalues, "path", document.getPath());
    addS(pkvalues, "contentType", document.getContentType());

    if (document.getContentLength() != null) {
      addN(pkvalues, "contentLength", "" + document.getContentLength());
    }

    if (document.getChecksum() != null) {
      String etag = document.getChecksum().replaceAll("^\"|\"$", "");
      addS(pkvalues, "etag", etag);
    }

    if (document.getBelongsToDocumentId() != null) {
      addS(pkvalues, "belongsToDocumentId", document.getBelongsToDocumentId());
    }
    
    if (timeToLive != null) {
      addN(pkvalues, "TimeToLive", timeToLive);
    }

    save(pkvalues);
  }

  /**
   * Save Document.
   * 
   * @param keys {@link Map}
   * @param siteId {@link String}
   * @param document {@link DocumentItem}
   * @param tags {@link Collection} {@link DocumentTag}
   * @param saveGsi1 boolean
   * @param timeToLive {@link String}
   */
  private void saveDocument(final Map<String, AttributeValue> keys, final String siteId,
      final DocumentItem document, final Collection<DocumentTag> tags, final boolean saveGsi1,
      final String timeToLive) {
    // TODO save Document/Tags inside transaction.
    saveDocument(keys, siteId, document, saveGsi1, timeToLive);
    addTags(siteId, document.getDocumentId(), tags, timeToLive);
    
    if (saveGsi1) {
      saveDocumentDate(document);
    }
  }

  @Override
  public void saveDocument(final String siteId, final DocumentItem document,
      final Collection<DocumentTag> tags) {
    Map<String, AttributeValue> keys = keysDocument(siteId, document.getDocumentId());
    saveDocument(keys, siteId, document, tags, true, null);
  }

  /** 
   * Save Document Date record, if it already doesn't exist.
   * @param document {@link DocumentItem}
   */
  private void saveDocumentDate(final DocumentItem document) {
    Date insertedDate = document.getInsertedDate();
    String shortdate = this.yyyymmddFormat.format(insertedDate);
    
    Map<String, AttributeValue> values =
        Map.of(PK, AttributeValue.builder().s(PREFIX_DOCUMENT_DATE).build(), SK,
            AttributeValue.builder().s(shortdate).build());
    String conditionExpression = "attribute_not_exists(" + PK + ")";
    PutItemRequest put = PutItemRequest.builder().tableName(this.documentTableName)
        .conditionExpression(conditionExpression).item(values).build();
    
    try {
      this.dynamoDB.putItem(put).attributes();
    } catch (ConditionalCheckFailedException e) {
      // Conditional Check Fails on second insert attempt
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
    List<DocumentTag> tags = doctags.stream().map(t -> {
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

    if (doc.getPath() != null) {
      tags.add(new DocumentTag(null, "path", doc.getPath(), date, username,
          DocumentTagType.SYSTEMDEFINED));
    }

    if (doc.getUserId() != null) {
      tags.add(new DocumentTag(null, "userId", doc.getUserId(), date, username,
          DocumentTagType.SYSTEMDEFINED));
    }
    
    return tags;
  }

  @Override
  public DocumentItem saveDocumentItemWithTag(final String siteId, final DynamicDocumentItem doc) {

    Date date = new Date();
    String username = doc.getUserId();
    String documentId = resetDatabaseKey(siteId, doc.getDocumentId());
    
    if (isDocumentUserTagged(doc.getList("tags"))) {
      deleteDocumentTag(siteId, documentId, "untagged");
    }

    DocumentItem item = new DocumentItemDynamoDb(documentId, date, username);

    String path = doc.getPath();

    item.setDocumentId(doc.getDocumentId());
    item.setPath(path);
    item.setContentType(doc.getContentType());
    item.setChecksum(doc.getChecksum());
    item.setContentLength(doc.getContentLength());
    item.setUserId(doc.getUserId());
    item.setInsertedDate(doc.getInsertedDate() != null ? doc.getInsertedDate() : date);
    item.setBelongsToDocumentId(doc.getBelongsToDocumentId());
    item.setTagSchemaId(doc.getTagSchemaId());

    List<DocumentTag> tags = saveDocumentItemGenerateTags(siteId, doc, date, username);

    boolean saveGsi1 = doc.getBelongsToDocumentId() == null;
    Map<String, AttributeValue> keys = keysDocument(siteId, item.getDocumentId());
    saveDocument(keys, siteId, item, tags, saveGsi1, doc.getString("TimeToLive"));

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
      saveDocument(keys, siteId, dockey, null, false, doc.getString("TimeToLive"));

      List<DynamicObject> doctags = subdoc.getList("tags");
      tags = doctags.stream().map(t -> {
        DynamicObjectToDocumentTag transformer = new DynamicObjectToDocumentTag(this.df);
        return transformer.apply(t);
      }).collect(Collectors.toList());

      keys = keysDocument(siteId, subdoc.getString("documentId"));
      saveDocument(keys, siteId, document, tags, false, doc.getString("TimeToLive"));
    }

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
   * Sort {@link DocumentItem} to match DocumentIds {@link List}.
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
}
