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
package com.formkiq.aws.dynamodb;

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 *
 * Implementation of {@link DynamoDbService}.
 *
 */
public final class DynamoDbServiceImpl implements DynamoDbService {

  /** Max Backoff in MS. */
  private static final int MAX_BACKOFF_IN_MS = 2000;
  /** Default Backoff In Ms. */
  private static final int DEFAULT_BACKOFF_IN_MS = 200;
  /** Thousand constant. */
  private static final int TS = 1000;
  /** 1 Hour in Seconds. */
  public static final int TIME_TO_LIVE_IN_SECONDS = 3600;

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** Table Name. */
  private final String tableName;

  /**
   * constructor.
   *
   * @param client {@link DynamoDbClient}
   * @param dynamoDbTableName {@link String}
   */
  public DynamoDbServiceImpl(final DynamoDbClient client, final String dynamoDbTableName) {
    if (dynamoDbTableName == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = client;
    this.tableName = dynamoDbTableName;
  }

  /**
   * constructor.
   *
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param dynamoDbTableName {@link String}
   */
  public DynamoDbServiceImpl(final DynamoDbConnectionBuilder connection,
      final String dynamoDbTableName) {
    if (dynamoDbTableName == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dbClient = connection.build();
    this.tableName = dynamoDbTableName;
  }

  @Override
  public boolean acquireLock(final DynamoDbKey key, final long aquireLockTimeoutInMs,
      final long lockExpirationInMs) {

    boolean lock = false;
    long expirationTime = Instant.now().getEpochSecond() + lockExpirationInMs / TS;

    Map<String, AttributeValue> item = new HashMap<>(key.toMap());
    item.put(SK, getLock(fromS(key.sk())));
    item.put("ExpirationTime", AttributeValue.builder().n(Long.toString(expirationTime)).build());

    long ttl = Instant.now().getEpochSecond() + TIME_TO_LIVE_IN_SECONDS;
    item.put("TimeToLive", AttributeValue.builder().n(String.valueOf(ttl)).build());

    Put.Builder put = Put.builder().tableName(tableName).item(item).conditionExpression(
        "(attribute_not_exists(PK) and attribute_not_exists(SK)) OR ExpirationTime < :currentTime");

    long startTime = System.currentTimeMillis();
    long waitInterval = DEFAULT_BACKOFF_IN_MS;

    while (System.currentTimeMillis() - startTime < aquireLockTimeoutInMs) {

      try {

        put.expressionAttributeValues(Map.of(":currentTime",
            AttributeValue.builder().n(Long.toString(Instant.now().getEpochSecond())).build()));

        TransactWriteItemsRequest tx = TransactWriteItemsRequest.builder()
            .transactItems(TransactWriteItem.builder().put(put.build()).build()).build();

        this.dbClient.transactWriteItems(tx);
        lock = true;
        break;

      } catch (TransactionCanceledException e) {
        // Lock is already held or transaction was canceled, wait and retry with exponential backoff
        try {
          TimeUnit.MILLISECONDS.sleep(waitInterval);
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }

        // Cap backoff at 1 second
        waitInterval = Math.min(waitInterval * 2, MAX_BACKOFF_IN_MS);
      }
    }

    return lock;
  }

  @Override
  public QueryResponse between(final QueryConfig config, final AttributeValue pk,
      final AttributeValue skStart, final AttributeValue skEnd,
      final Map<String, AttributeValue> startkey, final int limit) {

    String gsi = isEmpty(config.indexName()) ? "" : config.indexName();

    String expression = gsi + PK + " = :pk and " + gsi + SK + " between :start and :end";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", pk);
    values.put(":start", skStart);
    values.put(":end", skEnd);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
            .projectionExpression(config.projectionExpression()).indexName(config.indexName())
            .exclusiveStartKey(startkey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public boolean deleteItem(final AttributeValue pk, final AttributeValue sk) {
    Map<String, AttributeValue> sourceKey = Map.of(PK, pk, SK, sk);
    return deleteItem(sourceKey);
  }

  @Override
  public DeleteResult deleteItem(final DynamoDbKey key) {
    Map<String, AttributeValue> sourceKey = Map.of(PK, fromS(key.pk()), SK, fromS(key.sk()));
    DeleteItemResponse response = this.dbClient.deleteItem(DeleteItemRequest.builder()
        .tableName(this.tableName).key(sourceKey).returnValues(ReturnValue.ALL_OLD).build());
    return new DeleteResult(response.attributes());
  }

  @Override
  public boolean deleteItem(final Map<String, AttributeValue> key) {
    DeleteItemResponse response = this.dbClient.deleteItem(DeleteItemRequest.builder()
        .tableName(this.tableName).key(key).returnValues(ReturnValue.ALL_OLD).build());
    return !response.attributes().isEmpty();
  }

  @Override
  public boolean deleteItems(final Collection<Map<String, AttributeValue>> attrs) {

    boolean deleted = false;

    if (!attrs.isEmpty()) {

      List<WriteRequest> writes = attrs.stream().map(
          a -> WriteRequest.builder().deleteRequest(DeleteRequest.builder().key(a).build()).build())
          .collect(Collectors.toList());

      WriteRequestBuilder builder = new WriteRequestBuilder().append(this.tableName, writes);
      deleted = builder.batchWriteItem(this.dbClient);
    }

    return deleted;
  }

  @Override
  public boolean deleteItems(final String dbTableName, final Collection<DynamoDbKey> keys) {
    boolean deleted = false;

    if (!keys.isEmpty()) {

      List<WriteRequest> writes = keys.stream()
          .map(a -> WriteRequest.builder()
              .deleteRequest(DeleteRequest.builder().key(a.toMap()).build()).build())
          .collect(Collectors.toList());

      WriteRequestBuilder builder = new WriteRequestBuilder().append(dbTableName, writes);
      deleted = builder.batchWriteItem(this.dbClient);
    }

    return deleted;
  }

  @Override
  public boolean deleteItemsBeginsWith(final AttributeValue pk, final AttributeValue sk) {
    return deleteItemsBeginsWith(new DynamoDbKey(DynamoDbTypes.toString(pk),
        DynamoDbTypes.toString(sk), null, null, null, null), "PK,SK").deleted();
  }

  @Override
  public DeleteResults deleteItemsBeginsWith(final DynamoDbKey key, final String projection) {

    final int limit = 100;
    Map<String, AttributeValue> startkey = null;
    List<Map<String, AttributeValue>> list = new ArrayList<>();
    QueryConfig config = new QueryConfig().projectionExpression(projection);

    do {

      AttributeValue pk = !isEmpty(key.pk()) ? fromS(key.pk()) : null;
      AttributeValue sk = !isEmpty(key.sk()) ? fromS(key.sk()) : null;
      QueryResponse response = queryBeginsWith(config, pk, sk, startkey, limit);

      List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    Collection<Map<String, AttributeValue>> attrs =
        list.stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();
    boolean deleted = deleteItems(attrs);

    return new DeleteResults(deleted, list);
  }

  @Override
  public boolean deleteItemsBeginsWith(final String indexName, final AttributeValue pk) {

    final int limit = 100;
    List<Map<String, AttributeValue>> list = new ArrayList<>();

    Map<String, AttributeValue> expressionAttributeValues = Map.of(":pkValue", pk);

    String prefix = indexName != null ? indexName : "";
    String filterExpression = "begins_with(" + prefix + "PK, :pkValue)";
    ScanRequest.Builder scanRequest = ScanRequest.builder().tableName(this.tableName)
        .indexName(indexName).limit(limit).filterExpression(filterExpression)
        .expressionAttributeValues(expressionAttributeValues).projectionExpression("PK,SK");

    Map<String, AttributeValue> startkey;

    do {

      ScanResponse response = this.dbClient.scan(scanRequest.build());

      List<Map<String, AttributeValue>> attrs = response.items().stream().toList();
      list.addAll(attrs);

      startkey = response.lastEvaluatedKey();
      scanRequest.exclusiveStartKey(response.lastEvaluatedKey());

    } while (startkey != null && !startkey.isEmpty());

    return deleteItems(list);
  }

  @Override
  public boolean exists(final AttributeValue pk, final AttributeValue sk) {
    GetItemRequest r = GetItemRequest.builder().key(Map.of(PK, pk, SK, sk))
        .tableName(this.tableName).projectionExpression("PK").consistentRead(Boolean.TRUE).build();
    GetItemResponse response = this.dbClient.getItem(r);
    return !response.item().isEmpty();
  }

  @Override
  public Collection<DynamoDbKey> exists(final Collection<DynamoDbKey> keys) {
    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK");
    List<Map<String, AttributeValue>> fetchKeys = keys.stream().map(DynamoDbKey::toMap).toList();
    List<Map<String, AttributeValue>> batch = getBatch(config, fetchKeys);
    return batch.stream().map(b -> new DynamoDbKey(b.get(PK).s(), b.get(SK).s(), "", "", "", ""))
        .toList();
  }

  @Override
  public boolean exists(final DynamoDbKey key) {
    return exists(this.tableName, key);
  }

  @Override
  public boolean exists(final QueryRequest query) {
    QueryResponse response = this.dbClient.query(query);
    return !response.items().isEmpty();
  }

  @Override
  public boolean exists(final String dynamodbTableName, final DynamoDbKey key) {

    GetItemRequest r = GetItemRequest.builder().key(key.toMap()).tableName(dynamodbTableName)
        .projectionExpression("PK").build();

    GetItemResponse response = this.dbClient.getItem(r);
    return !response.item().isEmpty();
  }

  @Override
  public Map<String, AttributeValue> get(final AttributeValue pk, final AttributeValue sk) {
    return get(new QueryConfig(), pk, sk);
  }

  @Override
  public Map<String, AttributeValue> get(final DynamoDbKey key) {
    return get(new QueryConfig(), fromS(key.pk()), fromS(key.sk()));
  }

  @Override
  public Map<String, AttributeValue> get(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk) {
    Map<String, AttributeValue> key = Map.of(PK, pk, SK, sk);
    return this.dbClient.getItem(GetItemRequest.builder().tableName(this.tableName).key(key)
        .projectionExpression(config.projectionExpression())
        .expressionAttributeNames(config.expressionAttributeNames()).consistentRead(Boolean.TRUE)
        .build()).item();
  }

  @Override
  public Collection<Map<String, AttributeValue>> get(final String dbTableName,
      final Collection<DynamoDbKey> key) {
    List<Map<String, AttributeValue>> list = key.stream().map(DynamoDbKey::toMap).toList();

    ReadRequestBuilder request = new ReadRequestBuilder().append(dbTableName, list);
    Map<String, List<Map<String, AttributeValue>>> stringListMap =
        request.batchReadItems(this.dbClient, new BatchGetConfig());
    return stringListMap.values().stream().flatMap(Collection::stream).toList();
  }

  @Override
  public Map<String, AttributeValue> getAquiredLock(final DynamoDbKey key) {
    return get(fromS(key.pk()), getLock(fromS(key.sk())));
  }

  @Override
  public List<Map<String, AttributeValue>> getBatch(final BatchGetConfig config,
      final List<Map<String, AttributeValue>> attributes) {
    return getBatch(this.tableName, config, attributes);
  }

  private List<Map<String, AttributeValue>> getBatch(final String dbTableName,
      final BatchGetConfig config, final List<Map<String, AttributeValue>> attributes) {

    List<Map<String, AttributeValue>> keys =
        attributes.stream().map(a -> Map.of(PK, a.get(PK), SK, a.get(SK))).toList();

    List<Map<String, AttributeValue>> list = Collections.emptyList();

    if (!keys.isEmpty()) {

      ReadRequestBuilder builder = new ReadRequestBuilder();
      builder.append(dbTableName, keys);

      Map<String, List<Map<String, AttributeValue>>> batchReadItems =
          builder.batchReadItems(this.dbClient, config);

      list = batchReadItems.get(dbTableName);

      Map<String, Map<String, AttributeValue>> data =
          list.stream().collect(Collectors.toMap(this::getKey, l -> l));

      list = keys.stream().map(k -> data.get(getKey(k))).filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    return list;
  }

  @Override
  public Map<String, AttributeValue> getByQuery(final QueryRequest query) {
    Map<String, AttributeValue> attributes = Collections.emptyMap();
    QueryResponse response = this.dbClient.query(query);
    if (!response.items().isEmpty()) {
      attributes = response.items().get(0);
    }

    return attributes;
  }

  private String getKey(final Map<String, AttributeValue> attr) {
    return attr.get(PK).s() + "#" + attr.get(SK).s();
  }

  private AttributeValue getLock(final AttributeValue sk) {
    return fromS(sk.s() + ".lock");
  }

  @Override
  public Long getNextNumber(final Map<String, AttributeValue> keys) {

    UpdateItemRequest updateItemRequest = UpdateItemRequest.builder().tableName(this.tableName)
        .key(keys).updateExpression("ADD #autoIncrement :val")
        .expressionAttributeNames(Map.of("#autoIncrement", "Number"))
        .expressionAttributeValues(Map.of(":val", AttributeValue.builder().n("1").build()))
        .returnValues(ReturnValue.UPDATED_NEW).build();

    UpdateItemResponse response = updateItem(updateItemRequest);

    AttributeValue val = response.attributes().get("Number");
    return Long.valueOf(val.n());
  }

  @Override
  public String getTableName() {
    return this.tableName;
  }

  @Override
  public boolean moveItems(final Collection<Map<String, AttributeValue>> attrs,
      final MoveAttributeFunction func) {

    List<WriteRequest> writes = new ArrayList<>();

    for (Map<String, AttributeValue> attr : attrs) {

      Map<String, AttributeValue> newAttr = func.transform(attr);

      Map<String, AttributeValue> key = Map.of(PK, attr.get(PK), SK, attr.get(SK));
      WriteRequest del =
          WriteRequest.builder().deleteRequest(DeleteRequest.builder().key(key).build()).build();
      writes.add(del);

      WriteRequest add =
          WriteRequest.builder().putRequest(PutRequest.builder().item(newAttr).build()).build();
      writes.add(add);
    }

    WriteRequestBuilder builder = new WriteRequestBuilder().append(this.tableName, writes);
    return builder.batchWriteItem(this.dbClient);
  }

  @Override
  public void putInTransaction(final Collection<Map<String, AttributeValue>> attributes) {

    TransactWriteItemsRequest.Builder transactionRequestBuilder =
        TransactWriteItemsRequest.builder();

    List<Put> puts =
        attributes.stream().map(a -> Put.builder().tableName(tableName).item(a).build()).toList();

    List<TransactWriteItem> writes =
        puts.stream().map(p -> TransactWriteItem.builder().put(p).build()).toList();

    TransactWriteItemsRequest transactionRequest =
        transactionRequestBuilder.transactItems(writes).build();
    this.dbClient.transactWriteItems(transactionRequest);
  }

  @Override
  public void putInTransaction(final WriteRequestBuilder writeRequest) {
    writeRequest.batchWriteItem(this.dbClient);
  }

  @Override
  public void putItem(final Map<String, AttributeValue> attributes) {
    putItem(this.tableName, attributes);
  }

  @Override
  public PutResult putItem(final Map<String, AttributeValue> attributes,
      final ReturnValue returnValue) {
    return putItem(this.tableName, attributes, returnValue);
  }

  @Override
  public void putItem(final String dynamoDbTable, final Map<String, AttributeValue> attributes) {
    putItem(dynamoDbTable, attributes, null);
  }

  private PutResult putItem(final String dynamoDbTable,
      final Map<String, AttributeValue> attributes, final ReturnValue returnValue) {
    PutItemResponse putItemResponse = this.dbClient.putItem(PutItemRequest.builder()
        .tableName(dynamoDbTable).item(attributes).returnValues(returnValue).build());
    return new PutResult(putItemResponse.attributes());
  }

  @Override
  public void putItems(final List<Map<String, AttributeValue>> attrs) {
    putItems(this.tableName, attrs);
  }

  @Override
  public void putItems(final String dynamoTableName,
      final List<Map<String, AttributeValue>> attrs) {

    if (!attrs.isEmpty()) {
      WriteRequestBuilder builder = new WriteRequestBuilder();
      for (Map<String, AttributeValue> attr : attrs) {
        builder.append(dynamoTableName, attr);
      }
      builder.batchWriteItem(this.dbClient);
    }
  }

  @Override
  public QueryResponse query(final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {
    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.FALSE);
    return query(config, pk, exclusiveStartKey, limit);
  }

  @Override
  public QueryResponse query(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk, final Map<String, AttributeValue> exclusiveStartKey,
      final int limit) {

    String gsi = isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk and " + gsi + SK + " = :sk";
    Map<String, AttributeValue> values = Map.of(":pk", pk, ":sk", sk);

    QueryRequest q = QueryRequest.builder().tableName(this.tableName).indexName(config.indexName())
        .expressionAttributeNames(config.expressionAttributeNames())
        .keyConditionExpression(expression).projectionExpression(config.projectionExpression())
        .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
        .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse query(final QueryConfig config, final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String gsi = isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);

    QueryRequest q = QueryRequest.builder().tableName(this.tableName).indexName(config.indexName())
        .expressionAttributeNames(config.expressionAttributeNames())
        .keyConditionExpression(expression).projectionExpression(config.projectionExpression())
        .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
        .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse query(final QueryRequest q) {
    return query(q, true);
  }

  @Override
  public QueryResponse query(final QueryRequest q, final boolean fetchAllAttributes) {

    try {
      QueryResponse response = this.dbClient.query(q);

      if (q.indexName() != null && fetchAllAttributes) {
        List<Map<String, AttributeValue>> results =
            response.items().stream().map(i -> Map.of(PK, i.get(PK), SK, i.get(SK))).toList();

        results = getBatch(q.tableName(), new BatchGetConfig(), results);

        response = QueryResponse.builder().items(results)
            .lastEvaluatedKey(response.lastEvaluatedKey()).build();
      }

      return response;
    } catch (DynamoDbException e) {
      throw new DynamoDbQueryException(e);
    }
  }

  @Override
  public QueryResponse queryBeginsWith(final QueryConfig config, final AttributeValue pk,
      final AttributeValue sk, final Map<String, AttributeValue> exclusiveStartKey,
      final int limit) {
    return queryBeginsWith(this.tableName, config, pk, sk, exclusiveStartKey, limit);
  }

  @Override
  public QueryResponse queryBeginsWith(final String dynamoDbtableName, final QueryConfig config,
      final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String gsi = isEmpty(config.indexName()) ? "" : config.indexName();
    String expression = gsi + PK + " = :pk and begins_with(" + gsi + SK + ",:sk)";

    if (sk == null) {
      expression = gsi + PK + " = :pk";
    }

    Map<String, AttributeValue> values =
        sk != null ? Map.of(":pk", pk, ":sk", sk) : Map.of(":pk", pk);

    QueryRequest q =
        QueryRequest.builder().tableName(dynamoDbtableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(config.isScanIndexForward())
            .expressionAttributeNames(config.expressionAttributeNames())
            .projectionExpression(config.projectionExpression()).indexName(config.indexName())
            .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public QueryResponse queryIndex(final String indexName, final AttributeValue pk,
      final Map<String, AttributeValue> exclusiveStartKey, final int limit) {

    String expression = indexName + PK + " = :pk";
    Map<String, AttributeValue> values = Map.of(":pk", pk);

    QueryRequest q =
        QueryRequest.builder().tableName(this.tableName).keyConditionExpression(expression)
            .expressionAttributeValues(values).scanIndexForward(Boolean.FALSE).indexName(indexName)
            .exclusiveStartKey(exclusiveStartKey).limit(limit).build();

    return this.dbClient.query(q);
  }

  @Override
  public boolean releaseLock(final DynamoDbKey key) {

    DynamoDbKey k = new DynamoDbKey(key.pk(), getLock(fromS(key.sk())).s(), null, null, null, null);
    Delete deleteOp = Delete.builder().tableName(this.tableName).key(k.toMap())
        .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
        .conditionExpression("attribute_exists(PK) AND attribute_exists(SK)").build();

    TransactWriteItemsRequest txn = TransactWriteItemsRequest.builder()
        .transactItems(TransactWriteItem.builder().delete(deleteOp).build()).build();

    boolean released;
    try {
      dbClient.transactWriteItems(txn);
      released = true;
    } catch (TransactionCanceledException tce) {
      released = false;
    }

    return released;
  }

  @Override
  public Map<String, AttributeValue> updateItem(final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValueUpdate> updateValues) {
    Map<String, AttributeValue> dbKey = Map.of(PK, pk, SK, sk);

    return this.dbClient.updateItem(UpdateItemRequest.builder().tableName(this.tableName).key(dbKey)
        .attributeUpdates(updateValues).build()).attributes();
  }

  @Override
  public UpdateItemResponse updateItem(final UpdateItemRequest request) {
    return this.dbClient.updateItem(request);
  }

  @Override
  public void updateItems(final String dbTableName, final Collection<DynamoDbKey> keys,
      final Map<String, AttributeValueUpdate> updateValues) {
    keys.forEach(k -> {
      Map<String, AttributeValue> dbKey = Map.of(PK, fromS(k.pk()), SK, fromS(k.sk()));
      this.dbClient.updateItem(UpdateItemRequest.builder().tableName(dbTableName).key(dbKey)
          .attributeUpdates(updateValues).build());
    });
  }

  @Override
  public Map<String, AttributeValue> updateValues(final AttributeValue pk, final AttributeValue sk,
      final Map<String, AttributeValue> updateValues) {

    Map<String, AttributeValueUpdate> values = new HashMap<>();
    updateValues.forEach(
        (key, value) -> values.put(key, AttributeValueUpdate.builder().value(value).build()));

    return updateItem(pk, sk, values);
  }
}
