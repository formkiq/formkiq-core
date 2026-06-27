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
import com.formkiq.client.model.AddDocumentReview;
import com.formkiq.client.model.AddDocumentReviewRequest;
import com.formkiq.client.model.AddDocumentReviewResponse;
import com.formkiq.client.model.DocumentReviewStatus;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for POST /documents/{documentId}/reviews.
 */
public class AddDocumentReviewRequestBuilder
    implements HttpRequestBuilder<AddDocumentReviewResponse> {

  /** Document Artifact. */
  private final DocumentArtifact document;
  /** Review Request. */
  private final AddDocumentReview review = new AddDocumentReview();
  /** Add Document Review Request. */
  private final AddDocumentReviewRequest request = new AddDocumentReviewRequest();

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public AddDocumentReviewRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
    this.request.review(this.review);
  }

  /**
   * Set comments.
   *
   * @param comments {@link String}
   * @return {@link AddDocumentReviewRequestBuilder}
   */
  public AddDocumentReviewRequestBuilder comments(final String comments) {
    this.review.comments(comments);
    return this;
  }

  /**
   * Set required decisions.
   *
   * @param requiredDecisions {@link Long}
   * @return {@link AddDocumentReviewRequestBuilder}
   */
  public AddDocumentReviewRequestBuilder requiredDecisions(final Long requiredDecisions) {
    this.review.requiredDecisions(requiredDecisions);
    return this;
  }

  /**
   * Set review category.
   *
   * @param reviewCategory {@link String}
   * @return {@link AddDocumentReviewRequestBuilder}
   */
  public AddDocumentReviewRequestBuilder reviewCategory(final String reviewCategory) {
    this.review.reviewCategory(reviewCategory);
    return this;
  }

  /**
   * Set review status.
   *
   * @param reviewStatus {@link DocumentReviewStatus}
   * @return {@link AddDocumentReviewRequestBuilder}
   */
  public AddDocumentReviewRequestBuilder reviewStatus(final DocumentReviewStatus reviewStatus) {
    this.review.reviewStatus(reviewStatus);
    return this;
  }

  @Override
  public ApiHttpResponse<AddDocumentReviewResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentReviewsApi(apiClient).addDocumentReview(
        this.document.documentId(), this.request, siteId, this.document.artifactId()));
  }

}
