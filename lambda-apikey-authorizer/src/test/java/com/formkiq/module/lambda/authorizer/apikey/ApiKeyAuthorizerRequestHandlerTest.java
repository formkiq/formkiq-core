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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.dynamodb.ApiKeysService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LambdaContextRecorder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Unit Tests {@link ApiKeyAuthorizerRequestHandler}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
class ApiKeyAuthorizerRequestHandlerTest {

  /** {@link ApiKeysService}. */
  private static ApiKeysService apiKeysService;
  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();
  /** {@link ApiKeyAuthorizerRequestHandler}. */
  private static ApiKeyAuthorizerRequestHandler processor;

  @BeforeAll
  public static void beforeAll() throws Exception {

    DynamoDbConnectionBuilder dbConnection = DynamoDbTestServices.getDynamoDbConnection();

    processor = new ApiKeyAuthorizerRequestHandler(Map.of("DOCUMENTS_TABLE", DOCUMENTS_TABLE),
        dbConnection);

    AwsServiceCache awsServices = processor.getAwsServices();
    apiKeysService = awsServices.getExtension(ApiKeysService.class);
  }

  /** {@link Context}. */
  private Context context = new LambdaContextRecorder();

  private InputStream getInput(final String siteId, final String apiKey) {
    Map<String, String> queryParameters = siteId != null ? Map.of("siteId", siteId) : Map.of();
    List<String> identitySource = apiKey != null ? Arrays.asList(apiKey) : Collections.emptyList();
    String json = GSON.toJson(Map.of("type", "REQUEST", "identitySource", identitySource,
        "queryStringParameters", queryParameters));

    InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    return is;
  }

  /**
   * Test Invalid API Key.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testHandleRequest01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, "default", UUID.randomUUID().toString())) {

      String apiKey = UUID.randomUUID().toString();

      try (InputStream is = getInput(siteId, apiKey)) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // when
        processor.handleRequest(is, os, this.context);

        // then
        String response = new String(os.toByteArray(), "UTF-8");
        Map<String, Object> map = GSON.fromJson(response, Map.class);
        assertEquals(Boolean.FALSE, map.get("isAuthorized"));
      }
    }
  }

  /**
   * Test VALID API Key.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testHandleRequest02() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      String apiKey = apiKeysService.createApiKey(siteId, name);

      try (InputStream is = getInput(siteId, apiKey)) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // when
        processor.handleRequest(is, os, this.context);

        // then
        String response = new String(os.toByteArray(), "UTF-8");
        Map<String, Object> map = GSON.fromJson(response, Map.class);
        assertEquals(Boolean.TRUE, map.get("isAuthorized"));
      }
    }
  }

  /**
   * Test missing API Key.
   * 
   * @throws Exception Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  void testHandleRequest03() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      try (InputStream is = getInput(siteId, null)) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // when
        processor.handleRequest(is, os, this.context);

        // then
        String response = new String(os.toByteArray(), "UTF-8");
        Map<String, Object> map = GSON.fromJson(response, Map.class);
        assertEquals(Boolean.FALSE, map.get("isAuthorized"));
      }
    }
  }
}
