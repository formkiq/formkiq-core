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
package com.formkiq.stacks.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;

/**
 * 
 * {@link IndexProcessor} for generating Folder structure.
 *
 */
public class FolderIndexProcessor implements IndexProcessor, DbKeys {

  @Override
  public List<Map<String, AttributeValue>> generateIndex(final String siteId,
      final DocumentItem item) {

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    if (!StringUtils.isEmpty(item.getPath())) {

      String[] folders = tokens(item.getPath());
      int len = folders.length;

      int i = 0;

      StringBuilder sb = new StringBuilder();

      for (String folder : folders) {

        final String pk = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR + sb.toString();
        String sk = folder;
        String documentId = null;

        if (i > 0) {
          sb.append("/");
        }

        sb.append(folder);

        if (i >= len - 1) {
          documentId = item.getDocumentId();
          sk = folder + TAG_DELIMINATOR + documentId;
        }

        String path = sb.toString();
        Map<String, AttributeValue> values = keysGeneric(siteId, pk, sk);
        values.put("path", AttributeValue.fromS(path));

        if (documentId != null) {
          values.put("documentId", AttributeValue.fromS(documentId));
        }

        list.add(values);

        i++;
      }
    }

    return list;
  }

  private String[] tokens(final String s) {
    String split = "/";
    String ss = s.startsWith(split) ? s.substring(split.length()) : s;
    ss = ss.replaceAll(":://", split);
    return ss.split(split);
  }
}
