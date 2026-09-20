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
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbServiceImpl;
import com.formkiq.aws.dynamodb.QueryConfig;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.aws.dynamodb.model.SearchResponseFields;
import com.formkiq.aws.dynamodb.model.SearchTagCriteria;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeRecord;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.stacks.dynamodb.attributes.AttributeServiceDynamodb;
import com.formkiq.stacks.dynamodb.attributes.DocumentAttributeRecordToMap;
import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeValueType;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessor;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorExtension;
import com.formkiq.stacks.dynamodb.folders.FolderIndexProcessorImpl;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;
import com.formkiq.stacks.dynamodb.schemas.SchemaServiceDynamodb;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.formkiq.aws.dynamodb.DbKeys.GLOBAL_FOLDER_METADATA;
import static com.formkiq.aws.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.stacks.dynamodb.attributes.AttributeRecord.ATTR;

/**
 * 
 * Implementation {@link DocumentSearchService}.
 *
 */
public final class DocumentSearchServiceImpl implements DocumentSearchService {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DocumentService}. */
  private final DocumentService docService;
  /** {@link DocumentSearchQuery}. */
  private final DocumentSearchQuery documentSearchFilenameQuery;
  /** {@link DocumentSearchQuery}. */
  private final DocumentSearchQuery documentSearchFolderQuery;
  /** {@link DocumentSearchMetaQuery}. */
  private final DocumentSearchMetaQuery documentSearchMetaQuery;
  /** {@link DocumentSearchTagQuery}. */
  private final DocumentSearchTagQuery documentSearchTagQuery;
  /** {@link FolderIndexProcessor}. */
  private final FolderIndexProcessor folderIndexProcesor;
  /** {@link DocumentSearchAttributeQuery}. */
  private final DocumentSearchAttributeQuery documentSearchAttributeQuery;

  /**
   * constructor.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param documentService {@link DocumentService}
   * @param documentsTable {@link String}
   */
  public DocumentSearchServiceImpl(final DynamoDbConnectionBuilder connection,
      final DocumentService documentService, final String documentsTable) {

    final var dbClient = connection.build();
    this.docService = documentService;

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.db = new DynamoDbServiceImpl(connection, documentsTable);
    this.folderIndexProcesor = new FolderIndexProcessorImpl(connection, documentsTable,
        FolderIndexProcessorExtension.DEFAULT_PARENT_LAST_MODIFIED_UPDATE_INTERVAL_IN_MS);
    this.documentSearchFilenameQuery =
        new DocumentSearchFilenameQuery(this.db, dbClient, this.docService);
    this.documentSearchFolderQuery = new DocumentSearchFolderQuery(this.db, dbClient);
    this.documentSearchMetaQuery =
        new DocumentSearchMetaQuery(this.db, dbClient, this.docService, this.folderIndexProcesor);
    this.documentSearchTagQuery = new DocumentSearchTagQuery(this.db, dbClient, this.docService);
    AttributeService attributeService = new AttributeServiceDynamodb(this.db);
    SchemaService schemaService = new SchemaServiceDynamodb(this.db);
    this.documentSearchAttributeQuery = new DocumentSearchAttributeQuery(this.db, dbClient,
        this.docService, attributeService, schemaService);
  }

  private SearchCountResult countExistingDocuments(final String siteId,
      final Collection<String> documentIds, final int maxResults) {

    List<Map<String, AttributeValue>> keys = documentIds.stream().limit(maxResults)
        .map(id -> new DocumentRecordBuilder().documentId(id).buildKey(siteId).toMap()).toList();

    BatchGetConfig config = new BatchGetConfig().projectionExpression("PK,SK,documentId");
    int count = keys.isEmpty() ? 0 : this.db.getBatch(config, keys).size();
    return new SearchCountResult(count, documentIds.size() > maxResults);
  }

  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query, final int maxResults)
      throws ValidationException {

    DocumentSearchQuery documentSearchQuery = getDocumentSearchQuery(query);
    return documentSearchQuery != null ? documentSearchQuery.count(siteId, query, maxResults)
        : countExistingDocuments(siteId, query.documentIds(), maxResults);
  }

  private DocumentSearchQuery getDocumentSearchQuery(final SearchQuery query) {
    DocumentSearchQuery documentSearchQuery = null;

    if (query.meta() != null) {
      documentSearchQuery = this.documentSearchMetaQuery;
    } else if (query.filename() != null) {
      documentSearchQuery = this.documentSearchFilenameQuery;
    } else if (query.folder() != null) {
      documentSearchQuery = this.documentSearchFolderQuery;
    } else if (query.attribute() != null || !notNull(query.attributes()).isEmpty()) {
      documentSearchQuery = this.documentSearchAttributeQuery;
    } else if (query.tag() != null) {
      documentSearchQuery = this.documentSearchTagQuery;
    }

    return documentSearchQuery;
  }

  /**
   * Add Response Fields to {@link DocumentSearchResult}.
   *
   * @param siteId {@link String}
   * @param searchResults {@link List} {@link DocumentSearchResult}
   * @param searchResponseFields {@link SearchResponseFields}
   * @return {@link List} {@link DocumentSearchResult}
   */
  private List<DocumentSearchResult> addResponseFields(final String siteId,
      final List<DocumentSearchResult> searchResults,
      final SearchResponseFields searchResponseFields) {

    final int limit = 1000;
    List<DocumentSearchResult> results = searchResults;

    if (searchResponseFields != null) {

      results = new ArrayList<>(searchResults.size());

      Set<String> keyNames = new HashSet<>(notNull(searchResponseFields.attributes()));

      for (DocumentSearchResult item : searchResults) {

        DocumentAttributeRecord sr =
            new DocumentAttributeRecord().setDocument(item.documentRecord().document());

        QueryConfig config = new QueryConfig().scanIndexForward(Boolean.TRUE)
            .projectionExpression(
                "#key,valueType,stringValue,numberValue,booleanValue,dateValue,documentId")
            .expressionAttributeNames(Map.of("#key", "key"));

        AttributeValue pk = sr.fromS(sr.pk(siteId));
        AttributeValue sk = sr.fromS(ATTR);
        QueryResponse response = this.db.queryBeginsWith(config, pk, sk, null, limit);

        List<DocumentAttributeRecord> records =
            notNull(response.items()).stream().filter(a -> keyNames.contains(a.get("key").s()))
                .map(a -> new DocumentAttributeRecord().getFromAttributes(siteId, a)).toList();

        Collection<Map<String, Object>> attributes =
            new DocumentAttributeRecordToMap(true).apply(siteId, records);

        Map<String, Object> attributeFields = new HashMap<>();

        attributes.forEach(a -> {
          if (a.containsKey("stringValue")) {
            a.put("stringValues", List.of(a.get("stringValue")));
            a.remove("stringValue");
          } else if (a.containsKey("numberValue")) {
            a.put("numberValues", List.of(a.get("numberValue")));
            a.remove("numberValue");
          } else if (a.containsKey("dateValue")) {
            a.put("dateValues", List.of(a.get("dateValue")));
            a.remove("dateValue");
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
            case DATE -> attributeFields.put((String) a.get("key"),
                Map.of("valueType", a.get("valueType"), "dateValues", a.get("dateValues")));
            case STRING, COMPOSITE_STRING, RELATIONSHIPS, CLASSIFICATION, PUBLICATION ->
              attributeFields.put((String) a.get("key"),
                  Map.of("stringValues", a.get("stringValues"), "valueType", a.get("valueType")));
            default ->
              attributeFields.put((String) a.get("key"), Map.of("valueType", a.get("valueType")));
          }
        });

        results.add(new DocumentSearchResult(item, attributeFields));
      }
    }

    return results;
  }

  @Override
  public Pagination<DocumentSearchResult> findInFolder(final String siteId, final String indexKey,
      final String nextToken, final int maxresults) {

    String path = this.folderIndexProcesor.toPath(siteId, indexKey);
    DynamicObject o = this.folderIndexProcesor.getIndex(siteId, indexKey, false);

    String value = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR;
    if (o != null) {
      value += o.getString("documentId");
    }

    return this.documentSearchMetaQuery.queryByMetadataKey(siteId, value, null, nextToken,
        maxresults, path);
  }

  @Override
  public Pagination<DocumentSearchResult> search(final String siteId, final SearchQuery query,
      final SearchResponseFields searchResponseFields, final String nextToken, final int limit)
      throws ValidationException {

    DocumentSearchQuery documentSearchQuery = getDocumentSearchQuery(query);
    Pagination<DocumentSearchResult> results =
        documentSearchQuery != null ? documentSearchQuery.query(siteId, query, nextToken, limit)
            : searchByDocumentIds(siteId, query.documentIds());

    var searchResultsWithFields =
        addResponseFields(siteId, results.getResults(), searchResponseFields);
    return new Pagination<>(searchResultsWithFields, results.getNextToken());
  }

  private Pagination<DocumentSearchResult> searchByDocumentIds(final String siteId,
      final Collection<String> documentIds) {

    List<DocumentArtifact> documents =
        documentIds.stream().map(d -> DocumentArtifact.of(d, null)).toList();
    List<DocumentRecord> list = this.docService.findDocuments(siteId, documents);

    return new Pagination<>(list.stream().map(DocumentSearchResult::new).toList());
  }

  @Override
  public Pagination<String> searchForDocumentIds(final String siteId,
      final SearchTagCriteria criteria, final String nextToken, final int maxresults) {
    return this.documentSearchTagQuery.queryDocumentIds(siteId, criteria, nextToken, maxresults);
  }

}
