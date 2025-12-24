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
package com.formkiq.module.lambda.authorizer.apikey;

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.formkiq.aws.dynamodb.ID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.DynamoDbAwsServiceRegistry;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.AwsServiceCacheBuilder;
import com.formkiq.stacks.dynamodb.ApiKeyPermission;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.formkiq.testutils.aws.TestServices;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * Unit Tests {@link ApiKeyAuthorizerRequestHandler}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class ApiKeyAuthorizerRequestHandlerTest {

  /** {@link ApiKeysService}. */
  private static ApiKeysService apiKeysService;
  /** {@link ApiKeyAuthorizerRequestHandler}. */
  private static ApiKeyAuthorizerRequestHandler processor;

  /**
   * Before Class.
   *
   */
  @BeforeAll
  public static void beforeClass() {

    Map<String, String> env = new HashMap<>();
    env.put("AWS_REGION", Region.US_EAST_1.id());
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);

    AwsCredentials creds = AwsBasicCredentials.create("aaa", "bbb");
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(creds);

    AwsServiceCache awsServices =
        new AwsServiceCacheBuilder(env, TestServices.getEndpointMap(), credentialsProvider)
            .addService(new DynamoDbAwsServiceRegistry()).build();

    processor = new ApiKeyAuthorizerRequestHandler(awsServices);
    apiKeysService = awsServices.getExtension(ApiKeysService.class);
  }

  /** {@link Context}. */
  private final Context context = new LambdaContextRecorder();

  private APIGatewayV2CustomAuthorizerEvent getInput(final String apiKey) {
    List<String> identitySource = apiKey != null ? List.of(apiKey) : Collections.emptyList();
    return APIGatewayV2CustomAuthorizerEvent.builder().withType("REQUEST")
        .withIdentitySource(identitySource).build();
  }

  /**
   * Test Invalid API Key.
   *
   */
  @Test
  void testInvalidApiKey() {
    // given
    String apiKey = ID.uuid();

    APIGatewayV2CustomAuthorizerEvent is = getInput(apiKey);

    // when
    Map<String, Object> map = processor.handleRequest(is, this.context);

    // then
    assertEquals(Boolean.FALSE, map.get("isAuthorized"));
    Map<String, Object> ctx = (Map<String, Object>) map.get("context");
    Map<String, Object> claims = (Map<String, Object>) ctx.get("apiKeyClaims");
    assertEquals("[API_KEY]", claims.get("cognito:groups"));
    assertEquals("", claims.get("cognito:username"));
    assertEquals("", claims.get("permissions"));
  }

  /**
   * Test Invalid very long API Key.
   *
   */
  @Test
  void testInvalidVeryLongApiKey() {
    // given
    final int len = 2000;
    String apiKey = Strings.generateRandomString(len);

    APIGatewayV2CustomAuthorizerEvent is = getInput(apiKey);

    // when
    Map<String, Object> map = processor.handleRequest(is, this.context);

    // then
    assertEquals(Boolean.FALSE, map.get("isAuthorized"));
    Map<String, Object> ctx = (Map<String, Object>) map.get("context");
    Map<String, Object> claims = (Map<String, Object>) ctx.get("apiKeyClaims");
    assertEquals("[API_KEY]", claims.get("cognito:groups"));
    assertEquals("", claims.get("cognito:username"));
    assertEquals("", claims.get("permissions"));
  }

  /**
   * Test missing API Key.
   *
   */
  @Test
  void testMissingApiKey() {
    // given
    APIGatewayV2CustomAuthorizerEvent is = getInput(null);

    // when
    Map<String, Object> map = processor.handleRequest(is, this.context);

    // then
    assertEquals(Boolean.FALSE, map.get("isAuthorized"));

    Map<String, Object> ctx = (Map<String, Object>) map.get("context");
    Map<String, Object> claims = (Map<String, Object>) ctx.get("apiKeyClaims");
    assertEquals("[API_KEY]", claims.get("cognito:groups"));
    assertEquals("", claims.get("cognito:username"));
    assertEquals("", claims.get("permissions"));
  }

  /**
   * Test VALID API Key.
   *
   */
  @Test
  void testValidApiKey() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String name = ID.uuid();

      String apiKey = apiKeysService.createApiKey(siteId, name,
          Arrays.asList(ApiKeyPermission.READ, ApiKeyPermission.WRITE, ApiKeyPermission.DELETE),
          List.of());

      APIGatewayV2CustomAuthorizerEvent is = getInput(apiKey);

      // when
      Map<String, Object> map = processor.handleRequest(is, this.context);

      // then
      assertEquals(Boolean.TRUE, map.get("isAuthorized"));

      Map<String, Object> ctx = (Map<String, Object>) map.get("context");
      Map<String, Object> claims = (Map<String, Object>) ctx.get("apiKeyClaims");

      if (siteId != null) {
        assertEquals("[" + siteId + " API_KEY]", claims.get("cognito:groups"));
      } else {
        assertEquals("[default API_KEY]", claims.get("cognito:groups"));
      }

      assertEquals(name, claims.get("cognito:username"));
      assertEquals("DELETE,READ,WRITE", claims.get("permissions"));
    }
  }

  /**
   * Test VALID API Key with groups.
   *
   */
  @Test
  void testValidApiKeyWithGroups() {
    // given
    for (String siteId : Arrays.asList(null, ID.uuid())) {

      String name = ID.uuid();

      String apiKey = apiKeysService.createApiKey(siteId, name,
          Arrays.asList(ApiKeyPermission.READ, ApiKeyPermission.WRITE, ApiKeyPermission.DELETE),
          List.of("test1", "test2"));

      APIGatewayV2CustomAuthorizerEvent is = getInput(apiKey);

      // when
      Map<String, Object> map = processor.handleRequest(is, this.context);

      // then
      assertEquals(Boolean.TRUE, map.get("isAuthorized"));

      Map<String, Object> ctx = (Map<String, Object>) map.get("context");
      Map<String, Object> claims = (Map<String, Object>) ctx.get("apiKeyClaims");

      if (siteId != null) {
        assertEquals("[" + siteId + " test1 test2 API_KEY]", claims.get("cognito:groups"));
      } else {
        assertEquals("[default test1 test2 API_KEY]", claims.get("cognito:groups"));
      }

      assertEquals(name, claims.get("cognito:username"));
      assertEquals("DELETE,READ,WRITE", claims.get("permissions"));
    }
  }
}
