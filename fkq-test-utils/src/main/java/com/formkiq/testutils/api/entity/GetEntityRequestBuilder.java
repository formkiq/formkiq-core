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

import com.formkiq.aws.dynamodb.documentattributes.DocumentAttributeEntityKeyValue;
import com.formkiq.client.api.EntityApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntityResponse;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.HttpRequestBuilder;

/**
 * Builder for Get Entity.
 */
public class GetEntityRequestBuilder implements HttpRequestBuilder<GetEntityResponse> {

  /** Entity Type Id. */
  private final String entityType;
  /** {@link EntityTypeNamespace}. */
  private final EntityTypeNamespace namespace;
  /** Entity. */
  private final String entity;

  /**
   * constructor.
   * 
   * @param entityTypeId {@link String}
   * @param entityId {@link String}
   * @param entityTypeNamespace {@link EntityTypeNamespace}
   */
  public GetEntityRequestBuilder(final String entityTypeId, final String entityId,
      final EntityTypeNamespace entityTypeNamespace) {
    this.entityType = entityTypeId;
    this.entity = entityId;
    this.namespace = entityTypeNamespace;
  }

  /**
   * constructor.
   * 
   * @param entityKey {@link DocumentAttributeEntityKeyValue}
   * @param entityTypeNamespace {@link EntityTypeNamespace}
   */
  public GetEntityRequestBuilder(final DocumentAttributeEntityKeyValue entityKey,
      final EntityTypeNamespace entityTypeNamespace) {
    this.entityType = entityKey.entityTypeId();
    this.entity = entityKey.entityId();
    this.namespace = entityTypeNamespace;
  }

  @Override
  public ApiHttpResponse<GetEntityResponse> submit(final ApiClient apiClient, final String siteId) {
    return executeApiCall(() -> new EntityApi(apiClient).getEntity(this.entityType, this.entity,
        siteId, namespace.name()));
  }
}
