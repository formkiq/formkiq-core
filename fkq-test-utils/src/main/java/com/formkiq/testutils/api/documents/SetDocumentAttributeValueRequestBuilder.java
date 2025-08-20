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

import com.formkiq.client.api.DocumentAttributesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddDocumentAttributeValue;
import com.formkiq.client.model.SetDocumentAttributeRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.Objects;

/**
 * Builder for {@link SetDocumentAttributeRequest}.
 */
public class SetDocumentAttributeValueRequestBuilder implements HttpRequestBuilder {

  /** {@link SetDocumentAttributeRequest}. */
  private final SetDocumentAttributeRequest request;
  /** Document Id. */
  private String id;
  /** Attribute Key. */
  private String key;

  /**
   * constructor.
   */
  public SetDocumentAttributeValueRequestBuilder() {
    this.request = new SetDocumentAttributeRequest();
  }

  /**
   * Set Document Id.
   *
   * @param documentId {@link String}
   * @return SetDocumentAttributeValueRequestBuilder
   */
  public SetDocumentAttributeValueRequestBuilder setDocumentId(final String documentId) {
    this.id = documentId;
    return this;
  }

  /**
   * Set Attribute Key.
   *
   * @param attributeKey {@link String}
   * @return SetDocumentAttributeValueRequestBuilder
   */
  public SetDocumentAttributeValueRequestBuilder setKey(final String attributeKey) {
    this.key = attributeKey;
    return this;
  }

  /**
   * Add Attribute Value..
   * 
   * @param stringValue {@link String}
   * @return SetDocumentAttributeRequestBuilder
   */
  public SetDocumentAttributeValueRequestBuilder stringValue(final String stringValue) {
    this.request.setAttribute(new AddDocumentAttributeValue().stringValue(stringValue));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    SetResponse obj = null;
    ApiException ex = null;
    Objects.requireNonNull(id, "documentId must not be null");
    Objects.requireNonNull(key, "key must not be null");

    try {
      obj = new DocumentAttributesApi(apiClient).setDocumentAttributeValue(this.id, this.key,
          this.request, siteId);
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(obj, ex);
  }
}
