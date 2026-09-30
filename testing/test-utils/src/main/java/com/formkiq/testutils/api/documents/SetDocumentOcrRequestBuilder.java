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

import com.formkiq.client.api.DocumentOcrApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentOcrResponse;
import com.formkiq.client.model.SetDocumentOcrRequest;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the SetDocumentOcr API operation.
 */
public class SetDocumentOcrRequestBuilder implements HttpRequestBuilder<AddDocumentOcrResponse> {

  /** Request parameter. */
  private String documentId;

  /** Request parameter. */
  private String artifactId;

  /** Request parameter. */
  private SetDocumentOcrRequest setDocumentOcrRequest;

  @Override
  public ApiHttpResponse<AddDocumentOcrResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentOcrApi(apiClient).setDocumentOcr(this.documentId,
        siteId, this.artifactId, this.setDocumentOcrRequest));
  }

  /**
   * Set artifactId.
   *
   * @param value String
   * @return this builder
   */
  public SetDocumentOcrRequestBuilder withArtifactId(final String value) {
    this.artifactId = value;
    return this;
  }

  /**
   * Set documentId.
   *
   * @param value String
   * @return this builder
   */
  public SetDocumentOcrRequestBuilder withDocumentId(final String value) {
    this.documentId = value;
    return this;
  }

  /**
   * Set setDocumentOcrRequest.
   *
   * @param value SetDocumentOcrRequest
   * @return this builder
   */
  public SetDocumentOcrRequestBuilder withSetDocumentOcrRequest(final SetDocumentOcrRequest value) {
    this.setDocumentOcrRequest = value;
    return this;
  }
}
