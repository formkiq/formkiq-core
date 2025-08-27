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
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.SetDocumentAttributesRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Builder for {@link SetDocumentAttributesRequest}.
 */
public class SetDocumentAttributeRequestBuilder implements HttpRequestBuilder {

  /** {@link SetDocumentAttributesRequest}. */
  private final SetDocumentAttributesRequest request;
  /** Document Id. */
  private String id;

  /**
   * constructor.
   */
  public SetDocumentAttributeRequestBuilder() {
    this.request = new SetDocumentAttributesRequest();
  }

  /**
   * Set Document Id.
   *
   * @param documentId {@link String}
   * @return AddDocumentAttributeRequestBuilder
   */
  public SetDocumentAttributeRequestBuilder setDocumentId(final String documentId) {
    this.id = documentId;
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param stringValue {@link String}
   * @return SetDocumentAttributesRequestBuilder
   */
  public SetDocumentAttributeRequestBuilder addAttribute(final String key,
      final String stringValue) {
    this.request.addAttributesItem(new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).stringValue(stringValue)));
    return this;
  }

  /**
   * Add Document Attribute.
   *
   * @param key {@link String}
   * @param numberValue {@link BigDecimal}
   * @return SetDocumentAttributesRequestBuilder
   */
  public SetDocumentAttributeRequestBuilder addAttribute(final String key,
      final BigDecimal numberValue) {
    this.request.addAttributesItem(new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).numberValue(numberValue)));
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
    try {
      obj =
          new DocumentAttributesApi(apiClient).setDocumentAttributes(this.id, this.request, siteId);
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(obj, ex);
  }
}
