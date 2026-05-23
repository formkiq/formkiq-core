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
import com.formkiq.client.model.GetDocumentAiPromptResultsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Add Document AI Prompt Request.
 */
public class GetDocumentAiPromptRequestsBuilder
    implements HttpRequestBuilder<GetDocumentAiPromptResultsResponse> {

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** LLM Prompt Entity Name. */
  private final String llmPromptEntityName;
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
   * @param promptEntityName {@link String}
   */
  public GetDocumentAiPromptRequestsBuilder(final DocumentArtifact documentArtifact,
      final String promptEntityName) {
    this.document = documentArtifact;
    this.llmPromptEntityName = promptEntityName;
  }

  /**
   * Set Analysis Category.
   * 
   * @param category {@link String}
   * @return {@link GetDocumentAiPromptRequestsBuilder}
   */
  public GetDocumentAiPromptRequestsBuilder analysisCategory(final String category) {
    this.analysisCategory = category;
    return this;
  }

  /**
   * Set Limit.
   * 
   * @param limit int
   * @return {@link GetDocumentAiPromptRequestsBuilder}
   */
  public GetDocumentAiPromptRequestsBuilder limit(final int limit) {
    this.resultsLimit = String.valueOf(limit);
    return this;
  }

  /**
   * Set Next Token.
   * 
   * @param nextToken {@link String}
   * @return {@link GetDocumentAiPromptRequestsBuilder}
   */
  public GetDocumentAiPromptRequestsBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentAiPromptResultsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new IntelligentDocumentProcessingApi(apiClient)
        .getDocumentAiPromptResults(this.document.documentId(), this.llmPromptEntityName, siteId,
            this.document.artifactId(), this.analysisCategory, this.resultsLimit, this.next));
  }
}
