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

import java.util.Date;

/** Holder class for Document Sync(s). */
public interface DocumentSync {

  /**
   * Get Document Id.
   *
   * @return {@link String}
   */
  String getDocumentId();

  /**
   * Get service document was synced with.
   * 
   * @return {@link DocumentSyncServiceType}
   */
  DocumentSyncServiceType getService();

  /**
   * Get sync status.
   * 
   * @return {@link DocumentSyncStatus}
   */
  DocumentSyncStatus getStatus();

  /**
   * Get Sync date.
   *
   * @return {@link Date}
   */
  Date getSyncdDate();

  /**
   * Get {@link DocumentSyncType}.
   * 
   * @return {@link DocumentSyncType}
   */
  DocumentSyncType getType();

  /**
   * Get User Id.
   *
   * @return {@link String}
   */
  String getUserId();

  /**
   * Set Document ID.
   *
   * @param id {@link String}
   */
  void setDocumentId(String id);

  /**
   * Set service document was synced with.
   * 
   * @param service {@link DocumentSyncServiceType}
   */
  void setService(DocumentSyncServiceType service);

  /**
   * Set sync status.
   * 
   * @param status {@link DocumentSyncStatus}
   */
  void setStatus(DocumentSyncStatus status);

  /**
   * Set Sync Date.
   *
   * @param date {@link Date}
   */
  void setSyncDate(Date date);

  /**
   * Set sync type.
   * 
   * @param type {@link DocumentSyncType}
   */
  void setType(DocumentSyncType type);

  /**
   * Set User ID.
   *
   * @param id {@link String}
   */
  void setUserId(String id);

}
