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
import com.formkiq.client.model.AddDocumentAiPromptRequest;
import com.formkiq.client.model.AddDocumentAiResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Add Document AI Prompt Request.
 */
public class AddDocumentAiPromptRequestBuilder
    implements HttpRequestBuilder<AddDocumentAiResponse> {

  /** Artifact Id. */
  private String artifactId;
  /** Document Id. */
  private final String documentId;
  /** LLM Prompt Entity Name. */
  private final String llmPromptEntityName;
  /** Add Document AI Prompt Request. */
  private AddDocumentAiPromptRequest request = new AddDocumentAiPromptRequest();

  /**
   * constructor.
   *
   * @param document {@link DocumentArtifact}
   * @param promptEntityName {@link String}
   */
  public AddDocumentAiPromptRequestBuilder(final DocumentArtifact document,
      final String promptEntityName) {
    this(document.documentId(), promptEntityName);
    this.artifactId = document.artifactId();
  }

  /**
   * constructor.
   *
   * @param id {@link String}
   * @param promptEntityName {@link String}
   */
  public AddDocumentAiPromptRequestBuilder(final String id, final String promptEntityName) {
    this.documentId = id;
    this.llmPromptEntityName = promptEntityName;
  }

  /**
   * Set Model Id.
   * 
   * @param modelId {@link String}
   * @return {@link AddDocumentAiPromptRequestBuilder}
   */
  public AddDocumentAiPromptRequestBuilder setModelId(final String modelId) {
    this.request.modelId(modelId);
    return this;
  }

  @Override
  public ApiHttpResponse<AddDocumentAiResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new IntelligentDocumentProcessingApi(apiClient).addDocumentAiPrompt(
        this.documentId, this.llmPromptEntityName, siteId, this.artifactId, this.request));
  }
}
