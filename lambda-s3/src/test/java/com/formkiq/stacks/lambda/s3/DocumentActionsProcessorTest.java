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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsMap;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.ssm.SsmConnectionBuilder;
import com.formkiq.aws.ssm.SsmService;
import com.formkiq.aws.ssm.SsmServiceCache;
import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionType;
import com.formkiq.module.actions.services.ActionsService;
import com.formkiq.module.actions.services.ActionsServiceDynamoDb;
import com.formkiq.stacks.lambda.s3.util.LambdaContextRecorder;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.regions.Region;

/** Unit Tests for {@link DocumentActionsProcessor}. */
@ExtendWith(DynamoDbExtension.class)
@ExtendWith(LocalStackExtension.class)
public class DocumentActionsProcessorTest implements DbKeys {

  /** {@link ActionsService}. */
  private static ActionsService actionsService;
  /** App Environment. */
  private static final String APP_ENVIRONMENT = "test";
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbBuilder;

  /** {@link Gson}. */
  private static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  /** {@link ClientAndServer}. */
  private static ClientAndServer mockServer;

  /** Port to run Test server. */
  private static final int PORT = 8080;
  /** {@link DocumentActionsProcessor}. */
  private static DocumentActionsProcessor processor;
  /** Test server URL. */
  private static final String URL = "http://localhost:" + PORT;
  /** {@link RequestRecordExpectationResponseCallback}. */
  private static RequestRecordExpectationResponseCallback callback =
      new RequestRecordExpectationResponseCallback();

  /**
   * After Class.
   * 
   */
  @AfterAll
  public static void afterClass() {
    mockServer.stop();
  }

  /**
   * Before Class.
   * 
   * @throws URISyntaxException URISyntaxException
   * @throws InterruptedException InterruptedException
   * @throws IOException IOException
   */
  @BeforeAll
  public static void beforeClass() throws URISyntaxException, InterruptedException, IOException {

    dbBuilder = DynamoDbTestServices.getDynamoDbConnection(null);

    actionsService = new ActionsServiceDynamoDb(dbBuilder, DOCUMENTS_TABLE);
    createMockServer();

    SsmConnectionBuilder ssmBuilder = TestServices.getSsmConnection();
    SsmService ssmService = new SsmServiceCache(ssmBuilder, 1, TimeUnit.DAYS);
    ssmService.putParameter("/formkiq/" + APP_ENVIRONMENT + "/api/DocumentsIamUrl", URL);

    Map<String, String> env = new HashMap<>();
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("APP_ENVIRONMENT", APP_ENVIRONMENT);

    processor = new DocumentActionsProcessor(env, Region.US_EAST_1, null, dbBuilder,
        TestServices.getSsmConnection());
  }

  /**
   * Create Mock Server.
   */
  private static void createMockServer() {

    mockServer = startClientAndServer(Integer.valueOf(PORT));

    mockServer.when(request().withMethod("POST")).respond(callback);
  }

  /** {@link LambdaContextRecorder}. */
  private LambdaContextRecorder context;

  /**
   * before.
   */
  @BeforeEach
  public void before() {
    this.context = new LambdaContextRecorder();
  }

  /**
   * Test converting Ocr Parse Types.
   */
  @Test
  public void testGetOcrParseTypes01() {
    assertEquals("[TEXT]", processor.getOcrParseTypes(new Action()).toString());

    // invalid
    Map<String, String> parameters = Map.of("parseTypes", "ADAD,IUJK");
    assertEquals("[TEXT]",
        processor.getOcrParseTypes(new Action().parameters(parameters)).toString());

    parameters = Map.of("parseTypes", "tEXT, forms, TABLES");
    assertEquals("[TEXT, FORMS, TABLES]",
        processor.getOcrParseTypes(new Action().parameters(parameters)).toString());
  }

  /**
   * Handle OCR Action.
   * 
   * @throws IOException IOException
   * @throws URISyntaxException URISyntaxException
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandle01() throws IOException, URISyntaxException {
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      String documentId = UUID.randomUUID().toString();
      List<Action> actions = Arrays.asList(new Action().type(ActionType.OCR));
      actionsService.saveActions(siteId, documentId, actions);

      Map<String, Object> map =
          loadFileAsMap(this, "/actions-event01.json", "c2695f67-d95e-4db0-985e-574168b12e57",
              documentId, "default", siteId != null ? siteId : "default");

      // when
      processor.handleRequest(map, this.context);

      // then
      HttpRequest lastRequest = callback.getLastRequest();
      assertTrue(lastRequest.getPath().toString().endsWith("/ocr"));
      map = gson.fromJson(lastRequest.getBodyAsString(), Map.class);
      assertEquals("[TEXT]", map.get("parseTypes").toString());
    }
  }
}
