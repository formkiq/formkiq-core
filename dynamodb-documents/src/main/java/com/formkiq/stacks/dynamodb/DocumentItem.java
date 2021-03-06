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
   * Get Path.
   *
   * @return {@link String}
   */
  String getPath();

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
   * Set Path.
   *
   * @param filepath {@link String}
   */
  void setPath(String filepath);
  
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
}
