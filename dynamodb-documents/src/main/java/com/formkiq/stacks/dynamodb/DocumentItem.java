/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
   * Set User Id.
   *
   * @param username {@link String}
   */
  void setUserId(String username);
}
