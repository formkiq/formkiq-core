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
import com.formkiq.client.api.DocumentReviewsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentReviewDecision;
import com.formkiq.client.model.GetDocumentReviewDecisionsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

/**
 * Builder for GET /documents/{documentId}/reviews/{reviewId}/decisions.
 */
public class GetDocumentReviewDecisionsRequestBuilder
    implements HttpRequestBuilder<GetDocumentReviewDecisionsResponse> {

  /** Document Artifact. */
  private final DocumentArtifact document;
  /** Limit. */
  private String limit;
  /** Next token. */
  private String next;
  /** Review Id. */
  private final String reviewId;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   * @param documentReviewId {@link String}
   */
  public GetDocumentReviewDecisionsRequestBuilder(final DocumentArtifact documentArtifact,
      final String documentReviewId) {
    this.document = documentArtifact;
    this.reviewId = documentReviewId;
  }

  public List<DocumentReviewDecision> getDecisions(final ApiClient client, final String siteId)
      throws ApiException {
    return submitOk(client, siteId).response().getDecisions();
  }

  /**
   * Set limit.
   *
   * @param maxResults {@link String}
   * @return {@link GetDocumentReviewDecisionsRequestBuilder}
   */
  public GetDocumentReviewDecisionsRequestBuilder limit(final String maxResults) {
    this.limit = maxResults;
    return this;
  }

  /**
   * Set next token.
   *
   * @param nextToken {@link String}
   * @return {@link GetDocumentReviewDecisionsRequestBuilder}
   */
  public GetDocumentReviewDecisionsRequestBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentReviewDecisionsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentReviewsApi(apiClient).getDocumentReviewDecisions(
        this.document.documentId(), this.reviewId, siteId, this.document.artifactId(), this.limit,
        this.next));
  }
}
