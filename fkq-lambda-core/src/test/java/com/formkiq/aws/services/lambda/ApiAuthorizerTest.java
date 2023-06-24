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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 
 * Unit Test for {@link ApiAuthorizer}.
 *
 */
class ApiAuthorizerTest {

  /**
   * Basic 'default'/SiteId access.
   */
  @Test
  void testApiAuthorizer01() throws Exception {
    // given
    for (ApiAuthorizerType type : Arrays.asList(ApiAuthorizerType.COGNITO,
        ApiAuthorizerType.SAML)) {
      String s0 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_default]" : "[default]";
      String s1 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_finance]" : "[finance]";

      ApiGatewayRequestEvent event0 = getEvent(s0);
      ApiGatewayRequestEvent event1 = getEvent(s1);

      // when
      final ApiAuthorizer api0 = new ApiAuthorizer(event0, type);
      final ApiAuthorizer api1 = new ApiAuthorizer(event1, type);

      // then
      assertFalse(api0.isUserReadAccess());
      assertTrue(api0.isUserWriteAccess());
      assertFalse(api0.isCallerAssumeRole());
      assertFalse(api0.isCallerIamUser());
      assertFalse(api0.isUserAdmin());
      assertNull(api0.getSiteId());
      assertEquals("groups: default", api0.accessSummary());

      assertFalse(api1.isUserReadAccess());
      assertTrue(api1.isUserWriteAccess());
      assertFalse(api1.isCallerAssumeRole());
      assertFalse(api1.isCallerIamUser());
      assertFalse(api1.isUserAdmin());
      assertEquals("groups: finance", api1.accessSummary());
      assertEquals("finance", api1.getSiteId());
    }
  }

  /**
   * Basic 'default_read' access.
   */
  @Test
  void testApiAuthorizer02() throws Exception {
    // given
    for (ApiAuthorizerType type : Arrays.asList(ApiAuthorizerType.COGNITO,
        ApiAuthorizerType.SAML)) {
      String s0 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_default_read]" : "[default_read]";
      String s1 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_finance_read]" : "[finance_read]";
      ApiGatewayRequestEvent event0 = getEvent(s0);
      ApiGatewayRequestEvent event1 = getEvent(s1);

      // when
      final ApiAuthorizer api0 = new ApiAuthorizer(event0, type);
      final ApiAuthorizer api1 = new ApiAuthorizer(event1, type);

      // then
      assertTrue(api0.isUserReadAccess());
      assertFalse(api0.isUserWriteAccess());
      assertFalse(api0.isCallerAssumeRole());
      assertFalse(api0.isCallerIamUser());
      assertFalse(api0.isUserAdmin());
      assertNull(api0.getSiteId());
      assertEquals("groups: default_read", api0.accessSummary());

      assertTrue(api1.isUserReadAccess());
      assertFalse(api1.isUserWriteAccess());
      assertFalse(api1.isCallerAssumeRole());
      assertFalse(api1.isCallerIamUser());
      assertFalse(api1.isUserAdmin());
      assertEquals("finance", api1.getSiteId());
      assertEquals("groups: finance_read", api1.accessSummary());
    }
  }

  /**
   * Basic 'Admin' access.
   */
  @Test
  void testApiAuthorizer03() throws Exception {
    // given
    for (ApiAuthorizerType type : Arrays.asList(ApiAuthorizerType.COGNITO,
        ApiAuthorizerType.SAML)) {
      String s0 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_Admins formkiq_default]"
          : "[Admins default]";
      String s1 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_Admins formkiq_finance"
          : "[Admins finance]";
      ApiGatewayRequestEvent event0 = getEvent(s0);
      ApiGatewayRequestEvent event1 = getEvent(s1);

      // when
      final ApiAuthorizer api0 = new ApiAuthorizer(event0, type);
      final ApiAuthorizer api1 = new ApiAuthorizer(event1, type);

      // then
      assertFalse(api0.isUserReadAccess());
      assertTrue(api0.isUserWriteAccess());
      assertFalse(api0.isCallerAssumeRole());
      assertFalse(api0.isCallerIamUser());
      assertTrue(api0.isUserAdmin());
      assertNull(api0.getSiteId());
      assertEquals("groups: Admins,default", api0.accessSummary());

      assertFalse(api1.isUserReadAccess());
      assertFalse(api1.isUserWriteAccess());
      assertFalse(api1.isCallerAssumeRole());
      assertFalse(api1.isCallerIamUser());
      assertTrue(api1.isUserAdmin());
      assertNull(api1.getSiteId());
      assertEquals("groups: Admins,finance", api1.accessSummary());
    }
  }

  /**
   * Basic 'admin' access.
   */
  @Test
  void testApiAuthorizer04() throws Exception {
    // given
    for (ApiAuthorizerType type : Arrays.asList(ApiAuthorizerType.COGNITO,
        ApiAuthorizerType.SAML)) {
      String s0 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_admins formkiq_default]"
          : "[admins default]";
      String s1 = ApiAuthorizerType.SAML.equals(type) ? "[formkiq_admins formkiq_finance"
          : "[admins finance]";
      ApiGatewayRequestEvent event0 = getEvent(s0);
      ApiGatewayRequestEvent event1 = getEvent(s1);

      // when
      final ApiAuthorizer api0 = new ApiAuthorizer(event0, type);
      final ApiAuthorizer api1 = new ApiAuthorizer(event1, type);

      // then
      assertFalse(api0.isUserReadAccess());
      assertTrue(api0.isUserWriteAccess());
      assertFalse(api0.isCallerAssumeRole());
      assertFalse(api0.isCallerIamUser());
      assertTrue(api0.isUserAdmin());
      assertNull(api0.getSiteId());
      assertEquals("groups: admins,default", api0.accessSummary());

      assertFalse(api1.isUserReadAccess());
      assertFalse(api1.isUserWriteAccess());
      assertFalse(api1.isCallerAssumeRole());
      assertFalse(api1.isCallerIamUser());
      assertTrue(api1.isUserAdmin());
      assertNull(api1.getSiteId());
      assertEquals("groups: admins,finance", api1.accessSummary());
    }
  }

  /**
   * Get {@link ApiGatewayRequestEvent}.
   * 
   * @param group {@link String}
   * @return {@link ApiGatewayRequestEvent}
   */
  private ApiGatewayRequestEvent getEvent(final String group) {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    ApiGatewayRequestContext content = new ApiGatewayRequestContext();
    content.setAuthorizer(Map.of("claims", Map.of("cognito:groups", group)));
    event.setRequestContext(content);
    return event;
  }

}
