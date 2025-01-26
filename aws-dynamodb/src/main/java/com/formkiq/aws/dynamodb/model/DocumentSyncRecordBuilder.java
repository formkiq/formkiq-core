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

import com.formkiq.aws.dynamodb.ApiAuthorization;

import java.util.Date;

/**
 * Builder for {@link DocumentSyncRecord}.
 */
public class DocumentSyncRecordBuilder {

  /** Sync Message for Added Content. */
  public static final String MESSAGE_ADDED_CONTENT = "added Document Content";

  /** Sync Message for Added Metadata. */
  public static final String MESSAGE_ADDED_METADATA = "added Document Metadata";

  /** Sync Message for Updated Content. */
  public static final String MESSAGE_UPDATED_CONTENT = "updated Document Content";

  /** Sync Message for Updated Metadata. */
  public static final String MESSAGE_UPDATED_METADATA = "updated Document Metadata";


  /**
   * Build {@link DocumentSyncRecord}.
   * 
   * @param documentId {@link String}
   * @param syncType {@link DocumentSyncType}
   * @param documentExists does document already exist.
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord buildEventBridge(final String documentId,
      final DocumentSyncType syncType, final boolean documentExists) {

    String username = ApiAuthorization.getAuthorization().getUsername();
    return new DocumentSyncRecord().setInsertedDate(new Date()).setDocumentId(documentId)
        .setService(DocumentSyncServiceType.EVENTBRIDGE).setType(syncType)
        .setStatus(DocumentSyncStatus.PENDING).setMessage(getMessage(syncType, documentExists))
        .setUserId(username);
  }

  private String getMessage(final DocumentSyncType syncType, final boolean documentExists) {
    return switch (syncType) {
      case CONTENT -> documentExists ? MESSAGE_UPDATED_CONTENT : MESSAGE_ADDED_CONTENT;
      case DELETE_METADATA -> "deleted Document Metadata";
      case SOFT_DELETE_METADATA -> "soft deleted Document Metadata";
      default -> documentExists ? MESSAGE_UPDATED_METADATA : MESSAGE_ADDED_METADATA;
    };
  }

  /**
   * Build {@link DocumentSyncRecord}.
   * 
   * @param documentId {@link String}
   * @param serviceType {@link DocumentSyncServiceType}
   * @param syncStatus {@link DocumentSyncStatus}
   * @param syncType {@link DocumentSyncType}
   * @param syncDate {@link Date}
   * @param documentExists boolean
   * @return DocumentSyncRecord
   */
  public DocumentSyncRecord build(final String documentId,
      final DocumentSyncServiceType serviceType, final DocumentSyncStatus syncStatus,
      final DocumentSyncType syncType, final Date syncDate, final boolean documentExists) {

    String username = ApiAuthorization.getAuthorization().getUsername();
    String message = getMessage(syncType, documentExists);
    return new DocumentSyncRecord().setDocumentId(documentId).setService(serviceType)
        .setStatus(syncStatus).setType(syncType).setUserId(username).setMessage(message)
        .setInsertedDate(new Date()).setSyncDate(syncDate);
  }
}
