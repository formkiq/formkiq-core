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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

  /** Deliminator. */
  private static final String DELIMINATOR = "/";
  /** {@link SimpleDateFormat} in ISO Standard format. */
  private SimpleDateFormat df = DateUtil.getIsoDateFormatter();

  /**
   * Generate PKs from tokens.
   * 
   * @param tokens {@link String}
   * @return {@link List} {@link String}
   */
  public static List<String> generatePks(final String[] tokens) {

    int i = 0;
    StringBuilder sb = new StringBuilder();
    List<String> pks = new ArrayList<>(tokens.length);

    for (String folder : tokens) {
      String pk = GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR + sb.toString();
      pks.add(pk);

      if (i > 0) {
        sb.append("/");
      }

      sb.append(folder);

      i++;
    }

    return pks;
  }

  /**
   * Generate SKs from tokens.
   * 
   * @param tokens {@link String}
   * @param documentId {@link String}
   * @param path {@link String}
   * @return {@link List} {@link String}
   */
  public static List<String> generateSks(final String[] tokens, final String documentId,
      final String path) {

    int i = 0;
    int len = tokens.length;
    List<String> sks = new ArrayList<>(tokens.length);

    for (String folder : tokens) {

      if (isDocumentToken(path, i, len)) {
        sks.add(folder + TAG_DELIMINATOR + documentId);
      } else {
        sks.add(folder);
      }

      i++;
    }

    return sks;
  }

  private static boolean isDocumentToken(final String path, final int i, final int len) {
    return i >= len - 1 && !path.endsWith("/");
  }

  /**
   * Generate Path Tokens.
   * 
   * @param path {@link String}
   * @return {@link String}
   */
  public static String[] tokens(final String path) {
    String ss = path.startsWith(DELIMINATOR) ? path.substring(DELIMINATOR.length()) : path;
    ss = ss.replaceAll(":://", DELIMINATOR);
    return ss.split(DELIMINATOR);
  }

  @Override
  public List<Map<String, AttributeValue>> generateIndex(final String siteId,
      final DocumentItem item) {

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    String path = item.getPath();
    if (!StringUtils.isEmpty(path)) {

      String[] folders = tokens(path);
      List<String> pks = generatePks(folders);
      List<String> sks = generateSks(folders, item.getDocumentId(), path);
      int len = folders.length;

      int i = 0;

      StringBuilder sb = new StringBuilder();

      for (String folder : folders) {

        String documentId = null;

        if (i > 0) {
          sb.append(DELIMINATOR);
        }

        sb.append(folder);

        if (isDocumentToken(path, i, len)) {
          documentId = item.getDocumentId();
        }

        String pk = pks.get(i);
        String sk = sks.get(i);

        Map<String, AttributeValue> values = keysGeneric(siteId, pk, sk);
        values.put("path", AttributeValue.fromS(sb.toString()));

        if (documentId != null) {
          values.put("documentId", AttributeValue.fromS(documentId));
        } else {
          Date insertedDate = new Date();
          String fullInsertedDate = this.df.format(insertedDate);
          addS(values, "inserteddate", fullInsertedDate);
          addS(values, "lastModifiedDate", fullInsertedDate);
          addS(values, "userId", item.getUserId());
        }

        list.add(values);

        i++;
      }
    }

    return list;
  }
}
