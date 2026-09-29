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

import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentTag;
import com.formkiq.client.model.AddDocumentUploadRequest;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

import java.util.List;

/**
 * Builder for {@link AddDocumentUploadRequest}.
 */
public class AddDocumentUploadRequestBuilder implements HttpRequestBuilder<GetDocumentUrlResponse> {

  /** Optional shareKey query parameter. */
  private String shareKey;

  /** Optional contentLength query parameter. */
  private Integer contentLength;

  /** Optional duration query parameter. */
  private Integer duration;

  /** Optional S3 transfer acceleration selection. */
  private Boolean accelerate;

  /** {@link AddDocumentUploadRequest}. */
  private final AddDocumentUploadRequest request;

  /**
   * constructor.
   */
  public AddDocumentUploadRequestBuilder() {
    this(new AddDocumentUploadRequest());
  }

  /**
   * Construct from a request payload.
   * 
   * @param payload Request body
   */
  public AddDocumentUploadRequestBuilder(final AddDocumentUploadRequest payload) {
    this.request = payload;
  }

  /**
   * Select S3 Transfer Acceleration for returned URLs.
   * 
   * @param enabled Whether to accelerate the transfer
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder accelerate(final Boolean enabled) {
    this.accelerate = enabled;
    return this;
  }

  /**
   * Add a document attribute.
   *
   * @param attribute value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder addAttribute(final AddDocumentAttribute attribute) {
    this.request.addAttributesItem(attribute);
    return this;
  }

  /**
   * Add a document tag.
   *
   * @param tag value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder addTag(final AddDocumentTag tag) {
    this.request.addTagsItem(tag);
    return this;
  }

  /**
   * Set Artifacts.
   *
   * @param createArtifacts boolean
   * @return AddDocumentUploadRequestBuilder
   */
  public AddDocumentUploadRequestBuilder artifacts(final boolean createArtifacts) {
    this.request.setArtifacts(Boolean.valueOf(createArtifacts));
    return this;
  }

  /**
   * Set contentLength.
   * 
   * @param value Parameter value
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder contentLength(final Integer value) {
    this.contentLength = value;
    return this;
  }

  /**
   * Set Document content type.
   * 
   * @param contentType {@link String}
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder contentType(final String contentType) {
    this.request.contentType(contentType);
    return this;
  }

  /**
   * Set the deep link path.
   *
   * @param deepLinkPath value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder deepLinkPath(final String deepLinkPath) {
    this.request.deepLinkPath(deepLinkPath);
    return this;
  }

  /**
   * Set duration.
   * 
   * @param value Parameter value
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder duration(final Integer value) {
    this.duration = value;
    return this;
  }

  /**
   * Set the document height.
   *
   * @param height value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder height(final String height) {
    this.request.height(height);
    return this;
  }

  /**
   * Set the document path.
   *
   * @param path value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder path(final String path) {
    this.request.path(path);
    return this;
  }

  /**
   * Set shareKey.
   * 
   * @param value Parameter value
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder shareKey(final String value) {
    this.shareKey = value;
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return upload URL response
   */
  public ApiHttpResponse<GetDocumentUrlResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentsApi(apiClient).addDocumentUpload(request, siteId,
        this.contentLength, this.duration, this.shareKey, this.accelerate));
  }

  /**
   * Set document tags.
   *
   * @param tags value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder tags(final List<AddDocumentTag> tags) {
    this.request.tags(tags);
    return this;
  }

  /**
   * Set the document width.
   *
   * @param width value to set
   * @return this builder
   */
  public AddDocumentUploadRequestBuilder width(final String width) {
    this.request.width(width);
    return this;
  }
}
