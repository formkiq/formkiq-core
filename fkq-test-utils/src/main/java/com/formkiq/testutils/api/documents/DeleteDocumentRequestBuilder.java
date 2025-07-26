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
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Delete Document Request.
 */
public class DeleteDocumentRequestBuilder implements HttpRequestBuilder {

  /** {@link String}. */
  private final String id;
  /** Soft Delete. */
  private Boolean softDelete;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public DeleteDocumentRequestBuilder(final String documentId) {
    this.id = documentId;
  }

  /**
   * Whether to soft delete document.
   *
   * @param documentSoftDelete {@link Boolean}
   * @return this builder
   */
  public DeleteDocumentRequestBuilder softDelete(final boolean documentSoftDelete) {
    this.softDelete = documentSoftDelete;
    return this;
  }

  @Override
  public ApiHttpResponse<DeleteResponse> submit(final ApiClient apiClient, final String siteId) {
    ApiException ex = null;
    DeleteResponse response = null;

    try {
      response = new DocumentsApi(apiClient).deleteDocument(this.id, siteId, softDelete);
    } catch (ApiException e) {
      ex = e;
    }

    return new ApiHttpResponse<>(response, ex);
  }
}
