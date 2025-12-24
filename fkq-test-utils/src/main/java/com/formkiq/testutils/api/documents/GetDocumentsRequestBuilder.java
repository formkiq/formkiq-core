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
package com.formkiq.testutils.api.documents;

import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get /documents Request.
 */
public class GetDocumentsRequestBuilder implements HttpRequestBuilder<GetDocumentsResponse> {

  /** Action Status. */
  private String actionStatus;
  /** Sync Status. */
  private String syncStatus;
  /** Deleted. */
  private Boolean deleted;
  /** Next Token. */
  private String next;

  /**
   * constructor.
   *
   */
  public GetDocumentsRequestBuilder() {

  }

  /**
   * Set Action Status.
   * 
   * @param documentActionStatus {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder actionStatus(final String documentActionStatus) {
    this.actionStatus = documentActionStatus;
    return this;
  }

  /**
   * Set Sync Status.
   * 
   * @param documentDeleted {@link Boolean}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder deleted(final Boolean documentDeleted) {
    this.deleted = documentDeleted;
    return this;
  }

  /**
   * Set Next token.
   * 
   * @param nextToken {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentsApi(apiClient).getDocuments(siteId, actionStatus,
        syncStatus, deleted, null, null, next, null, null, null));
  }

  /**
   * Set Sync Status.
   * 
   * @param documentSyncStatus {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetDocumentsRequestBuilder syncStatus(final String documentSyncStatus) {
    this.syncStatus = documentSyncStatus;
    return this;
  }
}
