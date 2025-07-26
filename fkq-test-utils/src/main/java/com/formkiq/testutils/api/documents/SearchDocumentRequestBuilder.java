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

import com.formkiq.client.api.DocumentSearchApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentSearch;
import com.formkiq.client.model.DocumentSearchMeta;
import com.formkiq.client.model.DocumentSearchRequest;
import com.formkiq.client.model.DocumentSearchResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link DocumentSearchRequest}.
 */
public class SearchDocumentRequestBuilder implements HttpRequestBuilder {

  /** {@link DocumentSearchRequest}. */
  private final DocumentSearchRequest request;
  /** Previous Token. */
  private String previous;
  /** Next Token. */
  private String next;
  /** Limit. */
  private String limit;

  /**
   * constructor.
   */
  public SearchDocumentRequestBuilder() {
    this.request = new DocumentSearchRequest();
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param folder {@link String}
   * @return this builder
   */
  public SearchDocumentRequestBuilder folder(final String folder) {
    this.request.query(new DocumentSearch().meta(new DocumentSearchMeta().folder(folder)));
    return this;
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param docsLimit {@link String}
   * @return this builder
   */
  public SearchDocumentRequestBuilder limit(final String docsLimit) {
    this.limit = docsLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the docsNext set of results.
   *
   * @param docsNext {@link String}
   * @return this builder
   */
  public SearchDocumentRequestBuilder next(final String docsNext) {
    this.next = docsNext;
    return this;
  }

  /**
   * Set the pagination cursor for the docsPrevious set of results.
   *
   * @param docsPrevious {@link String}
   * @return this builder
   */
  public SearchDocumentRequestBuilder previous(final String docsPrevious) {
    this.previous = docsPrevious;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return DocumentSearchResponse
   */
  public ApiHttpResponse<DocumentSearchResponse> submit(final ApiClient apiClient,
      final String siteId) {
    DocumentSearchResponse obj = null;
    ApiException ex = null;
    try {
      obj = new DocumentSearchApi(apiClient).documentSearch(this.request, siteId, limit, next,
          previous);
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(obj, ex);
  }
}
