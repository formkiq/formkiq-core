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
import com.formkiq.client.model.UpdateAttribute;
import com.formkiq.client.model.UpdateAttributeRequest;
import com.formkiq.client.model.UpdateResponse;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.Watermark;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link UpdateAttributeRequest}.
 */
public class UpdateAttributeRequestBuilder implements HttpRequestBuilder<UpdateResponse> {

  /** {@link UpdateAttributeRequest}. */
  private final UpdateAttributeRequest request;
  /** Attribute Key. */
  private final String attribute;
  /** {@link Watermark}. */
  private Watermark watermark = null;
  /** {@link UpdateAttribute}. */
  private final UpdateAttribute updateAttribute = new UpdateAttribute();

  /**
   * constructor.
   * 
   * @param attributeKey {@link String}
   */
  public UpdateAttributeRequestBuilder(final String attributeKey) {
    this.attribute = attributeKey;
    this.request = new UpdateAttributeRequest().attribute(updateAttribute);
  }

  private void initWatermark() {
    if (watermark == null) {
      watermark = new Watermark();
    }
  }

  /**
   * Set Attribute Type.
   *
   * @param attributeType {@link AttributeType}
   * @return UpdateAttributeRequestBuilder
   */
  public UpdateAttributeRequestBuilder setType(final AttributeType attributeType) {
    updateAttribute.setType(attributeType);
    return this;
  }

  /**
   * Set Watermark Text.
   *
   * @param watermarkText {@link AttributeType}
   * @return UpdateAttributeRequestBuilder
   */
  public UpdateAttributeRequestBuilder setWatermarkText(final String watermarkText) {
    initWatermark();
    watermark.setText(watermarkText);
    updateAttribute.setWatermark(watermark);
    return this;
  }


  @Override
  public ApiHttpResponse<UpdateResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(
        () -> new AttributesApi(apiClient).updateAttribute(this.attribute, this.request, siteId));
  }
}
