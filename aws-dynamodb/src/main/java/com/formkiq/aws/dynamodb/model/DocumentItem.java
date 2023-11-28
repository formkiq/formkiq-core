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
package com.formkiq.aws.dynamodb.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/** Holder class for Document(s). */
public interface DocumentItem {

  /**
   * Gets Belongs To DocumentId.
   * 
   * @return {@link String}
   */
  String getBelongsToDocumentId();

  /**
   * Get Entity Checksum.
   * 
   * @return {@link String}
   */
  String getChecksum();

  /**
   * Get Content Length.
   *
   * @return {@link Long}
   */
  Long getContentLength();

  /**
   * Get Content Type.
   *
   * @return {@link String}
   */
  String getContentType();

  /**
   * Get Deep Link Path.
   * 
   * @return {@link String}
   */
  String getDeepLinkPath();

  /**
   * Get Document Id.
   *
   * @return {@link String}
   */
  String getDocumentId();

  /**
   * Get Documents.
   * 
   * @return {@link List} {@link DocumentItem}
   */
  List<DocumentItem> getDocuments();

  /**
   * Get Inserteddate.
   *
   * @return {@link Date}
   */
  Date getInsertedDate();

  /**
   * Get Last Modified Date.
   * 
   * @return {@link Date}
   */
  Date getLastModifiedDate();

  /**
   * Get Document Metadata.
   * 
   * @return {@link Collection} {@link DocumentMetadata}
   */
  Collection<DocumentMetadata> getMetadata();

  /**
   * Get Path.
   *
   * @return {@link String}
   */
  String getPath();

  /**
   * Get S3 Version.
   * 
   * @return {@link String}
   */
  String getS3version();

  /**
   * Get Tag Schema Id.
   * 
   * @return {@link String}
   */
  String getTagSchemaId();

  /**
   * Get Time To Live.
   *
   * @return {@link String}
   */
  String getTimeToLive();

  /**
   * Get User Id.
   *
   * @return {@link String}
   */
  String getUserId();

  /**
   * Get Document Version.
   * 
   * @return {@link String}
   */
  String getVersion();

  /**
   * Sets Belongs To DocumentId.
   * 
   * @param documentId {@link String}
   */
  void setBelongsToDocumentId(String documentId);

  /**
   * Set Entity Checksum.
   * 
   * @param checksum {@link String}
   */
  void setChecksum(String checksum);

  /**
   * Set Content Length.
   *
   * @param cl {@link Long}
   */
  void setContentLength(Long cl);

  /**
   * Set Content Type.
   *
   * @param ct {@link String}
   */
  void setContentType(String ct);

  /**
   * Deep Link Path.
   * 
   * @param deepLinkPath {@link String}
   */
  void setDeepLinkPath(String deepLinkPath);

  /**
   * Set Document ID.
   *
   * @param id {@link String}
   */
  void setDocumentId(String id);

  /**
   * Set Documents.
   * 
   * @param ids {@link List} {@link DocumentItem}
   */
  void setDocuments(List<DocumentItem> ids);

  /**
   * Set Inserted Date.
   *
   * @param date {@link Date}
   */
  void setInsertedDate(Date date);

  /**
   * Set Last Modified Date.
   *
   * @param date {@link Date}
   */
  void setLastModifiedDate(Date date);

  /**
   * Set Metadata {@link Collection} {@link DocumentMetadata}.
   * 
   * @param metadata {@link Collection} {@link DocumentMetadata}
   */
  void setMetadata(Collection<DocumentMetadata> metadata);

  /**
   * Set Path.
   *
   * @param filepath {@link String}
   */
  void setPath(String filepath);

  /**
   * Set S3 Version.
   * 
   * @param version {@link String}
   */
  void setS3version(String version);

  /**
   * Set TagSchema Id.
   * 
   * @param id {@link String}
   */
  void setTagSchemaId(String id);

  /**
   * Set Time To Live.
   *
   * @param ttl {@link String}
   */
  void setTimeToLive(String ttl);

  /**
   * Set User Id.
   *
   * @param username {@link String}
   */
  void setUserId(String username);

  /**
   * Set Document Version.
   * 
   * @param version {@link String}
   */
  void setVersion(String version);
}
