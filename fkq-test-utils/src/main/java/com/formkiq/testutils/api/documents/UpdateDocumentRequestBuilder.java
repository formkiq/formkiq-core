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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAction;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.AddDocumentMetadata;
import com.formkiq.client.model.UpdateDocumentRequest;
import com.formkiq.client.model.AddDocumentResponse;
import com.formkiq.client.model.DocumentActionType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link UpdateDocumentRequest}.
 */
public class UpdateDocumentRequestBuilder implements HttpRequestBuilder<AddDocumentResponse> {

  /** {@link UpdateDocumentRequest}. */
  private final UpdateDocumentRequest request;
  /** Document Id. */
  private String id;

  /**
   * constructor.
   */
  public UpdateDocumentRequestBuilder() {
    this.request = new UpdateDocumentRequest();
  }

  /**
   * Add Document Action.
   * 
   * @param type {@link DocumentActionType}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addAction(final DocumentActionType type) {
    AddAction action = new AddAction().type(type);
    this.request.addActionsItem(action);
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param value {@link BigDecimal}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addAttribute(final String key, final BigDecimal value) {
    AddDocumentAttribute attr =
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key(key).numberValue(value));
    this.request.addAttributesItem(attr);
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param value {@link Boolean}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addAttribute(final String key, final Boolean value) {
    AddDocumentAttribute attr =
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key(key).booleanValue(value));
    this.request.addAttributesItem(attr);
    return this;
  }

  /**
   * Add Document Attribute.
   * 
   * @param key {@link String}
   * @param value {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addAttribute(final String key, final String value) {
    AddDocumentAttribute attr =
        new AddDocumentAttribute(new AddDocumentAttributeStandard().key(key).stringValue(value));
    this.request.addAttributesItem(attr);
    return this;
  }

  /**
   * Add Metadata.
   *
   * @param key {@link String}
   * @param values {@link List} {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addMetadata(final String key, final List<String> values) {
    this.request.addMetadataItem(new AddDocumentMetadata().key(key).values(values));
    return this;
  }

  /**
   * Add Metadata.
   *
   * @param key {@link String}
   * @param value {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder addMetadata(final String key, final String value) {
    this.request.addMetadataItem(new AddDocumentMetadata().key(key).value(value));
    return this;
  }

  /**
   * Set Random Content.
   *
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder content() {
    this.request.setContent(ID.uuid());
    return this;
  }

  /**
   * Set Content.
   * 
   * @param content {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder content(final String content) {
    this.request.setContent(content);
    return this;
  }

  /**
   * Set Content Type.
   * 
   * @param contentType {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder contentType(final String contentType) {
    this.request.setContentType(contentType);
    return this;
  }

  /**
   * Set Path.
   * 
   * @param path {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder path(final String path) {
    this.request.setPath(path);
    return this;
  }

  /**
   * Set Document Id.
   *
   * @param documentId {@link String}
   * @return UpdateDocumentRequestBuilder
   */
  public UpdateDocumentRequestBuilder setDocumentId(final String documentId) {
    this.id = documentId;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return AddDocumentResponse
   */
  public ApiHttpResponse<AddDocumentResponse> submit(final ApiClient apiClient,
      final String siteId) {
    Objects.requireNonNull(id, "documentId must not be null");
    return executeApiCall(
        () -> new DocumentsApi(apiClient).updateDocument(id, this.request, siteId, null));
  }
}
