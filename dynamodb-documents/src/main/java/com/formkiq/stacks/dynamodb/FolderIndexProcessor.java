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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * 
 * Generates Indexes Processor.
 *
 */
public interface FolderIndexProcessor {

  /** Deliminator. */
  String DELIMINATOR = "/";
  /** Index File SK. */
  String INDEX_FILE_SK = "fi" + DbKeys.TAG_DELIMINATOR;
  /** Index Folder SK. */
  String INDEX_FOLDER_SK = "ff" + DbKeys.TAG_DELIMINATOR;

  /**
   * Delete Empty Directory.
   * 
   * @param siteId {@link String}
   * @param parentId {@link String}
   * @param path {@link String}
   * @return boolean
   */
  boolean deleteEmptyDirectory(String siteId, String parentId, String path);

  /**
   * Delete Index Path.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param path {@link String}
   */
  void deletePath(String siteId, String documentId, String path);

  /**
   * Generates DynamoDB {@link WriteRequest} for Index.
   * 
   * @param siteId {@link String}
   * @param item {@link DocumentItem}
   * @return {@link List} {@link Map} {@link AttributeValue}
   */
  List<Map<String, AttributeValue>> generateIndex(String siteId, DocumentItem item);

  /**
   * Generates DynamoDB {@link WriteRequest} for Index.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @return {@link List} {@link Map} {@link String}
   * @throws IOException IOException
   */
  Map<String, String> getIndex(String siteId, String path) throws IOException;

  /**
   * Get Folder / File Index.
   * 
   * @param siteId {@link String}
   * @param indexKey {@link String}
   * @param isFile boolean
   * @return {@link DynamicObject}
   */
  DynamicObject getIndex(String siteId, String indexKey, boolean isFile);

  /**
   * Is Folder in Path.
   * 
   * @param siteId {@link String}
   * @param path {@link String}
   * @param folderId {@link String}
   * @return boolean
   * @throws IOException IOException
   */
  boolean isFolderIdInPath(String siteId, String path, String folderId) throws IOException;

  /**
   * Move Index from one to another.
   * 
   * @param siteId {@link String}
   * @param sourcePath {@link String}
   * @param targetPath {@link String}
   * @param userId {@link String}
   * @throws IOException IOException
   */
  void moveIndex(String siteId, String sourcePath, String targetPath, String userId)
      throws IOException;
}
