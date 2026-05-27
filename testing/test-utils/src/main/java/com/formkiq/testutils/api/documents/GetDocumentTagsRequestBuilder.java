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

import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentTagsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document Tags Request.
 */
public class GetDocumentTagsRequestBuilder implements HttpRequestBuilder<GetDocumentTagsResponse> {

  /** {@link String}. */
  private final String docId;
  /** Artifact Id. */
  private String artifactId;
  /** Limit Results. */
  private String limit;
  /** Next Token. */
  private String next;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public GetDocumentTagsRequestBuilder(final String documentId) {
    this.docId = documentId;
  }

  /**
   * Set the maximum number of results to return.
   *
   * @param docsLimit {@link String}
   * @return this builder
   */
  public GetDocumentTagsRequestBuilder limit(final String docsLimit) {
    this.limit = docsLimit;
    return this;
  }

  /**
   * Set the pagination cursor for the docsNext set of results.
   *
   * @param docsNext {@link String}
   * @return this builder
   */
  public GetDocumentTagsRequestBuilder next(final String docsNext) {
    this.next = docsNext;
    return this;
  }

  public GetDocumentTagsRequestBuilder setArtifactId(final String id) {
    this.artifactId = id;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentTagsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentTagsApi(apiClient).getDocumentTags(this.docId, siteId,
        this.artifactId, limit, null, next, null));
  }
}
