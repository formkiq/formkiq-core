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
import com.formkiq.aws.dynamodb.base64.Pagination;
import com.formkiq.aws.dynamodb.model.SearchAttributeCriteria;
import com.formkiq.aws.dynamodb.model.SearchQuery;
import com.formkiq.stacks.dynamodb.attributes.AttributeService;
import com.formkiq.validation.ValidationException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/** Executes document searches using a single attribute criterion. */
public final class DocumentSearchAttributeQuery extends AbstractSearchAttributeQuery {

  /**
   * Constructor.
   *
   * @param dbService database service
   * @param dbClient database client
   * @param documents document service
   * @param attributes attribute definitions
   */
  public DocumentSearchAttributeQuery(final DynamoDbService dbService,
      final DynamoDbClient dbClient, final DocumentService documents,
      final AttributeService attributes) {
    super(dbService, dbClient, documents, attributes);
  }

  @Override
  public SearchCountResult count(final String siteId, final SearchQuery query,
      final int maxResults) {
    return countAttribute(siteId, resolveCriteria(siteId, query), query.documentIds(), maxResults);
  }

  @Override
  public Pagination<DocumentSearchResult> query(final String siteId, final SearchQuery query,
      final String nextToken, final int limit) {
    return queryAttribute(siteId, resolveCriteria(siteId, query), query.documentIds(), nextToken,
        limit);
  }

  private SearchAttributeCriteria resolveCriteria(final String siteId, final SearchQuery query) {
    List<SearchAttributeCriteria> criteria = normalizeCriteria(siteId, query);
    if (criteria.size() != 1) {
      throw ValidationException.builder().error("expected a single attribute in query").build();
    }
    SearchAttributeCriteria search = notNull(query.attributes()).isEmpty() ? criteria.getFirst()
        : new SearchAttributesToCriteria(null).apply(criteria);
    validate(search);
    return search;
  }
}
