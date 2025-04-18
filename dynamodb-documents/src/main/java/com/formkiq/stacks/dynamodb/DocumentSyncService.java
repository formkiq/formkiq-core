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
import com.formkiq.aws.dynamodb.model.DocumentSyncRecord;
import com.formkiq.aws.dynamodb.model.DocumentSyncServiceType;
import com.formkiq.aws.dynamodb.model.DocumentSyncStatus;
import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.validation.ValidationError;

import java.util.Collection;
import java.util.Date;

/**
 * 
 * Document Sync Service.
 *
 */
public interface DocumentSyncService {

  /**
   * Delete All.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   */
  void deleteAll(String siteId, String documentId);

  /**
   * Get List of {@link DocumentSyncRecord}.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param token {@link PaginationMapToken}
   * @param limit int
   * @return {@link PaginationResults} {@link DocumentSyncRecord}
   */
  PaginationResults<DocumentSyncRecord> getSyncs(String siteId, String documentId,
      PaginationMapToken token, int limit);

  /**
   * Save Sync.
   * 
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param service {@link DocumentSyncServiceType}
   * @param status {@link DocumentSyncStatus}
   * @param type {@link DocumentSyncType}
   * @param documentExists boolean
   */
  void saveSync(String siteId, String documentId, DocumentSyncServiceType service,
      DocumentSyncStatus status, DocumentSyncType type, boolean documentExists);

  /**
   * Update Document Sync Status.
   * 
   * @param pk {@link String}
   * @param sk {@link String}
   * @param status {@link DocumentSyncStatus}
   * @param syncDate {@link java.util.Date}
   */
  void update(String pk, String sk, DocumentSyncStatus status, Date syncDate);

  /**
   * Add Document Sync.
   *
   * @param siteId {@link String}
   * @param documentId {@link String}
   * @param service {@link DocumentSyncServiceType}
   * @param type {@link DocumentSyncType}
   * @return Collection {@link ValidationError}
   */
  Collection<ValidationError> addSync(String siteId, String documentId,
      DocumentSyncServiceType service, DocumentSyncType type);
}
