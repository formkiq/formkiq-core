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
package com.formkiq.testutils.api.attributes;

import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link AddAttributeRequest}.
 */
public class AddAttributeRequestBuilder implements HttpRequestBuilder {

  /** {@link AddAttributeRequest}. */
  private final AddAttributeRequest request;

  /**
   * constructor.
   */
  public AddAttributeRequestBuilder() {
    this.request = new AddAttributeRequest();
  }

  /**
   * Set Attribute Key.
   * 
   * @param attributeKey {@link String}
   * @return AddDocumentRequestBuilder
   */
  public AddAttributeRequestBuilder keyAsString(final String attributeKey) {
    this.request.setAttribute(new AddAttribute().key(attributeKey)
        .dataType(AttributeDataType.STRING).type(AttributeType.STANDARD));
    return this;
  }

  @Override
  public ApiHttpResponse<AddResponse> submit(final ApiClient apiClient, final String siteId) {

    AddResponse obj = null;
    ApiException ex = null;
    try {
      obj = new AttributesApi(apiClient).addAttribute(this.request, siteId);
    } catch (ApiException e) {
      ex = e;
    }
    return new ApiHttpResponse<>(obj, ex);
  }
}
