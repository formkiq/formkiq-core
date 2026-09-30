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

import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the DeleteDocumentFulltextTag API operation.
 */
public class DeleteDocumentFulltextTagRequestBuilder implements HttpRequestBuilder<Void> {

  /** Request parameter. */
  private String documentId;

  /** Request parameter. */
  private String tagKey;

  /** Request parameter. */
  private String shareKey;

  @Override
  public ApiHttpResponse<Void> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> {
      new AdvancedDocumentSearchApi(apiClient).deleteDocumentFulltextTag(this.documentId,
          this.tagKey, siteId, this.shareKey);
      return null;
    });
  }

  /**
   * Set documentId.
   *
   * @param value String
   * @return this builder
   */
  public DeleteDocumentFulltextTagRequestBuilder withDocumentId(final String value) {
    this.documentId = value;
    return this;
  }

  /**
   * Set shareKey.
   *
   * @param value String
   * @return this builder
   */
  public DeleteDocumentFulltextTagRequestBuilder withShareKey(final String value) {
    this.shareKey = value;
    return this;
  }

  /**
   * Set tagKey.
   *
   * @param value String
   * @return this builder
   */
  public DeleteDocumentFulltextTagRequestBuilder withTagKey(final String value) {
    this.tagKey = value;
    return this;
  }
}
