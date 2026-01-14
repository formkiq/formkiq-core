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
package com.formkiq.aws.dynamodb.llm;

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import static com.formkiq.aws.dynamodb.llm.DocumentLlmMetadataExtractionRecord.KEY_SK_PREFIX;

/**
 * Query Document Llm Metadata Extraction.
 */
public class QueryDocumentLlmMetadataExtractions implements DynamoDbQuery {

  /** {@link String}. */
  private final String name;
  /** Document Id. */
  private final String docId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param llmPromptEntityName {@link String}
   */
  public QueryDocumentLlmMetadataExtractions(final String documentId,
      final String llmPromptEntityName) {
    this.docId = documentId;
    this.name = llmPromptEntityName;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    DynamoDbKey key = DocumentLlmMetadataExtractionRecord.builder().documentId(docId)
        .llmPromptEntityName("").buildKey(siteId);

    return DynamoDbQueryBuilder.builder().pk(key.pk()).scanIndexForward(false)
        .beginsWith(KEY_SK_PREFIX + this.name).nextToken(nextToken).limit(limit).build(tableName);
  }
}
