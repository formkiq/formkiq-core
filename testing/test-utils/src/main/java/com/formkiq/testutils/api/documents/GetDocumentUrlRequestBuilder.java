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
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.module.http.HttpServiceJdk11;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.io.IOException;
import java.util.Optional;

/**
 * Builder for Get Document URL Request.
 */
public class GetDocumentUrlRequestBuilder implements HttpRequestBuilder<GetDocumentUrlResponse> {

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** Url Format. */
  private String urlFormat;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   */
  public GetDocumentUrlRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  public String getContent(final ApiClient client, final String siteId) throws ApiException {
    var resp = submitOk(client, siteId).response();
    try {
      return new HttpServiceJdk11().get(resp.getUrl(), Optional.empty(), Optional.empty()).body();
    } catch (IOException e) {
      throw new ApiException(e);
    }
  }

  public GetDocumentUrlRequestBuilder setFormat(final String format) {
    this.urlFormat = format;
    return this;
  }

  @Override
  public ApiHttpResponse<GetDocumentUrlResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new DocumentsApi(apiClient).getDocumentUrl(this.document.documentId(), siteId,
            this.document.artifactId(), null, null, null, null, null, urlFormat));
  }
}
