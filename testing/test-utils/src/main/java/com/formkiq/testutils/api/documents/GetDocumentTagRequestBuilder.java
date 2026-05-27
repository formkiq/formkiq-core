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
import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentTagResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Document Tags Request.
 */
public class GetDocumentTagRequestBuilder implements HttpRequestBuilder<GetDocumentTagResponse> {

  /** {@link DocumentArtifact}. */
  private final DocumentArtifact document;
  /** Tag Key. */
  private final String key;

  /**
   * constructor.
   *
   * @param documentArtifact {@link DocumentArtifact}
   * @param tagKey {@link String}
   */
  public GetDocumentTagRequestBuilder(final DocumentArtifact documentArtifact,
      final String tagKey) {
    this.document = documentArtifact;
    this.key = tagKey;
  }

  /**
   * constructor.
   *
   * @param documentId {@link String}
   * @param tagKey {@link String}
   */
  public GetDocumentTagRequestBuilder(final String documentId, final String tagKey) {
    this(DocumentArtifact.of(documentId, null), tagKey);
  }

  @Override
  public ApiHttpResponse<GetDocumentTagResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentTagsApi(apiClient).getDocumentTag(document.documentId(),
        this.key, siteId, document.artifactId(), null));
  }
}
