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
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecordBuilder;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.validation.ValidationError;
import com.formkiq.validation.ValidationErrorImpl;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDb implementation of {@link DocumentSyncService}.
 *
 */
public final class DocumentSyncServiceDynamoDb implements DocumentSyncService, DbKeys {

  /** Syncs SK. */
  private static final String SK_SYNCS = "syncs#";

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** Document Table Name. */
  private final String documentTableName;
  /** Document Sync Table Name. */
  private final String syncTableName;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   * @param documentsTable {@link String}
   * @param syncsTable {@link String}
   */
  public DocumentSyncServiceDynamoDb(final DynamoDbService dbService, final String documentsTable,
      final String syncsTable) {

    if (syncsTable == null) {
      throw new IllegalArgumentException("'syncsTable' is null");
    }

    this.documentTableName = documentsTable;
    this.syncTableName = syncsTable;
    this.db = dbService;
  }

  private String getPk(final String siteId, final String documentId) {
    return createDatabaseKey(siteId, PREFIX_DOCS + documentId);
  }

  @Override
  public PaginationResults<DocumentSyncRecord> getSyncs(final String siteId,
      final String documentId, final PaginationMapToken token, final int limit) {

    String pk = getPk(siteId, documentId);
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryRequest query = DynamoDbQueryBuilder.builder().scanIndexForward(Boolean.FALSE)
        .nextToken(startkey).pk(pk).beginsWith(SK_SYNCS).limit(limit).build(this.syncTableName);
    QueryResponse response = this.db.query(query);

    List<DocumentSyncRecord> syncs = response.items().stream()
        .map(a -> new DocumentSyncRecord().getFromAttributes(siteId, a)).toList();

    return new PaginationResults<>(syncs, new QueryResponseToPagination().apply(response));
  }

  @Override
  public void saveSync(final String siteId, final String documentId,
      final DocumentSyncServiceType service, final DocumentSyncStatus status,
      final DocumentSyncType type, final boolean documentExists) {

    DocumentSyncRecord r = new DocumentSyncRecordBuilder().build(documentId, service, status, type,
        new Date(), documentExists);

    this.db.putItem(this.syncTableName, r.getAttributes(siteId));
  }

  @Override
  public void update(final String pk, final String sk, final DocumentSyncStatus status,
      final Date syncDate) {

    DocumentSyncRecord r =
        new DocumentSyncRecord().setSyncDate(new Date()).setInsertedDate(new Date());
    Map<String, AttributeValue> attrs = r.getDataAttributes();

    Map<String, AttributeValueUpdate> updateValues =
        Map.of("status", AttributeValueUpdate.builder().value(fromS(status.name())).build(),
            "syncDate", AttributeValueUpdate.builder().value(attrs.get("syncDate")).build());

    DynamoDbKey key = new DynamoDbKey(pk, sk, "", "", "", "");
    this.db.updateItems(this.syncTableName, List.of(key), updateValues);
  }

  @Override
  public Collection<ValidationError> addSync(final String siteId, final String documentId,
      final DocumentSyncServiceType service, final DocumentSyncType type) {

    Collection<ValidationError> errors = new ArrayList<>();

    switch (service) {
      case OPENSEARCH, TYPESENSE -> {
        if (DocumentSyncType.METADATA.equals(type)) {
          setStreamTriggeredDate(siteId, documentId);
        } else {
          errors.add(new ValidationErrorImpl().key("type")
              .error("unsupport type '" + type + "' for service '" + service + "'"));
        }
      }
      case EVENTBRIDGE ->
        saveSync(siteId, documentId, service, DocumentSyncStatus.PENDING, type, true);
      default -> errors
          .add(new ValidationErrorImpl().key("service").error("invalid service '" + service + "'"));
    }

    return errors;
  }

  private void setStreamTriggeredDate(final String siteId, final String documentId) {

    Map<String, AttributeValue> key = keysDocument(siteId, documentId);

    String pk = key.get(PK).s();
    DynamoDbKey docKey = new DynamoDbKey(pk, key.get(SK).s(), "", "", "", "");

    DynamoDbQueryBuilder tags =
        DynamoDbQueryBuilder.builder().pk(pk).projectionExpression("PK,SK").beginsWith("tags#");
    QueryResponse responseTags = this.db.query(tags.build(this.documentTableName));

    DynamoDbQueryBuilder attrs =
        DynamoDbQueryBuilder.builder().pk(pk).projectionExpression("PK,SK").beginsWith("attr#");
    QueryResponse responseAttrs = this.db.query(attrs.build(this.documentTableName));

    Collection<DynamoDbKey> keys = new ArrayList<>();
    keys.add(docKey);
    responseTags.items().forEach(
        item -> keys.add(new DynamoDbKey(item.get(PK).s(), item.get(SK).s(), "", "", "", "")));
    responseAttrs.items().forEach(
        item -> keys.add(new DynamoDbKey(item.get(PK).s(), item.get(SK).s(), "", "", "", "")));

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();
    AttributeValue val = fromS(df.format(new Date()));

    Map<String, AttributeValueUpdate> updateValues =
        Map.of("streamTriggeredDate", AttributeValueUpdate.builder().value(val).build());
    this.db.updateItems(this.documentTableName, keys, updateValues);
  }

  @Override
  public void deleteAll(final String siteId, final String documentId) {

    final int limit = 100;
    String pk = getPk(siteId, documentId);

    Map<String, AttributeValue> startkey = null;
    List<DynamoDbKey> keys = new ArrayList<>();

    do {

      QueryRequest request = DynamoDbQueryBuilder.builder().nextToken(startkey).pk(pk).limit(limit)
          .beginsWith(SK_SYNCS).projectionExpression("PK,SK").build(this.syncTableName);
      QueryResponse response = this.db.query(request);

      List<Map<String, AttributeValue>> results = response.items();
      for (Map<String, AttributeValue> map : results) {
        keys.add(new DynamoDbKey(map.get(PK).s(), map.get(SK).s(), "", "", "", ""));
      }

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

    this.db.deleteItems(this.syncTableName, keys);
  }
}
