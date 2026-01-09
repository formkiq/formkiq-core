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
package com.formkiq.testutils.api.schemas;

import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddAttributeSchemaOptional;
import com.formkiq.client.model.AddAttributeSchemaRequired;
import com.formkiq.client.model.AddClassification;
import com.formkiq.client.model.SetClassificationRequest;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.SetSchemaAttributes;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for {@link SetClassificationRequest}.
 */
public class SetClassificationRequestBuilder implements HttpRequestBuilder<SetResponse> {
  /** {@link SetClassificationRequest}. */
  private final SetClassificationRequest request = new SetClassificationRequest();
  /** {@link SetSchemaAttributes}. */
  private final SetSchemaAttributes schemaAttributes = new SetSchemaAttributes();
  /** Classification. */
  private final String classification;

  /**
   * constructor.
   * 
   * @param classificationId {@link String}
   * @param name {@link String}
   */
  public SetClassificationRequestBuilder(final String classificationId, final String name) {
    this.classification = classificationId;
    AddClassification addClassification = new AddClassification();
    addClassification.setName(name);

    addClassification.setAttributes(schemaAttributes);
    request.setClassification(addClassification);
  }

  /**
   * Add Optional Attribute.
   *
   * @param attributeKey {@link String}
   * @return SetDocumentAttributesRequestBuilder
   */
  public SetClassificationRequestBuilder addOptionalAttribute(final String attributeKey) {
    schemaAttributes.addOptionalItem(new AddAttributeSchemaOptional().attributeKey(attributeKey));
    return this;
  }

  /**
   * Add Required Attribute.
   *
   * @param attributeKey {@link String}
   * @return SetDocumentAttributesRequestBuilder
   */
  public SetClassificationRequestBuilder addRequiredAttribute(final String attributeKey) {
    schemaAttributes.addRequiredItem(new AddAttributeSchemaRequired().attributeKey(attributeKey));
    return this;
  }

  /**
   * Optionally run the request using the FormKiQ API.
   *
   * @param apiClient ApiClient
   * @param siteId Site ID
   * @return SetResponse
   */
  public ApiHttpResponse<SetResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(
        () -> new SchemasApi(apiClient).setClassification(siteId, classification, request));
  }
}
