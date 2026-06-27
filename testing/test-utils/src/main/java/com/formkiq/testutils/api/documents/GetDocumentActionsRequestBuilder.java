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
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.DocumentAction;
import com.formkiq.client.model.GetDocumentActionsResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Builder for Get Document Actions Request.
 */
public class GetDocumentActionsRequestBuilder
    implements HttpRequestBuilder<GetDocumentActionsResponse> {

  /** {@link String}. */
  private final DocumentArtifact document;
  /** Limit. */
  private String queryLimit;
  /** Next Token. */
  private String queryNext;

  /**
   * constructor.
   *
   * @param artifact {@link DocumentArtifact}
   */
  public GetDocumentActionsRequestBuilder(final DocumentArtifact artifact) {
    this.document = artifact;
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public GetDocumentActionsRequestBuilder(final String documentId) {
    this(DocumentArtifact.of(documentId, null));
  }

  /**
   * Get Document Actions.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @return {@link DocumentAction}
   */
  public List<DocumentAction> getActions(final ApiClient client, final String siteId)
      throws ApiException {
    return notNull(submit(client, siteId).throwIfError().response().getActions());
  }

  /**
   * Set Limit.
   * 
   * @param limit {@link String}
   * @return {@link GetDocumentActionsRequestBuilder}
   */
  public GetDocumentActionsRequestBuilder limit(final String limit) {
    this.queryLimit = limit;
    return this;
  }

  /**
   * Set Next.
   * 
   * @param next {@link String}
   * @return {@link GetDocumentActionsRequestBuilder}
   */
  public GetDocumentActionsRequestBuilder next(final String next) {
    this.queryNext = next;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentActionsResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new DocumentActionsApi(apiClient).getDocumentActions(this.document.documentId(),
            siteId, this.document.artifactId(), queryLimit, null, queryNext));
  }
}
