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
import com.formkiq.client.model.SetEntityTypeRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for the SetEntityType API operation.
 */
public class SetEntityTypeRequestBuilder implements HttpRequestBuilder<SetResponse> {

  /** Request parameter. */
  private String entityTypeId;

  /** Request parameter. */
  private SetEntityTypeRequest setEntityTypeRequest;

  /** Request parameter. */
  private Boolean createIfMissing;

  @Override
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).setEntityType(this.entityTypeId,
        this.setEntityTypeRequest, siteId, this.createIfMissing));
  }

  /**
   * Set createIfMissing.
   *
   * @param value Boolean
   * @return this builder
   */
  public SetEntityTypeRequestBuilder withCreateIfMissing(final Boolean value) {
    this.createIfMissing = value;
    return this;
  }

  /**
   * Set entityTypeId.
   *
   * @param value String
   * @return this builder
   */
  public SetEntityTypeRequestBuilder withEntityTypeId(final String value) {
    this.entityTypeId = value;
    return this;
  }

  /**
   * Set setEntityTypeRequest.
   *
   * @param value SetEntityTypeRequest
   * @return this builder
   */
  public SetEntityTypeRequestBuilder withSetEntityTypeRequest(final SetEntityTypeRequest value) {
    this.setEntityTypeRequest = value;
    return this;
  }
}
