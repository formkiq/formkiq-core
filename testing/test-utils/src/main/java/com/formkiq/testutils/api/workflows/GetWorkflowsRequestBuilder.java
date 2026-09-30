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
package com.formkiq.testutils.api.workflows;

import com.formkiq.client.api.DocumentWorkflowsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetWorkflowsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the GetWorkflows API operation.
 */
public class GetWorkflowsRequestBuilder implements HttpRequestBuilder<GetWorkflowsResponse> {

  /** Request parameter. */
  private String next;

  /** Request parameter. */
  private String limit;

  /** Request parameter. */
  private String status;

  @Override
  public ApiHttpResponse<GetWorkflowsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentWorkflowsApi(apiClient).getWorkflows(siteId, this.next,
        this.limit, this.status));
  }

  /**
   * Set limit.
   *
   * @param value String
   * @return this builder
   */
  public GetWorkflowsRequestBuilder withLimit(final String value) {
    this.limit = value;
    return this;
  }

  /**
   * Set next.
   *
   * @param value String
   * @return this builder
   */
  public GetWorkflowsRequestBuilder withNext(final String value) {
    this.next = value;
    return this;
  }

  /**
   * Set status.
   *
   * @param value String
   * @return this builder
   */
  public GetWorkflowsRequestBuilder withStatus(final String value) {
    this.status = value;
    return this;
  }
}
