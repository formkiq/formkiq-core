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
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentTagsRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.Objects;

/**
 * Builder for {@link AddDocumentTagsRequest}.
 */
public class AddDocumentTagRequestBuilder implements HttpRequestBuilder {

  /** {@link AddDocumentTagsRequest}. */
  private final AddDocumentTagsRequest request;
  /** Document Id. */
  private String id;

  /**
   * constructor.
   */
  public AddDocumentTagRequestBuilder() {
    this.request = new AddDocumentTagsRequest();
  }

  /**
   * Set Document Id.
   *
   * @param documentId {@link String}
   * @return AddDocumentAttributeRequestBuilder
   */
  public AddDocumentTagRequestBuilder setDocumentId(final String documentId) {
    this.id = documentId;
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param stringValue {@link String}
   * @return AddDocumentTagsRequestBuilder
   */
  public AddDocumentTagRequestBuilder addTag(final String key, final String stringValue) {
    this.request.addTagsItem(new AddDocumentTag().key(key).value(stringValue));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<AddResponse> submit(final ApiClient apiClient, final String siteId) {
    AddResponse obj = null;
    ApiException ex = null;
    Objects.requireNonNull(id, "documentId must not be null");
    try {
      new DocumentTagsApi(apiClient).addDocumentTags(this.id, this.request, siteId);
      obj = new AddResponse();
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(obj, ex);
  }
}
