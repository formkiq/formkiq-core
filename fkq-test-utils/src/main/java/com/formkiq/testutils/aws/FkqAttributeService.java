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
import com.formkiq.client.model.AddDocumentAttribute;
import com.formkiq.client.model.AddDocumentAttributeStandard;
import com.formkiq.client.model.Attribute;
import com.formkiq.client.model.AttributeDataType;
import com.formkiq.client.model.AttributeType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 
 * FormKiQ Attribute Service.
 *
 */
public class FkqAttributeService {

  /**
   * Add Attribute.
   * 
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @param attributeDataType {@link AttributeDataType}
   * @param attributeType {@link AttributeType}
   * @throws ApiException ApiException
   */
  public static void addAttribute(final ApiClient client, final String siteId,
      final String attributeKey, final AttributeDataType attributeDataType,
      final AttributeType attributeType) throws ApiException {

    AttributesApi attributesApi = new AttributesApi(client);

    attributesApi.addAttribute(
        new AddAttributeRequest().attribute(
            new AddAttribute().key(attributeKey).dataType(attributeDataType).type(attributeType)),
        siteId);
  }

  /**
   * Create Number {@link AddDocumentAttribute}.
   * 
   * @param key {@link String}
   * @param numberValue {@link BigDecimal}
   * @return {@link AddDocumentAttribute}
   */
  public static AddDocumentAttribute createNumberAttribute(final String key,
      final BigDecimal numberValue) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).numberValue(numberValue));
  }

  /**
   * Create Numbers {@link AddDocumentAttribute}.
   * 
   * @param key {@link String}
   * @param numberValues {@link List} {@link BigDecimal}
   * @return {@link AddDocumentAttribute}
   */
  public static AddDocumentAttribute createNumbersAttribute(final String key,
      final List<BigDecimal> numberValues) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).numberValues(numberValues));
  }

  /**
   * Create String {@link AddDocumentAttribute}.
   * 
   * @param key {@link String}
   * @param stringValue {@link String}
   * @return {@link AddDocumentAttribute}
   */
  public static AddDocumentAttribute createStringAttribute(final String key,
      final String stringValue) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).stringValue(stringValue));
  }


  /**
   * Create Strings {@link AddDocumentAttribute}.
   * 
   * @param key {@link String}
   * @param stringValues {@link List} {@link String}
   * @return {@link AddDocumentAttribute}
   */
  public static AddDocumentAttribute createStringsAttribute(final String key,
      final List<String> stringValues) {
    return new AddDocumentAttribute(
        new AddDocumentAttributeStandard().key(key).stringValues(stringValues));
  }

  /**
   * Add Attribute.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param attributeKey {@link String}
   * @return Attribute
   * @throws ApiException ApiException
   */
  public static Attribute getAttribute(final ApiClient client, final String siteId,
      final String attributeKey) throws ApiException {
    AttributesApi attributesApi = new AttributesApi(client);
    return attributesApi.getAttribute(attributeKey, siteId).getAttribute();
  }
}
