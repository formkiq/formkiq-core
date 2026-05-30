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

import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.api.IntelligentDocumentProcessingApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentAiPromptsResultsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document AI Prompts Request.
 */
public class GetDocumentAiPromptsRequestsBuilder
    implements HttpRequestBuilder<GetDocumentAiPromptsResultsResponse> {

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** Analysis Category. */
  private String analysisCategory;
  /** Next Token. */
  private String next;
  /** Results limit. */
  private String resultsLimit;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public GetDocumentAiPromptsRequestsBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  /**
   * Set Analysis Category.
   * 
   * @param category {@link String}
   * @return {@link GetDocumentAiPromptsRequestsBuilder}
   */
  public GetDocumentAiPromptsRequestsBuilder analysisCategory(final String category) {
    this.analysisCategory = category;
    return this;
  }

  /**
   * Set Limit.
   * 
   * @param limit int
   * @return {@link GetDocumentAiPromptsRequestsBuilder}
   */
  public GetDocumentAiPromptsRequestsBuilder limit(final int limit) {
    this.resultsLimit = String.valueOf(limit);
    return this;
  }

  /**
   * Set Next Token.
   * 
   * @param nextToken {@link String}
   * @return {@link GetDocumentAiPromptsRequestsBuilder}
   */
  public GetDocumentAiPromptsRequestsBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentAiPromptsResultsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new IntelligentDocumentProcessingApi(apiClient)
        .getDocumentAiPromptsResults(this.document.documentId(), siteId, this.document.artifactId(),
            this.analysisCategory, this.resultsLimit, this.next));
  }
}
