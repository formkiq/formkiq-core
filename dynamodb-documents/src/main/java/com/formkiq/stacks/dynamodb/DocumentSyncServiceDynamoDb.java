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

import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.PaginationToAttributeValue;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.QueryResponseToPagination;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncRecordBuilder;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * DynamoDb implementation of {@link DocumentSyncService}.
 *
 */
public final class DocumentSyncServiceDynamoDb implements DocumentSyncService {

  /** Syncs SK. */
  private static final String SK_SYNCS = "syncs#";

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param syncsTable {@link String}
   */
  public DocumentSyncServiceDynamoDb(final DynamoDbConnectionBuilder connection,
      final String syncsTable) {

    if (syncsTable == null) {
      throw new IllegalArgumentException("'syncsTable' is null");
    }

    this.db = new DynamoDbServiceImpl(connection, syncsTable);
  }

  private String getPk(final String siteId, final String documentId) {
    return createDatabaseKey(siteId, PREFIX_DOCS + documentId);
  }

  @Override
  public PaginationResults<DocumentSyncRecord> getSyncs(final String siteId,
      final String documentId, final PaginationMapToken token, final int limit) {

    QueryConfig config = new QueryConfig().scanIndexForward(Boolean.FALSE);

    String pk = getPk(siteId, documentId);
    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryResponse response = this.db.queryBeginsWith(config, AttributeValue.fromS(pk),
        AttributeValue.fromS(SK_SYNCS), startkey, limit);

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

    this.db.putItem(r.getAttributes(siteId));
  }

  @Override
  public void update(final String pk, final String sk, final DocumentSyncStatus status,
      final Date syncDate) {

    DocumentSyncRecord r =
        new DocumentSyncRecord().setSyncDate(new Date()).setInsertedDate(new Date());
    Map<String, AttributeValue> attrs = r.getDataAttributes();

    Map<String, AttributeValue> updateValues =
        Map.of("status", AttributeValue.fromS(status.name()), "syncDate", attrs.get("syncDate"));
    this.db.updateValues(AttributeValue.fromS(pk), AttributeValue.fromS(sk), updateValues);
  }

  @Override
  public void deleteAll(final String siteId, final String documentId) {

    final int limit = 100;
    String pk = getPk(siteId, documentId);
    QueryConfig config = new QueryConfig().projectionExpression("PK,SK");

    Map<String, AttributeValue> startkey = null;

    do {

      QueryResponse response = this.db.queryBeginsWith(config, AttributeValue.fromS(pk),
          AttributeValue.fromS(SK_SYNCS), startkey, limit);

      List<Map<String, AttributeValue>> results = response.items();
      for (Map<String, AttributeValue> map : results) {
        this.db.deleteItem(map.get(PK), map.get(SK));
      }

      startkey = response.lastEvaluatedKey();

    } while (startkey != null && !startkey.isEmpty());

  }
}
