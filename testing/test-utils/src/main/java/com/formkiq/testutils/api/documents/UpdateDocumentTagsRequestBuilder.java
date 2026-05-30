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
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link AddDocumentTagsRequest}.
 */
public class UpdateDocumentTagsRequestBuilder implements HttpRequestBuilder<UpdateResponse> {

  /** {@link AddDocumentTagsRequest}. */
  private final AddDocumentTagsRequest request;
  /** Document Id. */
  private final String id;
  /** Artifact Id. */
  private String artifactId;

  /**
   * constructor.
   *
   * @param documentId {@link String}
   */
  public UpdateDocumentTagsRequestBuilder(final String documentId) {
    this.id = documentId;
    this.request = new AddDocumentTagsRequest();
  }

  /**
   * Add Document Tag.
   *
   * @param key {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentTagsRequestBuilder addTag(final String key) {
    this.request.addTagsItem(new AddDocumentTag().key(key));
    return this;
  }

  /**
   * Add Document Tag.
   *
   * @param key {@link String}
   * @param values {@link List} {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentTagsRequestBuilder addTag(final String key, final List<String> values) {
    this.request.addTagsItem(new AddDocumentTag().key(key).values(values));
    return this;
  }

  /**
   * Add Document Tag.
   *
   * @param key {@link String}
   * @param value {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentTagsRequestBuilder addTag(final String key, final String value) {
    this.request.addTagsItem(new AddDocumentTag().key(key).value(value));
    return this;
  }

  public UpdateDocumentTagsRequestBuilder setArtifactId(final String artifact) {
    this.artifactId = artifact;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return UpdateResponse
   */
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    Objects.requireNonNull(id, "documentId must not be null");
    return executeApiCall(() -> new DocumentTagsApi(apiClient).updateDocumentTags(id, this.request,
        siteId, this.artifactId));
  }
}
