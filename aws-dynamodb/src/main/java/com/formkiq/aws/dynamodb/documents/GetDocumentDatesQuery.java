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
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * {@link DynamoDbQuery} for finding possible dates documents were added.
 */
public class GetDocumentDatesQuery implements DynamoDbQuery {

  /** Less than Eq. */
  private final String lte;

  /**
   * constructor.
   *
   */
  public GetDocumentDatesQuery() {
    lte = null;
  }

  /**
   * constructor.
   * 
   * @param date {@link Date}
   *
   */
  public GetDocumentDatesQuery(final Date date) {
    lte = date != null
        ? DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(date.toInstant())
        : null;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    String pk = SiteIdKeyGenerator.createDatabaseKey(null, "docdate");
    DynamoDbQueryBuilder builder = DynamoDbQueryBuilder.builder().projectionExpression("PK, SK")
        .scanIndexForward(Boolean.FALSE).pk(pk).nextToken(nextToken).limit(limit);

    if (lte != null) {
      builder.lte(lte);
    }

    return builder.build(tableName);
  }
}
