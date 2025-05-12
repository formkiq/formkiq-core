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
package com.formkiq.stacks.api.awstest;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.client.api.EntityApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.model.AddEntity;
import com.formkiq.client.model.AddEntityRequest;
import com.formkiq.client.model.AddEntityResponse;
import com.formkiq.client.model.AddEntityType;
import com.formkiq.client.model.AddEntityTypeRequest;
import com.formkiq.client.model.AddEntityTypeResponse;
import com.formkiq.client.model.Entity;
import com.formkiq.client.model.EntityType;
import com.formkiq.client.model.EntityTypeNamespace;
import com.formkiq.client.model.GetEntityResponse;
import com.formkiq.client.model.GetEntityTypeResponse;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Process Urls.
 * <p>
 * /entityTypes/{entityTypeId} tests /entities/{entityTypeId} tests
 * </p>
 *
 */
public class EntityRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test GET /version.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testAddEntity01() throws Exception {
    // given
    String siteId = ID.uuid();
    for (ApiClient apiClient : getApiClients(siteId)) {

      EntityApi entityApi = new EntityApi(apiClient);
      AddEntityTypeRequest req = new AddEntityTypeRequest()
          .entityType(new AddEntityType().name("Company").namespace(EntityTypeNamespace.CUSTOM));

      // when
      AddEntityTypeResponse response = entityApi.addEntityType(req, siteId);

      // then
      String entityTypeId = response.getEntityTypeId();
      assertNotNull(entityTypeId);

      // given
      AddEntityRequest addEntityReq =
          new AddEntityRequest().entity(new AddEntity().name("Acme Inc"));

      // when
      AddEntityResponse addEntityResponse =
          entityApi.addEntity(response.getEntityTypeId(), addEntityReq, siteId, null);

      // then
      String entityId = addEntityResponse.getEntityId();
      assertNotNull(entityId);

      GetEntityTypeResponse entityType = entityApi.getEntityType(entityTypeId, siteId, null);
      assertEntityType(entityType.getEntityType());

      GetEntityResponse entity = entityApi.getEntity(entityTypeId, entityId, siteId, null);
      assertEntity(entity.getEntity());
    }
  }

  private void assertEntity(final Entity entity) {
    assertNotNull(entity);
    assertNotNull(entity.getEntityTypeId());
    assertNotNull(entity.getEntityId());
    assertNotNull(entity.getInsertedDate());
    assertEquals("Acme Inc", entity.getName());
  }

  private void assertEntityType(final EntityType entityType) {
    assertNotNull(entityType);
    assertNotNull(entityType.getEntityTypeId());
    assertNotNull(entityType.getInsertedDate());
    assertEquals("Company", entityType.getName());
    assertEquals(EntityTypeNamespace.CUSTOM, entityType.getNamespace());
  }
}
