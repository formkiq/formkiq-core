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

import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.documents.AttributeValueToDocumentArtifact;
import com.formkiq.aws.dynamodb.folders.GetFolderFilesByNameQuery;
import com.formkiq.aws.dynamodb.model.DocumentSearchFilename;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.List;

/** Executes document searches using {@link DocumentSearchFilename}. */
public final class DocumentSearchFilenameQuery implements DocumentSearchQuery {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;
  /** {@link DocumentService}. */
  private final DocumentService documentService;

  /**
   * Constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbClient {@link DynamoDbClient}
   * @param documents {@link DocumentService}
   */
  public DocumentSearchFilenameQuery(final DynamoDbService dbService,
      final DynamoDbClient dynamoDbClient, final DocumentService documents) {
    this.db = dbService;
    this.dbClient = dynamoDbClient;
    this.documentService = documents;
  }

  /**
   * Count documents matching filename criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   */
  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {

    DocumentSearchFilename filename = query.filename();
    List<QueryRequest> requests = new GetFolderFilesByNameQuery(false, filename.beginsWith())
        .build(this.db.getTableName(), siteId, null, maxResults);
    return DocumentSearchQuery.countQueries(this.dbClient::query, requests, maxResults);
  }

  /**
   * Search for documents matching filename criteria.
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

    DocumentSearchFilename filename = query.filename();
    QueryResult result = new GetFolderFilesByNameQuery(false, filename.beginsWith()).query(this.db,
        siteId, nextToken, limit);
    var documentIds = result.items().stream().map(new AttributeValueToDocumentArtifact())
        .filter(java.util.Objects::nonNull).toList();

    var documentRecords = this.documentService.findDocuments(siteId, documentIds);
    var searchResults = documentRecords.stream().map(DocumentSearchResult::new).toList();

    return new Pagination<>(searchResults, result.lastEvaluatedKey());
  }
}
