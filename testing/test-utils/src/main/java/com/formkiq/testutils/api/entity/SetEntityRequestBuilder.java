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
package com.formkiq.testutils.api.entity;

import com.formkiq.client.api.EntityApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.SetEntityRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the SetEntity API operation.
 */
public class SetEntityRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** Request parameter. */
  private String entityTypeId;

  /** Request parameter. */
  private String entityId;

  /** Request parameter. */
  private SetEntityRequest setEntityRequest;

  /** Request parameter. */
  private String namespace;

  /** Request parameter. */
  private Boolean createIfMissing;

  @Override
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).setEntity(this.entityTypeId, this.entityId,
        this.setEntityRequest, siteId, this.namespace, this.createIfMissing));
  }

  /**
   * Set createIfMissing.
   *
   * @param value Boolean
   * @return this builder
   */
  public SetEntityRequestBuilder withCreateIfMissing(final Boolean value) {
    this.createIfMissing = value;
    return this;
  }

  /**
   * Set entityId.
   *
   * @param value String
   * @return this builder
   */
  public SetEntityRequestBuilder withEntityId(final String value) {
    this.entityId = value;
    return this;
  }

  /**
   * Set entityTypeId.
   *
   * @param value String
   * @return this builder
   */
  public SetEntityRequestBuilder withEntityTypeId(final String value) {
    this.entityTypeId = value;
    return this;
  }

  /**
   * Set namespace.
   *
   * @param value String
   * @return this builder
   */
  public SetEntityRequestBuilder withNamespace(final String value) {
    this.namespace = value;
    return this;
  }

  /**
   * Set setEntityRequest.
   *
   * @param value SetEntityRequest
   * @return this builder
   */
  public SetEntityRequestBuilder withSetEntityRequest(final SetEntityRequest value) {
    this.setEntityRequest = value;
    return this;
  }
}
