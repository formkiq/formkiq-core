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
import com.formkiq.client.api.DocumentActionsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentActionsRequest;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link AddDocumentAttributesRequest}.
 */
public class AddDocumentActionsRequestBuilder implements HttpRequestBuilder<AddResponse> {

  /** Document Id. */
  private final String id;
  /** Artifact Id. */
  private String artifactId;
  /** {@link AddDocumentActionsRequest}. */
  private AddDocumentActionsRequest request;

  /**
   * constructor.
   *
   * @param document {@link DocumentArtifact}
   */
  public AddDocumentActionsRequestBuilder(final DocumentArtifact document) {
    this(document.documentId());
    this.artifactId = document.artifactId();
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public AddDocumentActionsRequestBuilder(final String documentId) {
    this.id = documentId;
    this.request = new AddDocumentActionsRequest();
  }

  /**
   * Add Action.
   * 
   * @param type {@link DocumentActionType}
   * @return {@link AddDocumentActionsRequestBuilder}
   */
  public AddDocumentActionsRequestBuilder addAction(final DocumentActionType type) {
    this.request.addActionsItem(new AddAction().type(type));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<AddResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new DocumentActionsApi(apiClient).addDocumentActions(this.id,
        siteId, this.artifactId, this.request));
  }
}
