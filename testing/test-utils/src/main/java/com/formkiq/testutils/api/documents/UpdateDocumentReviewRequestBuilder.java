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
import com.formkiq.client.model.DocumentReviewStatus;
import com.formkiq.client.model.UpdateDocumentReview;
import com.formkiq.client.model.UpdateDocumentReviewRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for PATCH /documents/{documentId}/reviews/{reviewId}.
 */
public class UpdateDocumentReviewRequestBuilder implements HttpRequestBuilder<UpdateResponse> {

  /** Document Artifact. */
  private final DocumentArtifact document;
  /** Review Request. */
  private final UpdateDocumentReview review = new UpdateDocumentReview();
  /** Review Id. */
  private final String reviewId;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   * @param documentReviewId {@link String}
   */
  public UpdateDocumentReviewRequestBuilder(final DocumentArtifact documentArtifact,
      final String documentReviewId) {
    this.document = documentArtifact;
    this.reviewId = documentReviewId;
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   * @param documentReviewId {@link String}
   */
  public UpdateDocumentReviewRequestBuilder(final String documentId,
      final String documentReviewId) {
    this(DocumentArtifact.of(documentId, null), documentReviewId);
  }

  /**
   * Set comments.
   *
   * @param comments {@link String}
   * @return {@link UpdateDocumentReviewRequestBuilder}
   */
  public UpdateDocumentReviewRequestBuilder comments(final String comments) {
    this.review.comments(comments);
    return this;
  }

  public String getMessage(final ApiClient client, final String siteId) throws ApiException {
    return submitOk(client, siteId).response().getMessage();
  }

  /**
   * Set required decisions.
   *
   * @param requiredDecisions {@link Long}
   * @return {@link UpdateDocumentReviewRequestBuilder}
   */
  public UpdateDocumentReviewRequestBuilder requiredDecisions(final Long requiredDecisions) {
    this.review.requiredDecisions(requiredDecisions);
    return this;
  }

  /**
   * Set review status.
   *
   * @param reviewStatus {@link DocumentReviewStatus}
   * @return {@link UpdateDocumentReviewRequestBuilder}
   */
  public UpdateDocumentReviewRequestBuilder reviewStatus(final DocumentReviewStatus reviewStatus) {
    this.review.reviewStatus(reviewStatus);
    return this;
  }

  @Override
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    UpdateDocumentReviewRequest request = new UpdateDocumentReviewRequest().review(this.review);
    return executeApiCall(() -> new DocumentReviewsApi(apiClient).updateDocumentReview(
        this.document.documentId(), this.reviewId, request, siteId, this.document.artifactId()));
  }
}
