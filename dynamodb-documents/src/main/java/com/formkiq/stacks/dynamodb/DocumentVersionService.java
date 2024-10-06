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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamodbVersionRecord;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * Interface for Document Versioning.
 *
 */
public interface DocumentVersionService {

  /** FormKiQ S3 Version. */
  String S3VERSION_ATTRIBUTE = "s3version";
  /** FormKiQ Version. */
  String VERSION_ATTRIBUTE = "version";

  /**
   * Delete all document versions.
   * 
   * @param client {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteAllVersionIds(DynamoDbClient client, String siteId, String documentId);

  /**
   * Get DynamoDB Documents Versions Table Name.
   * 
   * @return {@link String}
   */
  String getDocumentVersionsTableName();

  /**
   * Get S3 Version Id.
   * 
   * @param attributes {@link Map}
   * @return {@link String}
   */
  String getVersionId(Map<String, AttributeValue> attributes);

  /**
   * Get Version Record.
   * 
   * @param connection {@link DynamoDbConnectionBuilder}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param versionKey {@link String}
   * @return Map
   */
  Map<String, AttributeValue> get(DynamoDbConnectionBuilder connection, String siteId,
      String documentId, String versionKey);

  /**
   * Initialize Service.
   * 
   * @param map {@link Map}
   */
  void initialize(Map<String, String> map);

  /**
   * Revert to previous Document Version.
   * 
   * @param previous {@link Map}
   * @param current {@link Map}
   */
  void revertDocumentVersionAttributes(Map<String, AttributeValue> previous,
      Map<String, AttributeValue> current);

  /**
   * Add Versioning {@link DynamodbVersionRecord} records.
   *
   * @param client {@link DynamoDbClient}
   * @param siteId {@link String}
   * @param records {@link Collection} {@link DynamodbVersionRecord}
   * @return List
   */
  List<Map<String, AttributeValue>> addRecords(DynamoDbClient client, String siteId,
      Collection<? extends DynamodbVersionRecord<?>> records);

  /**
   * Get {@link DocumentItem} either current or versioned.
   * 
   * @param documentService {@link DocumentService}
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param versionKey {@link String}
   * @param versionAttributes {@link Map}
   * @return DocumentItem
   */
  DocumentItem getDocumentItem(DocumentService documentService, String siteId, String documentId,
      String versionKey, Map<String, AttributeValue> versionAttributes);
}
