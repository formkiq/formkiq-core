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

import com.formkiq.aws.dynamodb.DynamoDbFind;
import com.formkiq.aws.dynamodb.DynamoDbService;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;

import static com.formkiq.strings.Strings.isEmpty;

/**
 * {@link DynamoDbFind} for finding a folder document id by path.
 */
public class FindFolderIdByPath implements DynamoDbFind<String, String> {

  @Override
  public String find(final DynamoDbService db, final String tableName, final String siteId,
      final String path) {

    String folderId = null;

    if (!isEmpty(path) && !"/".equals(path)) {

      var parentDocumentId = "";

      for (String folder : new StringToFolderTokens().apply(path)) {

        var folderIndexRecord = new FolderIndexRecord().parentDocumentId(parentDocumentId)
            .path(folder).type(FolderType.FOLDER.getValue());

        var attributes = db.get(folderIndexRecord.buildPrimaryKey(siteId));

        if (!attributes.containsKey("documentId")) {
          folderId = null;
          break;
        }

        folderId = DynamoDbTypes.toString(attributes.get("documentId"));
        parentDocumentId = folderId;
      }
    }

    return folderId;
  }
}
