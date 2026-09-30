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
package com.formkiq.testutils.api.opensearch;

import com.formkiq.client.api.TagIndexApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.IndexSearchRequest;
import com.formkiq.client.model.IndexSearchResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the IndexSearch API operation.
 */
public class IndexSearchRequestBuilder implements HttpRequestBuilder<IndexSearchResponse> {

  /** Request parameter. */
  private IndexSearchRequest indexSearchRequest;

  /** Request parameter. */
  private String limit;

  /** Request parameter. */
  private String next;

  /** Request parameter. */
  private String previous;

  @Override
  public ApiHttpResponse<IndexSearchResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new TagIndexApi(apiClient).indexSearch(this.indexSearchRequest,
        siteId, this.limit, this.next, this.previous));
  }

  /**
   * Set indexSearchRequest.
   *
   * @param value IndexSearchRequest
   * @return this builder
   */
  public IndexSearchRequestBuilder withIndexSearchRequest(final IndexSearchRequest value) {
    this.indexSearchRequest = value;
    return this;
  }

  /**
   * Set limit.
   *
   * @param value String
   * @return this builder
   */
  public IndexSearchRequestBuilder withLimit(final String value) {
    this.limit = value;
    return this;
  }

  /**
   * Set next.
   *
   * @param value String
   * @return this builder
   */
  public IndexSearchRequestBuilder withNext(final String value) {
    this.next = value;
    return this;
  }

  /**
   * Set previous.
   *
   * @param value String
   * @return this builder
   */
  public IndexSearchRequestBuilder withPrevious(final String value) {
    this.previous = value;
    return this;
  }
}
