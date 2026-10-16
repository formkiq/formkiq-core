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
package com.formkiq.aws.dynamodb.documentattributes;

import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Loads all pages of document attribute records matching an attribute key's sort-key prefix.
 * Results retain DynamoDB's query order and are scoped to the supplied document or artifact and
 * site. The query methods use {@code limit} as the page size for each request, without capping the
 * total number of returned records. A supplied continuation token selects the starting position;
 * the completed result has no continuation key.
 */
public class QueryDocumentAttributesByKey implements DynamoDbQuery {

  /** Document Id. */
  private final DocumentArtifact document;
  /** Attribute Key. */
  private final String key;

  /**
   * Creates a query for a document or artifact and an attribute key.
   * 
   * @param documentArtifact {@link DocumentArtifact}
   * @param attributeKey {@link String}
   */
  public QueryDocumentAttributesByKey(final DocumentArtifact documentArtifact,
      final String attributeKey) {
    this.document = documentArtifact;
    this.key = attributeKey;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {
    DocumentAttributeRecord r = new DocumentAttributeRecord().setKey(key).setDocument(document)
        .setValueType(DocumentAttributeValueType.KEY_ONLY);
    String pk = r.pk(siteId);
    String sk = r.sk();
    return DynamoDbQueryBuilder.builder().pk(pk).beginsWith(sk).nextToken(nextToken).limit(limit)
        .build(tableName);
  }

  /**
   * Loads all remaining matching attribute records from the specified table, following continuation
   * keys until DynamoDB reports no more pages. Database failures propagate to the caller instead of
   * returning partial results.
   *
   * @param db document database
   * @param tableName table queried on every page
   * @param siteId document site, or null for the default site
   * @param nextToken continuation token for the starting position, or null to start at the
   *        beginning
   * @param limit page size for each request, not a limit on the total number of returned records
   * @return all remaining matching records with an empty continuation key
   */
  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {
    List<Map<String, AttributeValue>> items = new ArrayList<>();
    String token = nextToken;
    do {
      QueryResult page = DynamoDbQuery.super.query(db, tableName, siteId, token, limit);
      items.addAll(page.items());
      token = page.toNextToken();
    } while (token != null);
    return new QueryResult(items, Map.of());
  }
}
