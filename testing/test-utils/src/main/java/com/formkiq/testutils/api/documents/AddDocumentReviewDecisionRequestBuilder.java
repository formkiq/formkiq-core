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
import com.formkiq.client.model.AddDocumentReviewDecision;
import com.formkiq.client.model.AddDocumentReviewDecisionRequest;
import com.formkiq.client.model.AddDocumentReviewDecisionResponse;
import com.formkiq.client.model.ReviewDecisionType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for POST /documents/{documentId}/reviews/{reviewId}/decisions.
 */
public class AddDocumentReviewDecisionRequestBuilder
    implements HttpRequestBuilder<AddDocumentReviewDecisionResponse> {

  /** Decision Request. */
  private final AddDocumentReviewDecision decision = new AddDocumentReviewDecision();
  /** Document Artifact. */
  private final DocumentArtifact document;
  /** Add Document Review Decision Request. */
  private final AddDocumentReviewDecisionRequest request = new AddDocumentReviewDecisionRequest();
  /** Review Id. */
  private final String reviewId;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   * @param documentReviewId {@link String}
   */
  public AddDocumentReviewDecisionRequestBuilder(final DocumentArtifact documentArtifact,
      final String documentReviewId) {
    this.document = documentArtifact;
    this.reviewId = documentReviewId;
    this.request.decision(this.decision);
  }

  /**
   * Set comment.
   *
   * @param comment {@link String}
   * @return {@link AddDocumentReviewDecisionRequestBuilder}
   */
  public AddDocumentReviewDecisionRequestBuilder comment(final String comment) {
    this.decision.comment(comment);
    return this;
  }

  /**
   * Set decision.
   *
   * @param reviewDecision {@link String}
   * @return {@link AddDocumentReviewDecisionRequestBuilder}
   */
  public AddDocumentReviewDecisionRequestBuilder decision(final String reviewDecision) {
    this.decision.decision(reviewDecision);
    return this;
  }

  public String getDecisionId(final ApiClient apiClient, final String siteId) throws ApiException {
    return submitOk(apiClient, siteId).response().getDecisionId();
  }

  @Override
  public ApiHttpResponse<AddDocumentReviewDecisionResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentReviewsApi(apiClient).addDocumentReviewDecision(
        this.document.documentId(), this.reviewId, this.request, siteId,
        this.document.artifactId()));
  }

  /**
   * Set decision type.
   *
   * @param decisionType {@link ReviewDecisionType}
   * @return {@link AddDocumentReviewDecisionRequestBuilder}
   */
  public AddDocumentReviewDecisionRequestBuilder type(final ReviewDecisionType decisionType) {
    this.decision.type(decisionType);
    return this;
  }
}
