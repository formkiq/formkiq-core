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

import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.UserManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.GetGroupsResponse;
import com.formkiq.client.model.GetUsersInGroupResponse;
import com.formkiq.client.model.Group;
import com.formkiq.client.model.User;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;

/**
 * Process Urls.
 * <p>
 * GET /groups, /groups/{groupName}/users tests
 * </p>
 *
 */
public class CognitoRequestTest extends AbstractAwsIntegrationTest {

  /** JUnit Test Timeout. */
  private static final int TEST_TIMEOUT = 20;

  /**
   * Test GET /groups.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGetGroups01() throws Exception {

    // given
    List<ApiClient> clients = getApiClients(null);

    for (ApiClient client : clients) {

      UserManagementApi userApi = new UserManagementApi(client);

      // when
      GetGroupsResponse response = userApi.getGroups(null, null);

      // then
      assertFalse(response.getGroups().isEmpty());

      Optional<Group> o = response.getGroups().stream()
          .filter(g -> g.getName().equalsIgnoreCase("admins")).findFirst();
      assertFalse(o.isEmpty());

      assertNotNull(o.get().getDescription());
      assertNotNull(o.get().getInsertedDate());
      assertNotNull(o.get().getLastModifiedDate());
    }
  }

  /**
   * Test GET /groups/{groupName}/users.
   * 
   * @throws Exception Exception
   */
  @Test
  @Timeout(unit = TimeUnit.SECONDS, value = TEST_TIMEOUT)
  public void testGetGroupUsers01() throws Exception {

    // given
    List<ApiClient> clients = getApiClients(null);

    for (ApiClient client : clients) {

      UserManagementApi userApi = new UserManagementApi(client);
      String groupName = "Admins";

      // when
      GetUsersInGroupResponse response = userApi.getUsersInGroup(groupName, null, null);

      // then
      assertFalse(response.getUsers().isEmpty());

      User user = response.getUsers().get(0);

      assertNotNull(user.getUsername());
      assertNotNull(user.getUserStatus());
      assertNotNull(user.getInsertedDate());
      assertNotNull(user.getLastModifiedDate());
    }
  }

  /**
   * Test 'authentication_only' cognito group.
   * 
   * @throws ApiException ApiException
   */
  @Test
  public void testAuthenticationOnly() throws ApiException {
    // given
    String username = "noaccess1@formkiq.com";
    addAndLoginCognito(username, Arrays.asList("authentication_only"));

    ApiClient jwtClient = getApiClientForUser(username, USER_PASSWORD);

    DocumentsApi api = new DocumentsApi(jwtClient);

    // when
    try {
      api.getDocuments(null, null, null, null, null, null, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(SC_FORBIDDEN.getStatusCode(), e.getCode());
    }
  }
}
