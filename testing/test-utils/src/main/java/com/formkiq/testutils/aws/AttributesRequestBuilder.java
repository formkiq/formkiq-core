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
package com.formkiq.testutils.aws;

import com.formkiq.client.api.AttributesApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddAttribute;
import com.formkiq.client.model.AddAttributeRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;
import com.formkiq.client.model.Watermark;

/**
 * Attribute Request Builder.
 */
public class AttributesRequestBuilder {

  /**
   * Add Entity Type, skip if already exists.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @param watermarkText {@link String}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static AddResponse addAttributeWatermark(final ApiClient client, final String siteId,
      final String attributeKey, final String watermarkText) throws ApiException {

    AttributesApi api = new AttributesApi(client);
    Watermark watermark = new Watermark().text(watermarkText);
    AddAttributeRequest req = new AddAttributeRequest()
        .attribute(new AddAttribute().key(attributeKey).dataType(AttributeDataType.WATERMARK)
            .type(AttributeType.STANDARD).watermark(watermark));
    return api.addAttribute(req, siteId);
  }
}
