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
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchMetaCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.folders.FindFolderParentByPath;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderPermissionAttributePredicate;
import com.formkiq.stacks.dynamodb.folders.FolderPermissionPathPredicate;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.DbKeys.GLOBAL_FOLDER_METADATA;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/** Executes document searches using {@link SearchMetaCriteria}. */
public final class DocumentSearchMetaQuery implements DocumentSearchQuery {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private final DocumentService documentService;
  /** {@link FolderIndexProcessor}. */
  private final FolderIndexProcessor folderIndexProcessor;

  /**
   * Constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbClient {@link DynamoDbClient}
   * @param documents {@link DocumentService}
   * @param folderIndex {@link FolderIndexProcessor}
   */
  public DocumentSearchMetaQuery(final DynamoDbService dbService,
      final DynamoDbClient dynamoDbClient, final DocumentService documents,
      final FolderIndexProcessor folderIndex) {
    this.db = dbService;
    this.dbClient = dynamoDbClient;
    this.documentService = documents;
    this.folderIndexProcessor = folderIndex;
  }

  /**
   * Count documents matching metadata criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   */
  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {

    SearchMetaCriteria meta = query.meta();

    if (meta.path() != null) {
      return countByPath(siteId, meta.path());
    }

    return countByMeta(siteId, meta, maxResults);
  }

  private SearchCountResult countByMeta(final String siteId, final SearchMetaCriteria meta,
      final int maxResults) {

    SearchMetaCriteria normalizedMeta = normalize(meta);
    String path = normalizedMeta.eq();
    boolean hasAccess =
        new FolderPermissionPathPredicate(this.db, ApiPermission.READ).apply(siteId, path);

    if (!hasAccess) {
      return new SearchCountResult(0, false);
    }

    String value = getMetadataKey(siteId, normalizedMeta);
    if (value == null) {
      return new SearchCountResult(0, false);
    }

    QueryRequest request = createQueryRequest(createDatabaseKey(siteId, value),
        normalizedMeta.indexFilterBeginsWith(), null, maxResults, "documentId,#type").toBuilder()
        .expressionAttributeNames(Map.of("#type", "type")).build();
    FolderPermissionAttributePredicate permissionPredicate =
        new FolderPermissionAttributePredicate(this.db, ApiPermission.READ, path);
    Map<String, AttributeValue> startKey = null;
    int count = 0;

    do {
      int remaining = maxResults - count;
      request = request.toBuilder().limit(remaining).exclusiveStartKey(startKey).build();
      QueryResponse response = this.dbClient.query(request);
      List<Map<String, AttributeValue>> items = permissionPredicate.apply(siteId, response.items());

      if ("folder".equals(normalizedMeta.indexType())) {
        items = items.stream().filter(i -> i.containsKey("documentId")).toList();
      }

      count += items.size();
      startKey = response.lastEvaluatedKey();

      if (count >= maxResults) {
        return new SearchCountResult(maxResults, startKey != null && !startKey.isEmpty());
      }
    } while (startKey != null && !startKey.isEmpty());

    return new SearchCountResult(count, false);
  }

  private SearchCountResult countByPath(final String siteId, final String path) {
    if (!hasPathAccess(siteId, path)) {
      return new SearchCountResult(0, false);
    }

    try {
      Map<String, Object> map = this.folderIndexProcessor.getIndex(siteId, path);
      String documentId = (String) map.get("documentId");
      return documentId != null ? countExistingDocument(siteId, documentId)
          : new SearchCountResult(0, false);
    } catch (IOException e) {
      return new SearchCountResult(0, false);
    }
  }

  private SearchCountResult countExistingDocument(final String siteId, final String documentId) {
    List<Map<String, AttributeValue>> keys =
        List.of(new DocumentRecordBuilder().documentId(documentId).buildKey(siteId).toMap());
    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,documentId");
    return new SearchCountResult(this.db.getBatch(config, keys).size(), false);
  }

  private QueryRequest createQueryRequest(final String value, final String beginsWith,
      final String nextToken, final int limit, final String projectionExpression) {

    String expression = PK + " = :pk";
    Map<String, AttributeValue> values = new HashMap<>();
    values.put(":pk", AttributeValue.fromS(value));

    if (beginsWith != null) {
      expression += " and begins_with(" + SK + ", :sk)";
      values.put(":sk", AttributeValue.fromS(beginsWith));
    }

    Map<String, AttributeValue> startKey = new StringToMapAttributeValue().apply(nextToken);
    return QueryRequest.builder().tableName(this.db.getTableName())
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .projectionExpression(projectionExpression).exclusiveStartKey(startKey)
        .scanIndexForward(Boolean.TRUE).limit(limit).build();
  }

  private String getFolderMetadataKey(final String siteId, final SearchMetaCriteria meta) {
    String eq = meta.eq();
    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;

    if (!StringUtils.isBlank(eq)) {
      try {
        Map<String, Object> map = this.folderIndexProcessor.getIndex(siteId, eq + "/");
        if (map.containsKey("documentId")) {
          value += map.get("documentId");
        }
      } catch (IOException e) {
        value = null;
      }
    }

    return value;
  }

  private String getMetadataKey(final String siteId, final SearchMetaCriteria meta) {
    return "folder".equals(meta.indexType()) ? getFolderMetadataKey(siteId, meta)
        : DbKeys.GLOBAL_FOLDER_TAGS;
  }

  private boolean hasPathAccess(final String siteId, final String path) {
    String parentPath = new FindFolderParentByPath().apply(path);
    return new FolderPermissionPathPredicate(this.db, ApiPermission.READ).apply(siteId, parentPath);
  }

  private SearchMetaCriteria normalize(final SearchMetaCriteria meta) {
    String folder = meta.folder();

    if (folder != null) {
      if (folder.endsWith("/")) {
        folder = folder.substring(0, folder.length() - 1);
      }

      return new SearchMetaCriteria(folder, null, meta.indexFilterBeginsWith(), "folder",
          meta.path());
    }

    return meta;
  }

  /**
   * Search for documents matching metadata criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param nextToken pagination token
   * @param limit maximum number of results
   * @return {@link Pagination} of {@link DocumentSearchResult}
   */
  @Override
  public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final String nextToken, final int limit) {

    SearchMetaCriteria meta = query.meta();

    if (meta.path() != null) {
      return queryByPath(siteId, meta.path());
    }

    SearchMetaCriteria normalizedMeta = normalize(meta);
    String path = normalizedMeta.eq();
    Pagination<DocumentSearchResult> results =
        queryByMetadataKey(siteId, getMetadataKey(siteId, normalizedMeta),
            normalizedMeta.indexFilterBeginsWith(), nextToken, limit, path);

    if ("folder".equals(normalizedMeta.indexType())) {
      results.getResults().removeIf(r -> r.documentRecord() == null);
    }

    return results;
  }

  Pagination<DocumentSearchResult> queryByMetadataKey(final String siteId, final String metadataKey,
      final String beginsWith, final String nextToken, final int limit, final String path) {

    boolean hasAccess =
        new FolderPermissionPathPredicate(this.db, ApiPermission.READ).apply(siteId, path);

    if (!hasAccess || metadataKey == null) {
      return new Pagination<>(Collections.emptyList());
    }

    QueryRequest request = createQueryRequest(createDatabaseKey(siteId, metadataKey), beginsWith,
        nextToken, limit, null);
    return queryDocuments(siteId, request, path);
  }

  private Pagination<DocumentSearchResult> queryByPath(final String siteId, final String path) {
    if (!hasPathAccess(siteId, path)) {
      return new Pagination<>(Collections.emptyList());
    }

    try {
      Map<String, Object> map = this.folderIndexProcessor.getIndex(siteId, path);
      String documentId = (String) map.get("documentId");
      DocumentRecord item = documentId != null
          ? this.documentService.findDocument(siteId, DocumentArtifact.of(documentId, null))
          : null;
      if (item == null) {
        return new Pagination<>(Collections.emptyList());
      }

      return new Pagination<>(Collections.singletonList(new DocumentSearchResult(item)));
    } catch (IOException e) {
      return new Pagination<>(Collections.emptyList());
    }
  }

  private Pagination<DocumentSearchResult> queryDocuments(final String siteId,
      final QueryRequest request, final String path) {

    QueryResponse response = this.dbClient.query(request);

    List<DocumentArtifact> documents =
        response.items().stream().filter(r -> r.containsKey("documentId"))
            .map(new AttributeValueToDocumentArtifact()).distinct().toList();

    List<DocumentRecord> items = this.documentService.findDocuments(siteId, documents);
    Map<String, DocumentRecord> documentMap = items != null
        ? items.stream().collect(Collectors.toMap(DocumentRecord::documentId, Function.identity()))
        : Collections.emptyMap();

    AttributeValueToGlobalMetaFolder metaFolder = new AttributeValueToGlobalMetaFolder();
    List<Map<String, AttributeValue>> permitted =
        new FolderPermissionAttributePredicate(this.db, ApiPermission.READ, path).apply(siteId,
            response.items());

    List<DocumentSearchResult> results = permitted.stream().map(item -> {
      AttributeValue documentId = item.get("documentId");
      boolean isDocument = documentId != null && documentMap.containsKey(documentId.s());
      return isDocument ? new DocumentSearchResult(documentMap.get(documentId.s()))
          : metaFolder.apply(item);
    }).filter(result -> result != null).collect(Collectors.toList());

    return new Pagination<>(results, response.lastEvaluatedKey());
  }
}
