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
package com.formkiq.aws.dynamodb.actions;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;


import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * Find Document by action by status.
 */
public class FindDocumentActionByStatus implements DynamoDbQuery, DbKeys {

  /** Action Status. */
  private final String status;
  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;

  /**
   * constructor.
   * 
   * @param documentArtifact {@link DocumentArtifact}
   * @param actionStatus {@link String}
   */
  public FindDocumentActionByStatus(final DocumentArtifact documentArtifact,
      final String actionStatus) {
    this.document = documentArtifact;
    this.status = actionStatus;
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {
    String pk = createDatabaseKey(siteId, "actions#" + this.status + "#");
    String sk = !isEmpty(document.artifactId())
        ? "action_art#" + this.document.documentId() + "#" + this.document.artifactId()
        : "action#" + this.document.documentId();
    return DynamoDbQueryBuilder.builder().indexName(GSI2).pk(pk).eq(sk).build(tableName);
  }
}
