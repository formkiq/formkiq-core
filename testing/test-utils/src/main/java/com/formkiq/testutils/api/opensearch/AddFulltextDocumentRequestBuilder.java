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

import com.formkiq.client.api.AdvancedDocumentSearchApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentFulltextRequest;
import com.formkiq.client.model.AddDocumentFulltextResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/** Builds requests to add a document to full-text search. */
public class AddFulltextDocumentRequestBuilder
    implements HttpRequestBuilder<AddDocumentFulltextResponse> {

  /** Document identifier. */
  private final String id;
  /** Full-text document payload. */
  private final AddDocumentFulltextRequest request;

  /**
   * Construct a request for a full-text document.
   *
   * @param documentId document identifier
   * @param payload full-text document payload
   */
  public AddFulltextDocumentRequestBuilder(final String documentId,
      final AddDocumentFulltextRequest payload) {
    this.id = documentId;
    this.request = payload;
  }

  @Override
  public ApiHttpResponse<AddDocumentFulltextResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new AdvancedDocumentSearchApi(apiClient)
        .addDocumentFulltext(this.id, siteId, this.request));
  }
}
