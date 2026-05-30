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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.dynamodb.model.DocumentSyncType;
import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.stacks.dynamodb.DocumentSyncService;

/**
 * Add Document Sync.
 */
@Reflectable
public class AddDocumentSync {
  /** {@link AddDocumentSyncServiceType}. */
  private AddDocumentSyncServiceType service;
  /** {@link DocumentSyncType}. */
  private DocumentSyncType type;

  /**
   * constructor.
   */
  public AddDocumentSync() {

  }

  /**
   * Get {@link DocumentSyncService}.
   * 
   * @return {@link AddDocumentSyncServiceType}
   */
  public AddDocumentSyncServiceType getService() {
    return this.service;
  }

  /**
   * Get {@link DocumentSyncType}.
   * 
   * @return {@link DocumentSyncType}
   */
  public DocumentSyncType getType() {
    return this.type;
  }

  /**
   * Set Service.
   *
   * @param documentSyncService {@link AddDocumentSyncServiceType}
   * @return AddDocumentSync
   */
  public AddDocumentSync setService(final AddDocumentSyncServiceType documentSyncService) {
    this.service = documentSyncService;
    return this;
  }

  /**
   * Set Type.
   *
   * @param documentSyncType {@link DocumentSyncType}
   * @return AddDocumentSync
   */
  public AddDocumentSync setType(final DocumentSyncType documentSyncType) {
    this.type = documentSyncType;
    return this;
  }
}
