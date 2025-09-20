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
package com.formkiq.aws.services.lambda;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.dynamodb.ApiPermission;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

/**
 * 
 * Unit Test for {@link ApiAuthorization}.
 *
 */
class ApiAuthorizationBuilderTest {

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * Get {@link ApiGatewayRequestEvent}.
   *
   * @param groups {@link List} {@link String}
   * @param permissions {@link Map}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getExplicitSitesJwtEvent(final List<String> groups,
      final Map<String, List<String>> permissions) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();

    Map<String, Object> claims = Map.of("username", "oktaidp_test@formkiq.com", "sitesClaims", gson
        .toJson(Map.of("cognito:groups", String.join(" ", groups), "permissionsMap", permissions)));
    content.setAuthorizer(Map.of("claims", claims));
    event.setRequestContext(content);

    return event;
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   *
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getApiKeyClaimsEvent() {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();

    Map<String, Object> claims = Map.of("cognito:groups", "[apitestsite API_KEY]",
        "cognito:username", "Test Read/Write/Delete Key", "permissions", "DELETE,READ,WRITE");

    Map<String, Object> authorizer = new HashMap<>(Map.of("apiKeyClaims", claims));
    authorizer.put("claims", null);
    content.setAuthorizer(authorizer);
    event.setRequestContext(content);

    return event;
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   *
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getApiKeyJwtEvent(final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();
    content.setAuthorizer(
        Map.of("apiKeyClaims", Map.of("permissions", "read,write", "cognito:groups", group)));
    event.setRequestContext(content);
    return event;
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   * 
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getJwtEvent(final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();
    content.setAuthorizer(
        Map.of("claims", Map.of("cognito:username", "myuser", "cognito:groups", group)));
    event.setRequestContext(content);
    return event;
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   * 
   * @param userArn {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getUserArnEvent(final String userArn) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();
    content.setIdentity(Map.of("userArn", userArn));
    event.setRequestContext(content);
    return event;
  }

  /**
   * Basic 'default'/SiteId access.
   */
  @Test
  void testApiAuthorizer01() throws Exception {
    // given
    String s0 = "[default]";
    String s1 = "[finance]";

    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE,DELETE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getRoles()));
    assertEquals("myuser", api0.getUsername());

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("READ,WRITE,DELETE",
        api1.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE)", api1.getAccessSummary());
    assertEquals("finance", String.join(",", api1.getRoles()));
    assertEquals("myuser", api1.getUsername());
  }

  /**
   * Basic 'default_read' access.
   */
  @Test
  void testApiAuthorizer02() throws Exception {
    // given
    String s0 = "[default_read]";
    String s1 = "[finance_read]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ)", api0.getAccessSummary());
    assertEquals("default_read", String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("READ",
        api1.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (READ)", api1.getAccessSummary());
    assertEquals("finance_read", String.join(",", api1.getRoles()));
  }

  /**
   * Basic 'Admin' access.
   */
  @Test
  void testApiAuthorizer03() throws Exception {
    // given
    String s0 = "[Admins default]";
    String s1 = "[Admins finance]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: default (ADMIN,DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("default,Admins", String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (ADMIN,DELETE,READ,WRITE)", api1.getAccessSummary());
    assertEquals("Admins,finance", String.join(",", api1.getRoles()));
  }

  /**
   * Basic 'admin' access.
   */
  @Test
  void testApiAuthorizer04() throws Exception {
    // given
    String s0 = "[admins default]";
    String s1 = "[admins finance]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: default (ADMIN,DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("default,admins", String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("ADMIN,DELETE,READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (ADMIN,DELETE,READ,WRITE)", api1.getAccessSummary());
    assertEquals("admins,finance", String.join(",", api1.getRoles()));
  }

  /**
   * Multiple SiteId access with 'siteId' query.
   */
  @Test
  void testApiAuthorizer05() throws Exception {
    // given
    String s0 = "[default other]";
    String s1 = "[finance other]";

    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    event0.setQueryStringParameters(Map.of("siteId", "other"));

    ApiGatewayRequestEvent event1 = getJwtEvent(s1);
    event1.setQueryStringParameters(Map.of("siteId", "other"));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals("other", api0.getSiteId());
    assertEquals("default,other", String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api0.getAccessSummary());
    assertEquals("default,other", String.join(",", api0.getRoles()));

    assertEquals("other", api1.getSiteId());
    assertEquals("finance,other", String.join(",", api1.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api1.getAccessSummary());
    assertEquals("other,finance", String.join(",", api1.getRoles()));
  }

  /**
   * Multiple SiteId access without 'siteId' query.
   */
  @Test
  void testApiAuthorizer06() throws Exception {
    // given
    String s0 = "[default other]";

    ApiGatewayRequestEvent event = getJwtEvent(s0);

    // when
    ApiAuthorization api = new ApiAuthorizationBuilder().build(event);

    // then
    assertNull(api.getSiteId());
    assertEquals("default,other", String.join(",", api.getSiteIds()));
    assertEquals("READ",
        api.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("DELETE,READ,WRITE", api.getPermissions(DEFAULT_SITE_ID).stream().map(Enum::name)
        .sorted().collect(Collectors.joining(",")));
    assertEquals("DELETE,READ,WRITE", api.getPermissions("other").stream().map(Enum::name).sorted()
        .collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api.getAccessSummary());
    assertEquals("default,other", String.join(",", api.getRoles()));
  }

  /**
   * UserArn assume-role / user:.
   */
  @Test
  void testApiAuthorizer07() throws Exception {
    // given
    String userArn0 = "arn:aws:sts::111111111111:assumed-role/formkiqIUK/ApiGatewayInvokeRole";
    String userArn1 = "arn:aws:iam::1111111111111111:user/" + UUID.randomUUID();

    for (String userArn : Arrays.asList(userArn0, userArn1)) {

      ApiGatewayRequestEvent event0 = getUserArnEvent(userArn);

      // when
      ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

      // then
      assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
      assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
      assertEquals("ADMIN,DELETE,READ,WRITE",
          api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
      assertEquals("groups: default (ADMIN,DELETE,READ,WRITE)", api0.getAccessSummary());
      assertEquals("", String.join(",", api0.getRoles()));

      // given
      ApiGatewayRequestEvent event1 = getUserArnEvent(userArn);
      event1.setQueryStringParameters(Map.of("siteId", "finance"));

      // when
      ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

      // then
      assertEquals("finance", api1.getSiteId());
      assertEquals("finance", String.join(",", api1.getSiteIds()));
      assertEquals("ADMIN,DELETE,READ,WRITE",
          api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
      assertEquals("groups: finance (ADMIN,DELETE,READ,WRITE)", api1.getAccessSummary());
      assertEquals("", String.join(",", api1.getRoles()));
    }
  }

  /**
   * Invalid UserArn.
   */
  @Test
  void testApiAuthorizer08() throws Exception {
    // given
    String userArn = "arn:aws:sts::111111111111:another/formkiqIUK/ApiGatewayInvokeRole";

    ApiGatewayRequestEvent event = getUserArnEvent(userArn);

    // when
    ApiAuthorization api = new ApiAuthorizationBuilder().build(event);

    // then
    assertNull(api.getSiteId());
    assertEquals("", String.join(",", api.getSiteIds()));
    assertEquals("",
        api.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api.getAccessSummary());
    assertEquals("", String.join(",", api.getRoles()));
  }

  /**
   * multiple groups.
   */
  @Test
  void testApiAuthorizer09() throws Exception {
    // given
    String s = "[finance other]";

    ApiGatewayRequestEvent event = getJwtEvent(s);
    event.setQueryStringParameters(Map.of("siteId", "finance"));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event);

    // then
    assertEquals("finance", api0.getSiteId());
    assertEquals("finance,other", String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api0.getAccessSummary());
    assertEquals("other,finance", String.join(",", api0.getRoles()));
  }

  /**
   * User has 'default' & 'default_read'.
   */
  @Test
  void testApiAuthorizer10() throws Exception {
    // given
    String s0 = "[default default_read]";
    String s1 = "[finance finance_read]";

    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE", api0.getPermissions().stream().map(Enum::name)
        .sorted(String::compareTo).collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("default,default_read", String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE)", api1.getAccessSummary());
    assertEquals("finance,finance_read", String.join(",", api1.getRoles()));
  }

  /**
   * Basic API Key 'default'/SiteId access.
   */
  @Test
  void testApiAuthorizer11() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getApiKeyJwtEvent(DEFAULT_SITE_ID);
    ApiGatewayRequestEvent event1 = getApiKeyJwtEvent("finance");

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ,WRITE)", api0.getAccessSummary());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (READ,WRITE)", api1.getAccessSummary());
    assertEquals("finance", String.join(",", api1.getRoles()));
  }

  /**
   * Basic 'authentication_only' access.
   */
  @Test
  void testApiAuthorizer12() throws Exception {
    // given
    String s0 = "[authentication_only]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);

    // when
    ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api0.getAccessSummary());
    assertEquals("authentication_only", String.join(",", api0.getRoles()));
  }

  /**
   * Basic 'authentication_only' access.
   */
  @Test
  void testApiAuthorizer13() throws Exception {
    // given
    String s0 = "[finance authentication_only]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);

    // when
    ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals("finance", api0.getSiteId());
    assertEquals("finance", String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE,DELETE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("authentication_only,finance", String.join(",", api0.getRoles()));
  }

  /**
   * Basic 'Admins' only access.
   */
  @Test
  void testApiAuthorizer14() throws Exception {
    // given
    String s0 = "[Admins]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);

    // when
    ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE,DELETE,ADMIN",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (ADMIN,DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("Admins", String.join(",", api0.getRoles()));
  }

  /**
   * Basic 'Admins' only access.
   */
  @Test
  void testApiAuthorizerAdminsWithRandomSiteId() throws Exception {
    // given
    String s0 = "[Admins]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    event0.setPath("/random");
    event0.setQueryStringParameters(Map.of("siteId", "global"));

    // when
    ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals("global", api0.getSiteId());
    assertEquals("", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api0.getAccessSummary());
    assertEquals("Admins", String.join(",", api0.getRoles()));
  }

  /**
   * Empty groups access.
   */
  @Test
  void testApiAuthorizer15() throws Exception {
    // given
    String s0 = "[]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);

    // when
    ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api0.getAccessSummary());
    assertEquals("", String.join(",", api0.getRoles()));
  }

  /**
   * 'Admin' access across multiple sites.
   */
  @Test
  void testApiAuthorizer16() throws Exception {
    // given
    String s0 = "[Admins default sample]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);

    // when
    final ApiAuthorization api = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api.getSiteId());
    assertEquals("default,sample", String.join(",", api.getSiteIds()));
    assertEquals("ADMIN,DELETE,READ,WRITE", api.getPermissions(DEFAULT_SITE_ID).stream()
        .map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("ADMIN,DELETE,READ,WRITE", api.getPermissions("sample").stream().map(Enum::name)
        .sorted().collect(Collectors.joining(",")));
    assertEquals("ADMIN,DELETE,GOVERN,READ,WRITE", api.getPermissions("another").stream()
        .map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("ADMIN,DELETE,GOVERN,READ,WRITE",
        api.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: default (ADMIN,DELETE,READ,WRITE), sample (ADMIN,DELETE,READ,WRITE)",
        api.getAccessSummary());
    assertEquals("default,Admins,sample", String.join(",", api.getRoles()));
  }

  /**
   * multiple group with path parameter.
   */
  @Test
  void testApiAuthorizer17() throws Exception {
    // given
    String s = "[finance other]";

    ApiGatewayRequestEvent event = getJwtEvent(s);
    event.setPathParameters(Map.of("siteId", "finance"));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event);

    // then
    assertEquals("finance", api0.getSiteId());
    assertEquals("finance,other", String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api0.getAccessSummary());
    assertEquals("other,finance", String.join(",", api0.getRoles()));
  }

  /**
   * Multiple SiteId access with 'siteId' path parameter.
   */
  @Test
  void testApiAuthorizer18() throws Exception {
    // given
    String s0 = "[default other]";
    String s1 = "[finance other]";

    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    event0.setPathParameters(Map.of("siteId", "other"));

    ApiGatewayRequestEvent event1 = getJwtEvent(s1);
    event1.setPathParameters(Map.of("siteId", "other"));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals("other", api0.getSiteId());
    assertEquals("default,other", String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api0.getAccessSummary());
    assertEquals("default,other", String.join(",", api0.getRoles()));

    assertEquals("other", api1.getSiteId());
    assertEquals("finance,other", String.join(",", api1.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api1.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE), other (DELETE,READ,WRITE)",
        api1.getAccessSummary());
    assertEquals("other,finance", String.join(",", api1.getRoles()));
  }

  /**
   * Sites permissionsMap.
   */
  @Test
  void testApiAuthorizer19() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getExplicitSitesJwtEvent(List.of(DEFAULT_SITE_ID, "test"),
        Map.of(DEFAULT_SITE_ID, List.of(ApiPermission.READ.name()), "test",
            List.of(ApiPermission.WRITE.name(), ApiPermission.DELETE.name())));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("default,test", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ", api0.getPermissions(DEFAULT_SITE_ID).stream().map(Enum::name)
        .collect(Collectors.joining(",")));
    assertEquals("WRITE,DELETE",
        api0.getPermissions("test").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ), test (DELETE,WRITE)", api0.getAccessSummary());
    assertEquals("default,test", String.join(",", api0.getRoles()));
    assertEquals("oktaidp_test@formkiq.com", api0.getUsername());
  }

  /**
   * Sites permissionsMap missing permission for site.
   */
  @Test
  void testApiAuthorizer20() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getExplicitSitesJwtEvent(List.of(DEFAULT_SITE_ID, "test"),
        Map.of(DEFAULT_SITE_ID, List.of(ApiPermission.READ.name())));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ", api0.getPermissions(DEFAULT_SITE_ID).stream().map(Enum::name)
        .collect(Collectors.joining(",")));
    assertEquals("READ",
        api0.getPermissions("test").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ)", api0.getAccessSummary());
    assertEquals("default,test", String.join(",", api0.getRoles()));
  }

  /**
   * Sites permissionsMap invalid permission for site.
   */
  @Test
  void testApiAuthorizer21() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getExplicitSitesJwtEvent(List.of(DEFAULT_SITE_ID, "test"),
        Map.of(DEFAULT_SITE_ID, List.of("READ", "invalid")));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ", api0.getPermissions(DEFAULT_SITE_ID).stream().map(Enum::name)
        .collect(Collectors.joining(",")));
    assertEquals("READ",
        api0.getPermissions("test").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ)", api0.getAccessSummary());
    assertEquals("default,test", String.join(",", api0.getRoles()));
  }

  /**
   * Sites permissionsMap with Admins.
   */
  @Test
  void testApiAuthorizer22() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getExplicitSitesJwtEvent(List.of("Admins"), Map.of());

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE,DELETE,ADMIN",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ,WRITE,DELETE,ADMIN", api0.getPermissions(DEFAULT_SITE_ID).stream()
        .map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("ADMIN,DELETE,READ,WRITE,GOVERN",
        api0.getPermissions("test").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (ADMIN,DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("Admins", String.join(",", api0.getRoles()));
  }

  /**
   * Sites permissionsMap with user.
   */
  @Test
  void testApiAuthorizer23() throws Exception {
    // given
    ApiGatewayRequestEvent event0 =
        getExplicitSitesJwtEvent(List.of("student", "test", "joe"), Map.of("student",
            List.of("read", "write"), "test", List.of("delete"), "joe", List.of("govern")));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("joe,student,test", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ,WRITE",
        api0.getPermissions("student").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("DELETE",
        api0.getPermissions("test").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("GOVERN",
        api0.getPermissions("joe").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: joe (GOVERN), student (READ,WRITE), test (DELETE)",
        api0.getAccessSummary());
    assertEquals("joe,test,student", String.join(",", api0.getRoles()));
  }

  /**
   * Basic 'default_govern' access.
   */
  @Test
  void testApiAuthorizer24() throws Exception {
    // given
    String s0 = "[default_govern]";
    String s1 = "[finance_govern]";
    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("GOVERN,READ,WRITE,DELETE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,GOVERN,READ,WRITE)", api0.getAccessSummary());
    assertEquals("default_govern", String.join(",", api0.getRoles()));

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("GOVERN,READ,WRITE,DELETE",
        api1.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,GOVERN,READ,WRITE)", api1.getAccessSummary());
    assertEquals("finance_govern", String.join(",", api1.getRoles()));
  }

  /**
   * Sites permissionsMap with user in default group.
   */
  @Test
  void testApiAuthorizer25() throws Exception {
    // given
    ApiGatewayRequestEvent event0 =
        getExplicitSitesJwtEvent(List.of("default"), Map.of("default", List.of("read")));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals("default", api0.getSiteId());
    assertEquals("default", String.join(",", api0.getSiteIds()));
    assertEquals("READ",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ",
        api0.getPermissions("default").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (READ)", api0.getAccessSummary());
    assertEquals("default", String.join(",", api0.getRoles()));
  }

  /**
   * Sites permissionsMap with 'global'.
   */
  @Test
  void testApiAuthorizerGlobal() throws Exception {
    // given
    ApiGatewayRequestEvent event0 =
        getExplicitSitesJwtEvent(List.of("global"), Map.of("global", List.of("read")));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("",
        api0.getPermissions("global").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api0.getAccessSummary());
    assertEquals("global", String.join(",", api0.getRoles()));
    assertEquals("oktaidp_test@formkiq.com", api0.getUsername());
  }

  /**
   * Sites permissionsMap with 'API_KEY'.
   */
  @Test
  void testApiAuthorizerApiKeyExplicit() throws Exception {
    // given
    ApiGatewayRequestEvent event0 =
        getExplicitSitesJwtEvent(List.of("API_KEY"), Map.of("API_KEY", List.of("read")));

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertNull(api0.getSiteId());
    assertEquals("", String.join(",", api0.getSiteIds()));
    assertEquals("",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("",
        api0.getPermissions("global").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("no groups", api0.getAccessSummary());
    assertEquals("API_KEY", String.join(",", api0.getRoles()));
    assertEquals("oktaidp_test@formkiq.com", api0.getUsername());
  }

  /**
   * Sites permissionsMap with 'API_KEY' with apiKeyClaims.
   */
  @Test
  void testApiAuthorizerApiKeyExplicitNoUsername() throws Exception {
    // given
    ApiGatewayRequestEvent event0 = getApiKeyClaimsEvent();

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);

    // then
    assertEquals("apitestsite", api0.getSiteId());
    assertEquals("apitestsite", String.join(",", api0.getSiteIds()));
    assertEquals("DELETE,READ,WRITE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("READ",
        api0.getPermissions("global").stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: apitestsite (DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals("API_KEY,apitestsite", String.join(",", api0.getRoles()));
    assertEquals("Test Read/Write/Delete Key", api0.getUsername());
  }

  /**
   * Basic 'default'/SiteId access with API_KEY.
   */
  @Test
  void testApiAuthorizerApiKeyAutomatic() throws Exception {
    // given
    String s0 = "[default API_KEY]";
    String s1 = "[finance API_KEY]";

    ApiGatewayRequestEvent event0 = getJwtEvent(s0);
    ApiGatewayRequestEvent event1 = getJwtEvent(s1);

    // when
    final ApiAuthorization api0 = new ApiAuthorizationBuilder().build(event0);
    final ApiAuthorization api1 = new ApiAuthorizationBuilder().build(event1);

    // then
    assertEquals(DEFAULT_SITE_ID, api0.getSiteId());
    assertEquals(DEFAULT_SITE_ID, String.join(",", api0.getSiteIds()));
    assertEquals("READ,WRITE,DELETE",
        api0.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: default (DELETE,READ,WRITE)", api0.getAccessSummary());
    assertEquals(DEFAULT_SITE_ID + ",API_KEY", String.join(",", api0.getRoles()));
    assertEquals("myuser", api0.getUsername());

    assertEquals("finance", api1.getSiteId());
    assertEquals("finance", String.join(",", api1.getSiteIds()));
    assertEquals("READ,WRITE,DELETE",
        api1.getPermissions().stream().map(Enum::name).collect(Collectors.joining(",")));
    assertEquals("groups: finance (DELETE,READ,WRITE)", api1.getAccessSummary());
    assertEquals("API_KEY,finance", String.join(",", api1.getRoles()));
    assertEquals("myuser", api1.getUsername());
  }
}
