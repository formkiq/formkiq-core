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
package com.formkiq.testutils.api.esignature;

import com.formkiq.client.api.ESignatureApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.DocusignEnvironment;
import com.formkiq.client.model.VoidDocusignEnvelopeRequest;
import com.formkiq.client.model.VoidDocusignEnvelopeResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the VoidDocusignEnvelope API operation.
 */
public class VoidDocusignEnvelopeRequestBuilder
    implements HttpRequestBuilder<VoidDocusignEnvelopeResponse> {

  /** Request parameter. */
  private String documentId;

  /** Request parameter. */
  private String envelopeId;

  /** Request parameter. */
  private VoidDocusignEnvelopeRequest voidDocusignEnvelopeRequest;

  /** Request parameter. */
  private String artifactId;

  /** Request parameter. */
  private DocusignEnvironment environment;

  @Override
  public ApiHttpResponse<VoidDocusignEnvelopeResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(
        () -> new ESignatureApi(apiClient).voidDocusignEnvelope(this.documentId, this.envelopeId,
            this.voidDocusignEnvelopeRequest, siteId, this.artifactId, this.environment));
  }

  /**
   * Set artifactId.
   *
   * @param value String
   * @return this builder
   */
  public VoidDocusignEnvelopeRequestBuilder withArtifactId(final String value) {
    this.artifactId = value;
    return this;
  }

  /**
   * Set documentId.
   *
   * @param value String
   * @return this builder
   */
  public VoidDocusignEnvelopeRequestBuilder withDocumentId(final String value) {
    this.documentId = value;
    return this;
  }

  /**
   * Set envelopeId.
   *
   * @param value String
   * @return this builder
   */
  public VoidDocusignEnvelopeRequestBuilder withEnvelopeId(final String value) {
    this.envelopeId = value;
    return this;
  }

  /**
   * Set environment.
   *
   * @param value DocusignEnvironment
   * @return this builder
   */
  public VoidDocusignEnvelopeRequestBuilder withEnvironment(final DocusignEnvironment value) {
    this.environment = value;
    return this;
  }

  /**
   * Set voidDocusignEnvelopeRequest.
   *
   * @param value VoidDocusignEnvelopeRequest
   * @return this builder
   */
  public VoidDocusignEnvelopeRequestBuilder withVoidDocusignEnvelopeRequest(
      final VoidDocusignEnvelopeRequest value) {
    this.voidDocusignEnvelopeRequest = value;
    return this;
  }
}
