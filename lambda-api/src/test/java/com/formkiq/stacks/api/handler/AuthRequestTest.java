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

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.services.lambda.ApiResponseStatus;
import com.formkiq.testutils.api.ApiHttpResponse;
import com.formkiq.testutils.api.SetBearers;
import com.formkiq.testutils.api.SitesRequestRequestBuilder;
import com.formkiq.testutils.api.VersionRequestRequestBuilder;
import com.formkiq.testutils.api.documents.AddDocumentRequestBuilder;
import com.formkiq.testutils.api.documents.DeleteDocumentPurgeRequestBuilder;
import com.formkiq.testutils.api.documents.SearchDocumentRequestBuilder;
import com.formkiq.testutils.api.users.GetGroupRequestBuilder;
import com.formkiq.testutils.api.users.GetGroupUsersRequestBuilder;
import com.formkiq.testutils.api.users.GetGroupsRequestBuilder;
import com.formkiq.testutils.api.users.GetUserGroupsRequestBuilder;
import com.formkiq.testutils.api.users.GetUserRequestBuilder;
import com.formkiq.testutils.api.users.GetUsersRequestBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit Tests for Auth. */
public class AuthRequestTest extends AbstractApiClientRequestTest {

  private static void assertFail(final ApiHttpResponse<?> resp) {
    assertTrue(resp.isError());
    assertNull(resp.response());
  }

  private static void assertOk(final ApiHttpResponse<?> resp) {
    assertFalse(resp.isError());
    assertNotNull(resp.response());
  }

  private static void assertValidCognitoRequest(final ApiHttpResponse<?> resp) {
    assertNotEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), resp.exception().getCode());
    assertTrue(resp.is5XX()); // fails to access cognito, ok
  }

  private void assertDocumentAddFail(final String siteId) {
    var resp = new AddDocumentRequestBuilder().content().submit(client, siteId);
    assertTrue(resp.isError());
    assertEquals(ApiResponseStatus.SC_UNAUTHORIZED.getStatusCode(), resp.exception().getCode());
    assertTrue(resp.exception().getResponseBody().contains("fkq access denied"));
    assertNull(resp.response());
  }

  private String assertDocumentAddOk(final String siteId) {
    var resp = new AddDocumentRequestBuilder().content().submit(client, siteId);
    assertOk(resp);
    return resp.response().getDocumentId();
  }

  private void assertDocumentPurgeOk(final String siteId, final String documentId) {
    var resp = new DeleteDocumentPurgeRequestBuilder(documentId).submit(client, siteId);
    assertOk(resp);
  }

  private void assertDocumentSearchOk(final String siteId) {
    var resp = new SearchDocumentRequestBuilder().folder("").submit(client, siteId);
    assertOk(resp);
  }

  /**
   * Test siteId admin access.
   */
  @Test
  public void testAdmins() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearers().apply(client, new String[] {"Admins"});
      assertDocumentSearchOk(siteId);
      String documentId = assertDocumentAddOk(siteId);
      assertDocumentPurgeOk(siteId, documentId);
    }
  }

  /**
   * Test siteId cognito group access.
   */
  @Test
  public void testCognitoGroupAccess() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearers().apply(client, new String[] {siteId});
      assertDocumentSearchOk(siteId);
      assertDocumentAddOk(siteId);
    }
  }

  /**
   * Test siteId govern access.
   */
  @Test
  public void testGovern() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearers().apply(client, new String[] {siteId + "_govern"});
      assertDocumentSearchOk(siteId);
      String documentId = assertDocumentAddOk(siteId);
      assertDocumentPurgeOk(siteId, documentId);
    }
  }

  /**
   * Test GET /groups with multiple roles.
   */
  @Test
  public void testMultipleRoles() {
    new SetBearers().apply(client, new String[] {"manager", "finance"});
    assertValidCognitoRequest(new GetGroupsRequestBuilder().submit(client, null));
    assertValidCognitoRequest(new GetGroupRequestBuilder("name").submit(client, null));
    assertFail(new GetGroupUsersRequestBuilder("name").submit(client, null));
    assertFail(new GetUsersRequestBuilder().submit(client, null));
    assertFail(new GetUserRequestBuilder("joe").submit(client, null));
    assertFail(new GetUserGroupsRequestBuilder("joe").submit(client, null));
    assertOk(new VersionRequestRequestBuilder().submit(client, null));
    assertOk(new SitesRequestRequestBuilder().submit(client, null));
  }

  /**
   * Test siteId_read access.
   */
  @Test
  public void testReadOnly() {
    // given
    for (String siteId : Arrays.asList(DEFAULT_SITE_ID, ID.uuid())) {
      new SetBearers().apply(client, new String[] {siteId + "_read"});
      assertDocumentSearchOk(siteId);
      assertDocumentAddFail(siteId);
    }
  }
}
