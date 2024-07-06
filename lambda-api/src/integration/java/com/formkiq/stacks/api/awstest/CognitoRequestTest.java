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

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.api.DocumentsApi;
import com.formkiq.client.api.UserManagementApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddGroup;
import com.formkiq.client.model.AddGroupRequest;
import com.formkiq.client.model.AddResponse;
import com.formkiq.client.model.AddUser;
import com.formkiq.client.model.AddUserRequest;
import com.formkiq.client.model.DeleteResponse;
import com.formkiq.client.model.GetGroupsResponse;
import com.formkiq.client.model.GetUsersInGroupResponse;
import com.formkiq.client.model.GetUsersResponse;
import com.formkiq.client.model.Group;
import com.formkiq.client.model.SetResponse;
import com.formkiq.client.model.User;
import com.formkiq.testutils.aws.AbstractAwsIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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

  private static void deleteGroup(final UserManagementApi userApi, final String groupName)
      throws ApiException {
    DeleteResponse deleteResponse = userApi.deleteGroup(groupName);
    assertEquals("Group " + groupName + " deleted", deleteResponse.getMessage());
  }

  private static void addGroup(final UserManagementApi userApi, final String groupName)
      throws ApiException {
    // given
    AddGroupRequest req = new AddGroupRequest().group(new AddGroup().name(groupName));

    // when
    AddResponse response = userApi.addGroup(req);

    // then
    assertEquals("Group " + groupName + " created", response.getMessage());
  }

  private static void addUser(final UserManagementApi userApi, final String email)
      throws ApiException {
    // given
    AddUserRequest req = new AddUserRequest().user(new AddUser().username(email));

    // when
    AddResponse response = userApi.addUser(req);

    // then
    assertEquals("user '" + email + "' has been created", response.getMessage());
  }

  /**
   * Test GET /users.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetUsers01() throws Exception {

    // given
    List<ApiClient> clients = getApiClients(null);

    for (ApiClient client : getAdminClients(clients)) {

      UserManagementApi userApi = new UserManagementApi(client);

      // when
      GetUsersResponse response = userApi.getUsers(null, null);

      // then
      List<User> users = notNull(response.getUsers());
      assertFalse(users.isEmpty());

      User user = users.get(0);
      assertNotNull(user.getUsername());
      assertNotNull(user.getUserStatus());
      assertNotNull(user.getEmail());
      assertEquals(Boolean.TRUE, user.getEnabled());
      assertNotNull(user.getInsertedDate());
      assertNotNull(user.getLastModifiedDate());
    }

    // given
    UserManagementApi userApi = new UserManagementApi(clients.get(2));

    // when
    try {
      userApi.getUsers(null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }

  /**
   * Test GET /groups.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetGroups01() throws Exception {

    // given
    List<ApiClient> clients = getApiClients(null);

    for (ApiClient client : clients) {

      UserManagementApi userApi = new UserManagementApi(client);

      // when
      GetGroupsResponse response = userApi.getGroups(null, null);

      // then
      List<Group> groups = notNull(response.getGroups());
      assertFalse(groups.isEmpty());

      Optional<Group> o =
          groups.stream().filter(g -> "admins".equalsIgnoreCase(g.getName())).findFirst();
      assertFalse(o.isEmpty());

      assertNotNull(o.get().getDescription());
      assertNotNull(o.get().getInsertedDate());
      assertNotNull(o.get().getLastModifiedDate());

      Group group = userApi.getGroup(o.get().getName()).getGroup();
      assertNotNull(group);
      assertNotNull(group.getDescription());
      assertNotNull(group.getInsertedDate());
      assertNotNull(group.getLastModifiedDate());
    }
  }

  /**
   * Test GET /groups/{groupName}/users.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testGetGroupUsers01() throws Exception {

    // given
    String groupName = "Admins";
    List<ApiClient> clients = getApiClients(null);

    for (ApiClient client : getAdminClients(clients)) {

      UserManagementApi userApi = new UserManagementApi(client);

      // when
      GetUsersInGroupResponse response = userApi.getUsersInGroup(groupName, null, null);

      // then
      List<User> users = notNull(response.getUsers());
      assertFalse(users.isEmpty());

      User user = users.get(0);

      assertNotNull(user.getUsername());
      assertNotNull(user.getEmail());
      assertNotNull(user.getUserStatus());
      assertNotNull(user.getInsertedDate());
      assertNotNull(user.getLastModifiedDate());
    }

    // given
    UserManagementApi userApi = new UserManagementApi(clients.get(2));

    // when
    try {
      userApi.getUsersInGroup(groupName, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }

  private List<ApiClient> getAdminClients(final List<ApiClient> clients) {
    return Arrays.asList(clients.get(0), clients.get(1));
  }

  /**
   * Test POST /groups.
   *
   * @throws Exception Exception
   */
  @Test
  @Timeout(value = TEST_TIMEOUT)
  public void testAddGroupAndUser01() throws Exception {

    // given
    List<ApiClient> clients = getApiClients(null);
    for (ApiClient client : getAdminClients(clients)) {

      UserManagementApi userApi = new UserManagementApi(client);
      String groupName = "test_" + UUID.randomUUID();
      String email = groupName + "@formkiq.com";

      // when
      addUser(userApi, email);
      addGroup(userApi, groupName);
      addUserToGroup(userApi, email, groupName);

      // then
      List<User> usersInGroup = notNull(userApi.getUsersInGroup(groupName, null, null).getUsers());
      assertEquals(1, usersInGroup.size());
      assertNotNull(usersInGroup.get(0).getUsername());
      assertNotNull(usersInGroup.get(0).getEmail());

      List<Group> groups = notNull(userApi.getListOfUserGroups(email, null, null).getGroups());
      assertEquals(1, groups.size());
      assertEquals(groupName, groups.get(0).getName());

      // when
      disableUser(userApi, email);
      enableUser(userApi, email);
      removeUserFromGroup(userApi, email, groupName);

      // then
      deleteUser(userApi, email);
      deleteGroup(userApi, groupName);
    }

    // given
    UserManagementApi userApi = new UserManagementApi(clients.get(2));
    String groupName = "test_" + UUID.randomUUID();
    AddGroupRequest req = new AddGroupRequest().group(new AddGroup().name(groupName));

    // when
    try {
      userApi.addGroup(req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }

  private void enableUser(final UserManagementApi userApi, final String email) throws ApiException {
    SetResponse enable = userApi.setUserOperation(email, "enable");
    assertEquals("user '" + email + "' has been enabled", enable.getMessage());
  }

  private static void disableUser(final UserManagementApi userApi, final String email)
      throws ApiException {
    SetResponse disable = userApi.setUserOperation(email, "disable");
    assertEquals("user '" + email + "' has been disabled", disable.getMessage());
  }

  private void removeUserFromGroup(final UserManagementApi userApi, final String email,
      final String groupName) throws ApiException {
    DeleteResponse deleteResponse = userApi.removeUsernameFromGroup(groupName, email);
    assertEquals("user '" + email + "' removed from group '" + groupName + "'",
        deleteResponse.getMessage());
  }

  private void addUserToGroup(final UserManagementApi userApi, final String email,
      final String groupName) throws ApiException {
    AddUserRequest req = new AddUserRequest().user(new AddUser().username(email));
    AddResponse addResponse = userApi.addUserToGroup(groupName, req);
    assertEquals("user '" + email + "' added to group '" + groupName + "'",
        addResponse.getMessage());
  }

  private void deleteUser(final UserManagementApi userApi, final String email) throws ApiException {
    DeleteResponse deleteResponse = userApi.deleteUsername(email);
    assertEquals("user '" + email + "' has been deleted", deleteResponse.getMessage());
  }

  /**
   * Test 'authentication_only' cognito group.
   *
   */
  @Test
  public void testAuthenticationOnly() {
    // given
    String username = "noaccess1@formkiq.com";
    addAndLoginCognito(username, List.of("authentication_only"));

    ApiClient jwtClient = getApiClientForUser(username, USER_PASSWORD);

    DocumentsApi api = new DocumentsApi(jwtClient);

    // when
    try {
      api.getDocuments("default", null, null, null, null, null, null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(SC_UNAUTHORIZED.getStatusCode(), e.getCode());
    }
  }
}
