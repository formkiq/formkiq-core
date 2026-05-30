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

import com.formkiq.client.api.EntityApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddEntity;
import com.formkiq.client.model.AddEntityRequest;
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.EntityType;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntityTypeResponse;

import java.util.Optional;

/**
 * Entity Request Builder.
 */
public class EntityRequestBuilder {

  /**
   * Add Entity.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param entityTypeId Entity Type Id
   * @param entityName Entity Name
   * @param namespace Entity Namespace
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addEntity(final ApiClient client, final String siteId,
      final String entityTypeId, final String entityName, final String namespace)
      throws ApiException {
    EntityApi api = new EntityApi(client);
    return api.addEntity(entityTypeId,
        new AddEntityRequest().entity(new AddEntity().name(entityName)), siteId, namespace)
        .getEntityId();
  }

  /**
   * Add Entity Type, skip if already exists.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param name {@link String}
   * @return {@link String}
   * @throws ApiException ApiException
   */
  public static String addEntityType(final ApiClient client, final String siteId, final String name)
      throws ApiException {
    String entityTypeId;
    EntityApi api = new EntityApi(client);
    EntityTypeNamespace namespace = EntityTypeNamespace.CUSTOM;

    try {
      entityTypeId = api
          .addEntityType(new AddEntityTypeRequest()
              .entityType(new AddEntityType().namespace(namespace).name(name)), siteId)
          .getEntityTypeId();
    } catch (ApiException e) {
      if (!e.getResponseBody().contains("already exists")) {
        throw e;
      }

      EntityType entityType =
          Optional.ofNullable(getEntityType(client, siteId, name, namespace.name())).orElseThrow();
      entityTypeId = entityType.getEntityTypeId();
    }

    return entityTypeId;
  }

  /**
   * Add Entity Type, skip if already exists.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param entityTypeId {@link String}
   * @param namespace {@link String}
   * @return {@link EntityType}
   * @throws ApiException ApiException
   */
  public static EntityType getEntityType(final ApiClient client, final String siteId,
      final String entityTypeId, final String namespace) throws ApiException {
    EntityApi api = new EntityApi(client);
    GetEntityTypeResponse entityType = api.getEntityType(entityTypeId, siteId, namespace);
    return entityType.getEntityType();
  }
}
