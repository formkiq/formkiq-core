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
package com.formkiq.testutils.api.mappings;

import com.formkiq.client.api.MappingsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentWorkflowRequest;
import com.formkiq.client.model.GetMappingsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import com.formkiq.testutils.api.documents.GetDocumentsRequestBuilder;

/**
 * Builder for {@link AddDocumentWorkflowRequest}.
 */
public class GetMappingsRequestBuilder implements HttpRequestBuilder<GetMappingsResponse> {

  /** Limit. */
  private String limit;
  /** Limit. */
  private String next;

  /**
   * constructor.
   *
   */
  public GetMappingsRequestBuilder() {}

  /**
   * Set Limit.
   *
   * @param mappingsLimit {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetMappingsRequestBuilder limit(final String mappingsLimit) {
    this.limit = mappingsLimit;
    return this;
  }

  /**
   * Set Next token.
   *
   * @param nextToken {@link String}
   * @return {@link GetDocumentsRequestBuilder}
   */
  public GetMappingsRequestBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return GetMappingsResponse
   */
  public ApiHttpResponse<GetMappingsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new MappingsApi(apiClient).getMappings(siteId, limit, next));
  }
}
