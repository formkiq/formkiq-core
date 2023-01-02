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

import com.formkiq.aws.dynamodb.PaginationMapToken;
import com.formkiq.aws.dynamodb.PaginationResults;
import com.formkiq.aws.dynamodb.model.DocumentSync;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;

/**
 * 
 * Document Sync Service.
 *
 */
public interface DocumentSyncService {

  /** Sync Message for Added Metadata. */
  String MESSAGE_ADDED_METADATA = "added Document Metadata";

  /** Sync Message for Add Tag. */
  String MESSAGE_ADDED_TAG = "added Tag '%s'";

  /** Sync Message for Updated Metadata. */
  String MESSAGE_UPDATED_METADATA = "updated Document Metadata";

  /** Sync Message for Updated Tag. */
  String MESSAGE_UPDATED_TAG = "updated Tag '%s'";

  /**
   * Delete All.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteAll(String siteId, String documentId);

  /**
   * Get List of {@link DocumentSync}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentSync}
   */
  PaginationResults<DocumentSync> getSyncs(String siteId, String documentId,
      PaginationMapToken token, int limit);

  /**
   * Save Sync.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param service {@link DocumentSyncServiceType}
   * @param status {@link DocumentSyncStatus}
   * @param type {@link DocumentSyncType}
   * @param userId {@link String}
   * @param message {@link String}
   */
  void saveSync(String siteId, String documentId, DocumentSyncServiceType service,
      DocumentSyncStatus status, DocumentSyncType type, String userId, String message);
}
