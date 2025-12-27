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

import com.formkiq.aws.dynamodb.AttributeValueComparator;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbQueryBuilder;
import com.formkiq.aws.dynamodb.DynamoDbShardKey;
import com.formkiq.aws.dynamodb.DynamoDbShardQuery;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.folders.FolderIndexRecord.INDEX_FILE_SK;
import static com.formkiq.aws.dynamodb.folders.FolderIndexRecord.INDEX_FOLDER_SK;

/**
 * {@link DynamoDbShardQuery} for finding filenames in folders.
 */
public class GetFolderFilesByName implements DynamoDbShardQuery {

  /** Begins With {@link String}. */
  private final String begins;
  /** Is Folder Search. */
  private final boolean folderSearch;

  /**
   * constructor.
   *
   * @param isFolderSearch Is folder search
   * @param beginsWith {@link String}
   */
  public GetFolderFilesByName(final boolean isFolderSearch, final String beginsWith) {
    this.begins = beginsWith;
    this.folderSearch = isFolderSearch;
  }

  @Override
  public List<QueryRequest> build(final String tableName, final String siteId,
      final String nextToken, final int limit) {

    List<QueryRequest> requests = new ArrayList<>();

    List<String> shards =
        new ArrayList<>(DynamoDbShardKey.getShardsSuffix(FolderIndexRecord.SHARD_COUNT));

    DynamoDbKey key = new FolderIndexRecord().parentDocumentId("").type("file").path(begins)
        .buildNonShardKey(siteId);
    List<DynamoDbShardKey> shardKeys = shards.stream().map(
        shard -> DynamoDbShardKey.builder().key(key).pkGsi1Shard(shard).pkGsi2Shard(shard).build())
        .toList();

    for (DynamoDbShardKey skey : shardKeys) {

      String s = folderSearch ? INDEX_FOLDER_SK + begins : INDEX_FILE_SK + begins;
      QueryRequest request =
          DynamoDbQueryBuilder.builder().indexName(DbKeys.GSI2, skey.key()).beginsWith(s)
              .scanIndexForward(true).nextToken(skey, nextToken).limit(limit).build(tableName);
      requests.add(request);
    }

    return requests;
  }

  @Override
  public Comparator<? super Map<String, AttributeValue>> getComparator(
      final boolean scanIndexForward) {
    return new AttributeValueComparator("path", scanIndexForward);
  }
}
