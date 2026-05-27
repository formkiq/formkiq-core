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
package com.formkiq.aws.dynamodb.folders;

import com.formkiq.aws.dynamodb.DynamoDbQuery;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.DynamoDbShardQuery;
import com.formkiq.aws.dynamodb.QueryResult;
import com.formkiq.aws.dynamodb.base64.StringToMapAttributeValue;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static com.formkiq.strings.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

/**
 * {@link DynamoDbShardQuery} for finding filenames in folders.
 */
public class GetAllFolderAndFilesQuery implements DynamoDbQuery {

  /** Document Parent Id. */
  private String documentParentId;
  /** {@link Deque} of unprocessed document parent ids. */
  private final Deque<String> documentParentIds = new ArrayDeque<>();

  /**
   * constructor.
   */
  public GetAllFolderAndFilesQuery() {
    documentParentId = "";
  }

  @Override
  public QueryRequest build(final String tableName, final String siteId, final String nextToken,
      final int limit) {

    String nt = nextToken;

    Map<String, AttributeValue> map = new StringToMapAttributeValue().apply(nt);
    if (map != null && map.containsKey("parentId")) {
      nt = null;
    }

    String pk = new FolderIndexRecord().parentDocumentId(documentParentId).pk(siteId);
    return DynamoDbQueryBuilder.builder().scanIndexForward(true).pk(pk).nextToken(nt).limit(limit)
        .build(tableName);
  }

  @Override
  public QueryResult query(final DynamoDbService db, final String tableName, final String siteId,
      final String nextToken, final int limit) {
    QueryResult result = DynamoDbQuery.super.query(db, tableName, siteId, nextToken, limit);

    result.items().forEach(item -> {
      String documentId = DynamoDbTypes.toString(item.get("documentId"));
      if (!isEmpty(documentId) && "folder".equals(DynamoDbTypes.toString(item.get("type")))) {
        documentParentIds.addLast(documentId);
      }
    });

    if (!result.hasLastEvaluatedKey() && !documentParentIds.isEmpty()) {
      documentParentId = documentParentIds.removeFirst();
      result = new QueryResult(result.items(), Map.of("parentId", fromS(documentParentId)));
    }

    return result;
  }
}
