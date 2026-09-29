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
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.GetDocumentUrlResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for GET /documents/{documentId}/upload requests.
 */
public class GetDocumentIdUploadRequestBuilder
    implements HttpRequestBuilder<GetDocumentUrlResponse> {

  /** Optional shareKey query parameter. */
  private String shareKey;

  /** Optional S3 transfer acceleration selection. */
  private Boolean accelerate;

  /** Document checksum. */
  private String checksum;

  /** Document checksum type. */
  private String checksumType;

  /** Document content length. */
  private Integer contentLength;

  /** URL duration in hours. */
  private Integer duration;

  /** Target document or artifact. */
  private final DocumentArtifact document;

  /**
   * constructor.
   * 
   * @param documentArtifact Target document
   *
   */
  public GetDocumentIdUploadRequestBuilder(final DocumentArtifact documentArtifact) {
    this.document = documentArtifact;
  }

  /**
   * Select S3 Transfer Acceleration for returned URLs.
   * 
   * @param enabled Whether to accelerate the transfer
   * @return this builder
   */
  public GetDocumentIdUploadRequestBuilder accelerate(final Boolean enabled) {
    this.accelerate = enabled;
    return this;
  }

  /**
   * Set checksum.
   *
   * @param documentChecksum {@link String}
   * @return GetDocumentIdUploadRequestBuilder
   */
  public GetDocumentIdUploadRequestBuilder checksum(final String documentChecksum) {
    this.checksum = documentChecksum;
    return this;
  }

  /**
   * Set checksum type.
   *
   * @param type {@link String}
   * @return GetDocumentIdUploadRequestBuilder
   */
  public GetDocumentIdUploadRequestBuilder checksumType(final String type) {
    this.checksumType = type;
    return this;
  }

  /**
   * Set content length.
   *
   * @param length {@link Integer}
   * @return GetDocumentIdUploadRequestBuilder
   */
  public GetDocumentIdUploadRequestBuilder contentLength(final Integer length) {
    this.contentLength = length;
    return this;
  }

  /**
   * Set duration.
   *
   * @param hours {@link Integer}
   * @return GetDocumentIdUploadRequestBuilder
   */
  public GetDocumentIdUploadRequestBuilder duration(final Integer hours) {
    this.duration = hours;
    return this;
  }

  /**
   * Set shareKey.
   * 
   * @param value Parameter value
   * @return this builder
   */
  public GetDocumentIdUploadRequestBuilder shareKey(final String value) {
    this.shareKey = value;
    return this;
  }



  @Override
  public ApiHttpResponse<GetDocumentUrlResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new DocumentsApi(apiClient).getDocumentIdUpload(
        this.document.documentId(), siteId, this.document.artifactId(), this.checksumType,
        this.checksum, this.contentLength, this.duration, this.shareKey, this.accelerate));
  }
}
