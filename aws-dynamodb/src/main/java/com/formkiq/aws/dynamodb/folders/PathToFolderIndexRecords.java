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

import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.strings.Strings;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.formkiq.aws.dynamodb.objects.Objects.last;

/**
 * Convert {@link String} Path to {@link List} {@link FolderIndexRecord}.
 */
public class PathToFolderIndexRecords
    implements BiFunction<String, String, List<FolderIndexRecord>> {

  /** {@link DynamoDbService}. */
  private final DynamoDbService db;

  /**
   * constructor.
   * 
   * @param dbService {@link DynamoDbService}
   */
  public PathToFolderIndexRecords(final DynamoDbService dbService) {
    this.db = dbService;
  }

  @Override
  public List<FolderIndexRecord> apply(final String siteId, final String path) {

    if (Strings.isEmpty(path)) {
      return Collections.emptyList();
    }

    String[] tokens = new StringToFolderTokens().apply(path);
    String fileToken = getFileToken(path, tokens);
    String lastUuid = "";
    List<FolderIndexRecord> records = new ArrayList<>();

    for (String folder : tokens) {

      String pk = new FolderIndexRecord().parentDocumentId(lastUuid).pk(siteId);

      boolean isFile = folder.equals(fileToken);
      String sk = new FolderIndexRecord().path(folder).type(isFile ? "file" : "folder").sk();

      DynamoDbKey key = new DynamoDbKey(pk, sk, null, null, null, null);

      Map<String, AttributeValue> attr = this.db.get(key);
      if (!attr.isEmpty()) {
        FolderIndexRecord record = new FolderIndexRecord().getFromAttributes(siteId, attr);
        records.add(record);
        lastUuid = record.documentId();
      } else {
        throw new IllegalArgumentException("Cannot find folder '" + path + "'");
      }
    }

    return records;
  }

  private String getFileToken(final String path, final String[] tokens) {
    if (path.endsWith("/")) {
      return null;
    }

    return last(tokens);
  }
}
