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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * {@link DynamoDbQuery} to find all Document Attribute Key.
 */
public class QueryDocumentAttributesByKey implements DynamoDbQuery {

  /** Document Id. */
  private final String document;
  /** Attribute Key. */
  private final String key;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param attributeKey {@link String}
   */
  public QueryDocumentAttributesByKey(final String documentId, final String attributeKey) {
    this.document = documentId;
    this.key = attributeKey;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {
    DocumentAttributeRecord r = new DocumentAttributeRecord().setKey(key).setDocumentId(document)
        .setValueType(DocumentAttributeValueType.KEY_ONLY);
    String pk = r.pk(siteId);
    String sk = r.sk();
    return DynamoDbQueryBuilder.builder().pk(pk).beginsWith(sk).nextToken(nextToken).limit(limit)
        .build(tableName);
  }
}
