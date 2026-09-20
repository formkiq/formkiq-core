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
import com.formkiq.aws.dynamodb.documents.DocumentRecord;
import com.formkiq.aws.dynamodb.folders.GetFolderFilesByNameQuery;
import com.formkiq.aws.dynamodb.model.DocumentSearchFolder;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.List;

/** Executes document searches using {@link DocumentSearchFolder}. */
public final class DocumentSearchFolderQuery implements DocumentSearchQuery {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;
  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dbClient;

  /**
   * Constructor.
   *
   * @param dbService {@link DynamoDbService}
   * @param dynamoDbClient {@link DynamoDbClient}
   */
  public DocumentSearchFolderQuery(final DynamoDbService dbService,
      final DynamoDbClient dynamoDbClient) {
    this.db = dbService;
    this.dbClient = dynamoDbClient;
  }

  /**
   * Count folders matching folder criteria.
   *
   * @param siteId site identifier
   * @param query {@link SearchQuery}
   * @param maxResults maximum number of results to count
   * @return {@link SearchCountResult}
   */
  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {

    DocumentSearchFolder folder = query.folder();
    List<QueryRequest> requests = new GetFolderFilesByNameQuery(true, folder.beginsWith())
        .build(this.db.getTableName(), siteId, null, maxResults);
    return DocumentSearchQuery.countQueries(this.dbClient::query, requests, maxResults);
  }

  /**
   * Search for folders matching folder criteria.
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

    DocumentSearchFolder folder = query.folder();
    QueryResult result = new GetFolderFilesByNameQuery(true, folder.beginsWith()).query(this.db,
        this.db.getTableName(), siteId, nextToken, limit);

    var documentRecords = result.items().stream().map(DocumentRecord::fromAttributeMap).toList();
    var seachResults = documentRecords.stream()
        .map(a -> new DocumentSearchResult(a, new DocumentSearchFolderIndex(true, null))).toList();

    return new Pagination<>(seachResults, result.lastEvaluatedKey());
  }
}
