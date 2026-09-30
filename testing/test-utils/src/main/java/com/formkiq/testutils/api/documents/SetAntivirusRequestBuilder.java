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

import com.formkiq.client.api.MalwareScanApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the SetAntivirus API operation.
 */
public class SetAntivirusRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** Request parameter. */
  private String documentId;

  /** Request parameter. */
  private String artifactId;

  /** Request parameter. */
  private String shareKey;

  /** Request parameter. */
  private Object body;

  @Override
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new MalwareScanApi(apiClient).setAntivirus(this.documentId, siteId,
        this.artifactId, this.shareKey, this.body));
  }

  /**
   * Set artifactId.
   *
   * @param value String
   * @return this builder
   */
  public SetAntivirusRequestBuilder withArtifactId(final String value) {
    this.artifactId = value;
    return this;
  }

  /**
   * Set body.
   *
   * @param value Object
   * @return this builder
   */
  public SetAntivirusRequestBuilder withBody(final Object value) {
    this.body = value;
    return this;
  }

  /**
   * Set documentId.
   *
   * @param value String
   * @return this builder
   */
  public SetAntivirusRequestBuilder withDocumentId(final String value) {
    this.documentId = value;
    return this;
  }

  /**
   * Set shareKey.
   *
   * @param value String
   * @return this builder
   */
  public SetAntivirusRequestBuilder withShareKey(final String value) {
    this.shareKey = value;
    return this;
  }
}
