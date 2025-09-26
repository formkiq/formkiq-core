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
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeEntity;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentAttributesRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;

/**
 * Builder for {@link AddDocumentAttributesRequest}.
 */
public class AddDocumentAttributeRequestBuilder implements HttpRequestBuilder {

  /** {@link AddDocumentAttributesRequest}. */
  private final AddDocumentAttributesRequest request;
  /** Document Id. */
  private String id;

  /**
   * constructor.
   */
  public AddDocumentAttributeRequestBuilder() {
    this.request = new AddDocumentAttributesRequest();
  }

  /**
   * Set Document Id.
   *
   * @param documentId {@link String}
   * @return AddDocumentAttributeRequestBuilder
   */
  public AddDocumentAttributeRequestBuilder setDocumentId(final String documentId) {
    this.id = documentId;
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param stringValue {@link String}
   * @return AddDocumentAttributesRequestBuilder
   */
  public AddDocumentAttributeRequestBuilder addAttribute(final String key,
      final String stringValue) {
    this.request.addAttributesItem(new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).stringValue(stringValue)));
    return this;
  }

  /**
   * Add Document Attribute.
   *
   * @param key {@link String}
   * @param entityTypeId {@link String}
   * @param entityId {@link String}
   * @param namespace {@link EntityTypeNamespace}
   * @return AddDocumentAttributesRequestBuilder
   */
  public AddDocumentAttributeRequestBuilder addAttribute(final String key,
      final String entityTypeId, final String entityId, final EntityTypeNamespace namespace) {
    this.request.addAttributesItem(new AddDocumentAttribute(new AddDocumentAttributeEntity()
        .key(key).entityId(entityId).entityTypeId(entityTypeId).namespace(namespace)));
    return this;
  }

  /**
   * Add Document Attribute.
   *
   * @param key {@link String}
   * @param numberValue {@link BigDecimal}
   * @return AddDocumentAttributesRequestBuilder
   */
  public AddDocumentAttributeRequestBuilder addAttribute(final String key,
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
  public ApiHttpResponse<AddResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new DocumentAttributesApi(apiClient).addDocumentAttributes(this.id,
        this.request, siteId));
  }
}
