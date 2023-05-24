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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import com.formkiq.testutils.aws.LocalStackExtension;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * Unit Tests {@link ApiKeyAuthorizerRequestHandler}.
 *
 */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
class ApiKeyAuthorizerRequestHandlerTest {

  /** {@link ApiKeyAuthorizerRequestHandler}. */
  private static ApiKeyAuthorizerRequestHandler processor;
  /** {@link ApiKeysService}. */
  private static ApiKeysService apiKeysService;
  /** {@link Gson}. */
  private static final Gson GSON = new GsonBuilder().create();

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

  /**
   * Test S3 File doesn't exist.
   * 
   * @throws Exception Exception
   */
  @Test
  void testHandleRequest01() throws Exception {
    // given
    for (String siteId : Arrays.asList("default", UUID.randomUUID().toString())) {

      String name = UUID.randomUUID().toString();

      apiKeysService.createApiKey(siteId, name);

      // SqsMessageRecord record = new SqsMessageRecord().body(GSON.toJson(Map.of("siteId", siteId,
      // "documentId", documentId, "jobId", jobId, "contentType", MimeType.MIME_JPEG)));
      // SqsMessageRecords records = new SqsMessageRecords().records(Arrays.asList(record));

      String json = GSON.toJson(Map.of());
      InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      // when
      processor.handleRequest(is, null, this.context);

      // then
    }
  }
}
