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
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.AddEntityTypeResponse;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link AddEntityTypeRequest}.
 */
public class AddEntityTypeRequestBuilder implements HttpRequestBuilder<AddEntityTypeResponse> {

  /** {@link AddEntityTypeRequest}. */
  private final AddEntityTypeRequest request;

  /**
   * constructor.
   */
  public AddEntityTypeRequestBuilder() {
    this.request = new AddEntityTypeRequest();
  }

  /**
   * Set Attribute Key.
   *
   * @param name {@link String}
   * @param namespace {@link EntityTypeNamespace}
   * @return AddDocumentRequestBuilder
   */
  public AddEntityTypeRequestBuilder setEntityType(final String name,
      final EntityTypeNamespace namespace) {
    this.request.setEntityType(new AddEntityType().name(name).namespace(namespace));
    return this;
  }

  @Override
  public ApiHttpResponse<AddEntityTypeResponse> submit(final ApiClient apiClient,
      final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).addEntityType(this.request, siteId));
  }
}
