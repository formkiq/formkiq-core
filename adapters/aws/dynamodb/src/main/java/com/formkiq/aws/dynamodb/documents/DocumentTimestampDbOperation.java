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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbClientOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * DynamoDb Operation to Save Document Timestamp.
 */
public class DocumentTimestampDbOperation implements DynamoDbClientOperation, DbKeys {

  /** {@link SimpleDateFormat} YYYY-mm-dd format. */
  private final SimpleDateFormat yyyymmddFormat;
  /** Last Short Date. */
  private String lastShortDate = null;

  /**
   * constructor.
   */
  public DocumentTimestampDbOperation() {
    this.yyyymmddFormat = new SimpleDateFormat("yyyy-MM-dd");
    TimeZone tz = TimeZone.getTimeZone("UTC");
    this.yyyymmddFormat.setTimeZone(tz);
  }

  @Override
  public void execute(final DynamoDbClient dbClient, final String tableName, final String siteId,
      final Object... params) {

    Date insertedDate = params.length > 0 ? (Date) params[0] : new Date();
    String shortdate = this.yyyymmddFormat.format(insertedDate);

    if (this.lastShortDate == null || !this.lastShortDate.equals(shortdate)) {

      this.lastShortDate = shortdate;

      Map<String, AttributeValue> values =
          Map.of(PK, AttributeValue.builder().s(PREFIX_DOCUMENT_DATE).build(), SK,
              AttributeValue.builder().s(shortdate).build());
      String conditionExpression = "attribute_not_exists(" + PK + ")";
      PutItemRequest put = PutItemRequest.builder().tableName(tableName)
          .conditionExpression(conditionExpression).item(values).build();

      try {
        dbClient.putItem(put);
      } catch (ConditionalCheckFailedException e) {
        e.printStackTrace();
        // Conditional Check Fails on second insert attempt
      }
    }
  }
}
