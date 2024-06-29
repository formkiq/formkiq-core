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
package com.formkiq.stacks.api.handler;

import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AddGroup;
import com.formkiq.client.model.AddGroupRequest;
import com.formkiq.client.model.AddUser;
import com.formkiq.client.model.AddUserRequest;
import com.formkiq.testutils.aws.LocalStackExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /groups and /users. */
@ExtendWith(LocalStackExtension.class)
public class CognitoRequestTest extends AbstractApiClientRequestTest {

  /**
   * POST /groups only allowed by admin.
   *
   */
  @Test
  public void testAddGroups01() {
    // given
    AddGroupRequest req = new AddGroupRequest().group(new AddGroup().name("test"));

    setBearerToken("security");

    // when
    try {
      this.userManagementApi.addGroup(req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * GET /groups/{groupName}/users only allowed by admin.
   *
   */
  @Test
  public void testGetGroupUsers01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.getUsersInGroup("test", null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * POST /groups/{groupName}/users only allowed by admin.
   *
   */
  @Test
  public void testAddGroupUsers01() {
    // given
    setBearerToken("security");
    AddUserRequest req = new AddUserRequest().user(new AddUser().username("test"));

    // when
    try {
      this.userManagementApi.addUserToGroup("test", req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * DELETE /groups/{groupName}/users/{username} only allowed by admin.
   *
   */
  @Test
  public void testDeleteUserFromGroup01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.removeUsernameFromGroup("group", "test");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * GET /users only allowed by admin.
   *
   */
  @Test
  public void testGetUsers01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.getUsers(null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * POST /users only allowed by admin.
   *
   */
  @Test
  public void testAddUsers01() {
    // given
    setBearerToken("security");

    AddUserRequest req = new AddUserRequest().user(new AddUser().username("test"));

    // when
    try {
      this.userManagementApi.addUser(req);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * GET /users/{username} only allowed by admin.
   *
   */
  @Test
  public void testGetUser01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.getUser("test");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * DELETE /users/{username} only allowed by admin.
   *
   */
  @Test
  public void testDeleteUser01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.deleteUsername("test");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * GET /users/{username}/groups only allowed by admin.
   *
   */
  @Test
  public void testUsersGroups01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.getListOfUserGroups("test", null, null);
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * PUT /users/{username}/{userOperation} only allowed by admin.
   *
   */
  @Test
  public void testPutUserOperation01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.setUserOperation("test", "fsdf");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }

  /**
   * DELETE /groups/{groupName} only allowed by admin.
   *
   */
  @Test
  public void testDeleteGroup01() {
    // given
    setBearerToken("security");

    // when
    try {
      this.userManagementApi.deleteGroup("test");
      fail();
    } catch (ApiException e) {
      // then
      assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), e.getCode());
      assertEquals(
          "{\"message\":\"fkq access denied " + "(groups: security (DELETE,READ,WRITE))\"}",
          e.getResponseBody());
    }
  }
}
