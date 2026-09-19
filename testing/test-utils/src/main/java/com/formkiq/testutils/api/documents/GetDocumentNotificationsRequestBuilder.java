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
import com.formkiq.client.api.DocumentNotificationsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentNotification;
import com.formkiq.client.model.GetDocumentNotificationsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

/**
 * Builder for GET /documents/{documentId}/notifications.
 */
public class GetDocumentNotificationsRequestBuilder
    implements HttpRequestBuilder<GetDocumentNotificationsResponse> {

  /** Document Artifact. */
  private final DocumentArtifact document;
  /** Limit. */
  private String limit;
  /** Next token. */
  private String next;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public GetDocumentNotificationsRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  public List<DocumentNotification> getNotifications(final ApiClient client, final String siteId)
      throws ApiException {
    return submitOk(client, siteId).response().getNotifications();
  }

  /**
   * Set limit.
   *
   * @param maxResults {@link String}
   * @return {@link GetDocumentNotificationsRequestBuilder}
   */
  public GetDocumentNotificationsRequestBuilder limit(final String maxResults) {
    this.limit = maxResults;
    return this;
  }

  /**
   * Set next token.
   *
   * @param nextToken {@link String}
   * @return {@link GetDocumentNotificationsRequestBuilder}
   */
  public GetDocumentNotificationsRequestBuilder next(final String nextToken) {
    this.next = nextToken;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentNotificationsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentNotificationsApi(apiClient).getDocumentNotifications(
        this.document.documentId(), siteId, this.document.artifactId(), this.limit, this.next));
  }
}
