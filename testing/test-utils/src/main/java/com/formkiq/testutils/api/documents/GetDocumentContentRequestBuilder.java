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
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetDocumentContentResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import static com.formkiq.strings.Strings.isEmpty;

/**
 * Builder for Get Document Content Request.
 */
public class GetDocumentContentRequestBuilder
    implements HttpRequestBuilder<GetDocumentContentResponse> {

  /** Optional shareKey query parameter. */
  private String shareKey;

  /** Optional S3 transfer acceleration selection. */
  private Boolean accelerate;

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;

  /** Version Key. */
  private String versionKey;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public GetDocumentContentRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public GetDocumentContentRequestBuilder(final String documentId) {
    this(DocumentArtifact.of(documentId, null));
  }

  /**
   * Select S3 Transfer Acceleration for returned URLs.
   * 
   * @param enabled Whether to accelerate the transfer
   * @return this builder
   */
  public GetDocumentContentRequestBuilder accelerate(final Boolean enabled) {
    this.accelerate = enabled;
    return this;
  }

  public String getContent(final ApiClient client, final String siteId) throws ApiException {
    var resp = submitOk(client, siteId).response();
    return !isEmpty(resp.getContent()) ? resp.getContent() : resp.getContentUrl();
  }

  /**
   * Set shareKey.
   * 
   * @param value Parameter value
   * @return this builder
   */
  public GetDocumentContentRequestBuilder shareKey(final String value) {
    this.shareKey = value;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentContentResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new DocumentsApi(apiClient).getDocumentContent(this.document.documentId(), siteId,
            this.document.artifactId(), this.versionKey, this.shareKey, this.accelerate));
  }

  /**
   * Set Version Key.
   * 
   * @param documentVersionKey {@link String}
   * @return GetDocumentContentRequestBuilder
   */
  public GetDocumentContentRequestBuilder versionKey(final String documentVersionKey) {
    this.versionKey = documentVersionKey;
    return this;
  }
}
