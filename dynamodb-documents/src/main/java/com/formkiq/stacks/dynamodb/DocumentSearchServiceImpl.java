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

import static com.formkiq.stacks.dynamodb.DbKeys.GSI1;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_PK;
import static com.formkiq.stacks.dynamodb.DbKeys.GSI2_SK;
import static com.formkiq.stacks.dynamodb.DbKeys.TAG_DELIMINATOR;
import static com.formkiq.stacks.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * 
 * Implementation {@link DocumentSearchService}.
 *
 */
public class DocumentSearchServiceImpl implements DocumentSearchService {

  /** {@link DocumentService}. */
  private DocumentService docService;

  /** Documents Table Name. */
  private String documentTableName;

  /** {@link DynamoDbClient}. */
  private final DynamoDbClient dynamoDB;

  /**
   * constructor.
   * 
   * @param documentService {@link DocumentService}
   * @param builder {@link DynamoDbConnectionBuilder}
   * @param documentsTable {@link String}
   */
  public DocumentSearchServiceImpl(final DocumentService documentService,
      final DynamoDbConnectionBuilder builder, final String documentsTable) {
    this.docService = documentService;

    if (documentsTable == null) {
      throw new IllegalArgumentException("Table name is null");
    }

    this.dynamoDB = builder.build();
    this.documentTableName = documentsTable;
  }

  @Override
  public PaginationResults<DynamicDocumentItem> search(final String siteId,
      final SearchTagCriteria search, final PaginationMapToken token, final int maxresults) {

    search.isValid();

    PaginationResults<DynamicDocumentItem> result = null;
    String key = search.getKey();

    if (search.getEq() != null) {
      result = findDocumentsWithTagAndValue(siteId, key, search.getEq(), token, maxresults);
    } else if (search.getBeginsWith() != null) {
      result = findDocumentsTagStartWith(siteId, key, search.getBeginsWith(), token, maxresults);
    } else {
      result = findDocumentsWithTag(siteId, key, null, token, maxresults);
    }

    return result;
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB siteId.
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsTagStartWith(final String siteId,
      final String key, final String value, final PaginationMapToken token, final int maxresults) {

    String expression = GSI2_PK + " = :pk and begins_with(" + GSI2_SK + ", :sk)";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, key)).build());
    values.put(":sk", AttributeValue.builder().s(value).build());

    return searchForDocuments(siteId, GSI2, expression, values, token, maxresults);
  }

  /**
   * Find Document that match tagKey.
   *
   * @param siteId DynamoDB siteId Key
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTag(final String siteId,
      final String key, final String value, final PaginationMapToken token, final int maxresults) {

    String expression = GSI2_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder().s(createDatabaseKey(siteId, key)).build());

    return searchForDocuments(siteId, GSI2, expression, values, token, maxresults);
  }

  /**
   * Find Document that match tagKey & tagValue.
   *
   * @param siteId DynamoDB PK siteId
   * @param key {@link String}
   * @param value {@link String}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults}
   */
  private PaginationResults<DynamicDocumentItem> findDocumentsWithTagAndValue(final String siteId,
      final String key, final String value, final PaginationMapToken token, final int maxresults) {

    String expression = GSI1_PK + " = :pk";

    Map<String, AttributeValue> values = new HashMap<String, AttributeValue>();
    values.put(":pk", AttributeValue.builder()
        .s(createDatabaseKey(siteId, key + TAG_DELIMINATOR + value)).build());

    return searchForDocuments(siteId, GSI1, expression, values, token, maxresults);
  }

  /**
   * Search for Documents.
   *
   * @param siteId DynamoDB PK siteId
   * @param index {@link String}
   * @param expression {@link String}
   * @param values {@link Map} {@link String} {@link AttributeValue}
   * @param token {@link PaginationMapToken}
   * @param maxresults int
   * @return {@link PaginationResults} {@link DocumentItemSearchResult}
   */
  private PaginationResults<DynamicDocumentItem> searchForDocuments(final String siteId,
      final String index, final String expression, final Map<String, AttributeValue> values,
      final PaginationMapToken token, final int maxresults) {

    Map<String, AttributeValue> startkey = new PaginationToAttributeValue().apply(token);

    QueryRequest q = QueryRequest.builder().tableName(this.documentTableName).indexName(index)
        .keyConditionExpression(expression).expressionAttributeValues(values)
        .exclusiveStartKey(startkey).limit(Integer.valueOf(maxresults)).build();

    QueryResponse result = this.dynamoDB.query(q);

    Map<String, DocumentTag> tags = new HashMap<>();

    List<DocumentItem> list = result.items().stream().map(s -> {
      String documentId = s.get("documentId").s();

      String tagKey = s.containsKey("tagKey") ? s.get("tagKey").s() : null;
      String tagValue = s.containsKey("tagValue") ? s.get("tagValue").s() : null;
      tags.put(documentId, new DocumentTag(null, tagKey, tagValue, null, null));

      return new DocumentItemDynamoDb(documentId, null, null);
    }).collect(Collectors.toList());

    if (!list.isEmpty()) {
      List<String> documentIds =
          list.stream().map(s -> s.getDocumentId()).collect(Collectors.toList());

      list = this.docService.findDocuments(siteId, documentIds);
    }

    List<DynamicDocumentItem> results = list.stream()
        .map(l -> new DocumentItemToDynamicDocumentItem().apply(l)).collect(Collectors.toList());

    results.forEach(r -> {
      DocumentTag tag = tags.get(r.getDocumentId());
      r.put("matchedTag", new DocumentTagToDynamicDocumentTag().apply(tag));
    });

    return new PaginationResults<>(results, new QueryResponseToPagination().apply(result));
  }
}
