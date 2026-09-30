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
import com.formkiq.testutils.api.users.AddGroupsRequestBuilder;
import com.formkiq.testutils.api.users.GetGroupUsersRequestBuilder;
import com.formkiq.testutils.api.users.GetUserRequestBuilder;
import com.formkiq.testutils.api.users.GetUsersRequestBuilder;
import com.formkiq.testutils.api.users.AddUserRequestBuilder;
import com.formkiq.testutils.api.users.AddUserToGroupRequestBuilder;
import com.formkiq.testutils.api.users.DeleteGroupRequestBuilder;
import com.formkiq.testutils.api.users.DeleteUsernameRequestBuilder;
import com.formkiq.testutils.api.users.GetListOfUserGroupsRequestBuilder;
import com.formkiq.testutils.api.users.RemoveUsernameFromGroupRequestBuilder;
import com.formkiq.testutils.api.users.SetUserOperationRequestBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Unit Tests for request /groups and /users. */
public class CognitoRequestTest extends AbstractApiClientRequestTest {

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
      new AddUserToGroupRequestBuilder().withGroupName("test").withAddUserRequest(req)
          .submitOk(this.client, null);
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
      new AddGroupsRequestBuilder(req.getGroup().getName()).submitOk(this.client, null);
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
      new AddUserRequestBuilder().withAddUserRequest(req).submitOk(this.client, null);
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
      new DeleteGroupRequestBuilder().withGroupName("test").submitOk(this.client, null);
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
      new DeleteUsernameRequestBuilder().withUsername("test").submitOk(this.client, null);
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
      new RemoveUsernameFromGroupRequestBuilder().withGroupName("group").withUsername("test")
          .submitOk(this.client, null);
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
      new GetGroupUsersRequestBuilder("test").limit(null).next(null).submitOk(this.client, null);
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
      new GetUserRequestBuilder("test").submitOk(this.client, null);
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
      new GetUsersRequestBuilder().limit(null).next(null).submitOk(this.client, null);
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
      new SetUserOperationRequestBuilder().withUsername("test").withUserOperation("fsdf")
          .submitOk(this.client, null);
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
      new GetListOfUserGroupsRequestBuilder().withUsername("test").withLimit(null).withNext(null)
          .submitOk(this.client, null);
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
