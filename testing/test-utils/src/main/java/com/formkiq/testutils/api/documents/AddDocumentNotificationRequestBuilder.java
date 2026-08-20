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
import com.formkiq.client.model.AddDocumentNotificationRequest;
import com.formkiq.client.model.AddDocumentNotificationResponse;
import com.formkiq.client.model.DocumentNotificationType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;
import java.util.Set;

/** Builder for POST /documents/{documentId}/notifications. */
public class AddDocumentNotificationRequestBuilder
    implements HttpRequestBuilder<AddDocumentNotificationResponse> {

  /** Document. */
  private final DocumentArtifact document;
  /** Request. */
  private final AddDocumentNotificationRequest request = new AddDocumentNotificationRequest();

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public AddDocumentNotificationRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  /**
   * constructor.
   *
   * @param documentId document identifier
   */
  public AddDocumentNotificationRequestBuilder(final String documentId) {
    this(DocumentArtifact.of(documentId, null));
  }

  /**
   * Set blind-copy recipients.
   *
   * @param recipients recipient email addresses
   * @return this builder
   */
  public AddDocumentNotificationRequestBuilder bcc(final Set<String> recipients) {
    this.request.bcc(recipients);
    return this;
  }

  /**
   * Set notification body.
   *
   * @param notificationBody notification body
   * @return this builder
   */
  public AddDocumentNotificationRequestBuilder body(final String notificationBody) {
    this.request.body(notificationBody);
    return this;
  }

  /**
   * Set copy recipients.
   *
   * @param recipients recipient email addresses
   * @return this builder
   */
  public AddDocumentNotificationRequestBuilder cc(final Set<String> recipients) {
    this.request.cc(recipients);
    return this;
  }

  /**
   * Set notification type.
   *
   * @param type {@link DocumentNotificationType}
   * @return this builder
   */
  public AddDocumentNotificationRequestBuilder notificationType(
      final DocumentNotificationType type) {
    this.request.notificationType(type);
    return this;
  }

  /**
   * Set notification subject.
   *
   * @param notificationSubject notification subject
   * @return this builder
   */
  public AddDocumentNotificationRequestBuilder subject(final String notificationSubject) {
    this.request.subject(notificationSubject);
    return this;
  }

  @Override
  public ApiHttpResponse<AddDocumentNotificationResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentNotificationsApi(apiClient).addDocumentNotification(
        this.document.documentId(), this.request, siteId, this.document.artifactId()));
  }
}
