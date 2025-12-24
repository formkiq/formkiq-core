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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.base64.MapAttributeValueToString;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.DbKeys.GSI1;
import static com.formkiq.aws.dynamodb.DbKeys.GSI1_PK;
import static com.formkiq.aws.dynamodb.DbKeys.PK;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_DATE;
import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_DATE_TS;
import static com.formkiq.aws.dynamodb.DbKeys.SK;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * {@link DynamoDbQuery} for finding All Documents.
 */
public class GetAllDocumentsQuery implements DynamoDbQuery {

  /** {@link GetDocumentDatesQuery}. */
  private final GetDocumentDatesQuery datesQuery;
  /** Fetch All Attributes. */
  private final boolean fetchAllAttributes;

  /**
   * constructor.
   */
  public GetAllDocumentsQuery() {
    this(null, false);
  }

  /**
   * constructor.
   *
   * @param date search for date
   * @param fetchAllDocumentAttributes boolean
   */
  public GetAllDocumentsQuery(final Date date, final boolean fetchAllDocumentAttributes) {
    datesQuery = new GetDocumentDatesQuery(date);
    this.fetchAllAttributes = fetchAllDocumentAttributes;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {
    throw new UnsupportedOperationException("Unsupported method");
  }

  private QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit, final String dateQuerySk) {
    String pk = SiteIdKeyGenerator.createDatabaseKey(siteId, "docts#" + dateQuerySk);
    return DynamoDbQueryBuilder.builder().scanIndexForward(Boolean.FALSE).indexName(GSI1).pk(pk)
        .projectionExpression("PK,SK,documentId").nextToken(nextToken).limit(limit)
        .build(tableName);
  }

  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {

    var next = nextToken;
    var queryDatesResult = queryDates(db, tableName, siteId, next);
    var iter = queryDatesResult.items().iterator();

    int limitCount = limit;
    List<Map<String, AttributeValue>> results = new ArrayList<>();
    Map<String, AttributeValue> lastEvaluatedKey = null;

    while (iter.hasNext() && limitCount > 0) {

      String sk = DynamoDbTypes.toString(iter.next().get(SK));

      QueryRequest queryRequest = build(tableName, siteId, next, limitCount, sk);
      QueryResponse response = db.query(queryRequest, fetchAllAttributes);

      results.addAll(response.items());
      lastEvaluatedKey = response.lastEvaluatedKey();

      limitCount -= response.items().size();
      next = null;
    }

    return new QueryResult(results, lastEvaluatedKey);
  }


  private QueryResult queryDates(final DynamoDbService db, final String tableName,
      final String siteId, final String nextToken) {

    String datesQueryNextToken = null;
    String nextTokenDate = null;

    if (!isEmpty(nextToken)) {
      var lastEvaluatedKey = new StringToMapAttributeValue().apply(nextToken);

      String pk = DynamoDbTypes.toString(lastEvaluatedKey.get(GSI1_PK));
      pk = SiteIdKeyGenerator.getDocumentId(pk);

      nextTokenDate = pk.substring(PREFIX_DOCUMENT_DATE_TS.length());
      var key = Map.of(PK, fromS(PREFIX_DOCUMENT_DATE), SK, fromS(nextTokenDate));
      datesQueryNextToken = new MapAttributeValueToString().apply(key);
    }

    final int datesLimit = 100;
    QueryResult result = datesQuery.query(db, tableName, siteId, datesQueryNextToken, datesLimit);
    List<Map<String, AttributeValue>> items = result.items();

    if (!isEmpty(nextToken)) {
      items = new ArrayList<>(items);
      items.add(0, Map.of(PK, fromS(PREFIX_DOCUMENT_DATE), SK, fromS(nextTokenDate)));
    }

    return new QueryResult(items, null);
  }
}
