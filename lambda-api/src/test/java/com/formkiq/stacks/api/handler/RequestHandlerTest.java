/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.stacks.api.handler;

import static com.formkiq.stacks.api.ApiGatewayRequestEventUtil.getCallingCognitoUsername;
import static org.junit.Assert.assertEquals;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import com.formkiq.stacks.api.ApiGatewayRequestEvent;

/**
 * Unit Tests for {@link RequestHandler}.
 *
 */
public class RequestHandlerTest {

  /**
   * Test cognito:username.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetCallingCognitoUsername01() throws Exception {
    // given
    try (InputStream in =
        this.getClass().getResourceAsStream("/request-patch-documents-documentid01.json")) {
      ApiGatewayRequestEvent event = RequestHandler.GSON.fromJson(
          new InputStreamReader(in, StandardCharsets.UTF_8), ApiGatewayRequestEvent.class);

      // when
      String username = getCallingCognitoUsername(event);

      // then
      assertEquals("8a73dfef-26d3-43d8-87aa-b3ec358e43ba@formkiq.com", username);
    }
  }

  /**
   * Test user assumed role.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetCallingCognitoUsername02() throws Exception {
    // given
    try (InputStream in =
        this.getClass().getResourceAsStream("/request-post-documents-documentid-tags04.json")) {
      ApiGatewayRequestEvent event = RequestHandler.GSON.fromJson(
          new InputStreamReader(in, StandardCharsets.UTF_8), ApiGatewayRequestEvent.class);

      // when
      String username = getCallingCognitoUsername(event);

      // then
      assertEquals(
          "AROAZB6IP7U6SDBIQTEUX:formkiq-docstack-unittest-api-ApiGatewayInvokeRole-IKJY8XKB0IUK",
          username);
    }
  }

  /**
   * Test IAM Credentials as part of Group.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetCallingCognitoUsername03() throws Exception {
    // given
    try (InputStream in =
        this.getClass().getResourceAsStream("/request-post-documents-documentid-tags05.json")) {
      ApiGatewayRequestEvent event = RequestHandler.GSON.fromJson(
          new InputStreamReader(in, StandardCharsets.UTF_8), ApiGatewayRequestEvent.class);

      // when
      String username = getCallingCognitoUsername(event);

      // then
      assertEquals("arn:aws:iam::111111111111:user/mike", username);
    }
  }

  /**
   * Test Cognito User.
   * 
   * @throws Exception Exception
   */
  @Test
  public void testGetCallingCognitoUsername04() throws Exception {
    // given
    try (InputStream in = this.getClass().getResourceAsStream("/request-get-documents02.json")) {
      ApiGatewayRequestEvent event = RequestHandler.GSON.fromJson(
          new InputStreamReader(in, StandardCharsets.UTF_8), ApiGatewayRequestEvent.class);

      // when
      String username = getCallingCognitoUsername(event);

      // then
      assertEquals("demo@formkiq.com", username);
    }
  }
}
