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
import com.formkiq.aws.dynamodb.objects.DateUtil;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.formkiq.aws.dynamodb.DbKeys.GSI2;
import static com.formkiq.aws.dynamodb.documents.DocumentDeleteMoveAttributeFunction.SOFT_DELETE;

/**
 * {@link DynamoDbQuery} for finding soft-deleted documents by deleted date.
 */
public class GetSoftDeletedDocumentsQuery implements DynamoDbQuery {

  /** Sort key prefix. */
  private static final String DATE_PREFIX = "date#";
  /** Sort key minimum. */
  private static final String DATE_MIN = DATE_PREFIX;
  /** Sort key maximum. */
  private static final String DATE_MAX = DATE_PREFIX + "~";

  /** Range start. */
  private final Date start;
  /** Range end. */
  private final Date end;
  /** Sort direction. */
  private final boolean scanIndexForward;

  /**
   * constructor.
   */
  public GetSoftDeletedDocumentsQuery() {
    this(null, null, false);
  }

  /**
   * constructor.
   *
   * @param startDate {@link Date}
   * @param endDate {@link Date}
   * @param scanForward boolean
   */
  public GetSoftDeletedDocumentsQuery(final Date startDate, final Date endDate,
      final boolean scanForward) {
    this.start = startDate != null ? new Date(startDate.getTime()) : null;
    this.end = endDate != null ? new Date(endDate.getTime()) : null;
    this.scanIndexForward = scanForward;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    String pk = SiteIdKeyGenerator.createDatabaseKey(siteId, SOFT_DELETE + "docs#");
    DynamoDbQueryBuilder builder = DynamoDbQueryBuilder.builder().indexName(GSI2).pk(pk)
        .scanIndexForward(this.scanIndexForward).nextToken(nextToken).limit(limit);

    if (this.start != null || this.end != null) {
      String low = this.start != null ? toStartDateKey(this.start) : DATE_MIN;
      String high = this.end != null ? toEndDateKey(this.end) : DATE_MAX;
      builder.betweenSK(low, high);
    }

    return builder.build(tableName);
  }

  /**
   * Build a primary-key query for legacy soft-deleted documents without GSI2 keys.
   *
   * @param tableName {@link String}
   * @param siteId {@link String}
   * @param nextToken {@link String}
   * @param limit int
   * @return {@link QueryRequest}
   */
  public QueryRequest buildPrimaryKey(final String tableName, final String siteId,
      final String nextToken, final int limit) {

    String pk = SiteIdKeyGenerator.createDatabaseKey(siteId, SOFT_DELETE + "docs#");
    return DynamoDbQueryBuilder.builder().pk(pk).scanIndexForward(this.scanIndexForward)
        .nextToken(nextToken).limit(limit).build(tableName);
  }

  private String toEndDateKey(final Date date) {
    Instant instant = date.toInstant().plus(1, ChronoUnit.MILLIS).minusNanos(1);
    return DATE_PREFIX + DateUtil.getIso8601Formatter().format(instant);
  }

  private String toStartDateKey(final Date date) {
    Instant instant = date.toInstant().truncatedTo(ChronoUnit.MILLIS).minus(1, ChronoUnit.MILLIS);
    return DATE_PREFIX + DateUtil.getIso8601Formatter().format(instant);
  }
}
