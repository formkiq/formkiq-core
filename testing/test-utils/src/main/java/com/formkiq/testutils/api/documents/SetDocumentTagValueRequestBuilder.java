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

import com.formkiq.client.api.DocumentTagsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.SetDocumentTagKeyRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link SetDocumentTagKeyRequest}.
 */
public class SetDocumentTagValueRequestBuilder implements HttpRequestBuilder<UpdateResponse> {

  /** {@link SetDocumentTagKeyRequest}. */
  private final SetDocumentTagKeyRequest request;
  /** Document Id. */
  private final String id;
  /** Tag Key. */
  private final String key;
  /** Artifact Id. */
  private String artifactId;

  /**
   * constructor.
   * 
   * @param documentId {@link String}
   * @param tagKey {@link String}
   */
  public SetDocumentTagValueRequestBuilder(final String documentId, final String tagKey) {
    this.id = documentId;
    this.key = tagKey;
    this.request = new SetDocumentTagKeyRequest();
  }

  /**
   * Add Document Tag value.
   *
   * @param value {@link String}
   * @return SetDocumentTagKeyRequestBuilder
   */
  public SetDocumentTagValueRequestBuilder addValue(final String value) {
    this.request.addValuesItem(value);
    return this;
  }

  public SetDocumentTagValueRequestBuilder setArtifactId(final String artifact) {
    this.artifactId = artifact;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new DocumentTagsApi(apiClient).setDocumentTag(this.id, key,
        this.request, siteId, this.artifactId));
  }

  /**
   * Add Document Tag value.
   *
   * @param value {@link String}
   * @return SetDocumentTagKeyRequestBuilder
   */
  public SetDocumentTagValueRequestBuilder value(final String value) {
    this.request.setValue(value);
    return this;
  }
}
