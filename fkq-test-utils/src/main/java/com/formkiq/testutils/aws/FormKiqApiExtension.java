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
package com.formkiq.testutils.aws;

import static com.formkiq.testutils.aws.DynamoDbExtension.CACHE_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_VERSION_TABLE;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENT_SYNCS_TABLE;
import static com.formkiq.testutils.aws.TestServices.AWS_REGION;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.FORMKIQ_APP_ENVIRONMENT;
import static com.formkiq.testutils.aws.TestServices.OCR_BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;

/**
 * 
 * JUnit 5 Extension for FormKiQ.
 *
 */
public class FormKiqApiExtension
    implements BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {

  /** {@link Random}. */
  private static final Random NUM_RAND = new Random();
  /** {@link FormKiQApiExtensionConfig}. */
  private final FormKiQApiExtensionConfig config;

  /**
   * Generate Random Port.
   * 
   * @return int
   */
  public static int generatePort() {
    final int topPort = 8000;
    final int bottomPort = 5000;
    return NUM_RAND.nextInt(topPort - bottomPort) + bottomPort;
  }

  /** {@link AbstractFormKiqApiResponseCallback}. */
  private final AbstractFormKiqApiResponseCallback callback;
  /** Environment {@link Map}. */
  private Map<String, String> environmentMap;

  /** {@link ClientAndServer}. */
  private ClientAndServer formkiqServer;
  /** {@link LocalStackExtension}. */
  private final LocalStackExtension localStackExtension;
  /** Port to run Test server. */
  private final int port;
  /** Is server running. */
  private boolean running = false;
  /** {@link TypesenseExtension}. */
  private final TypesenseExtension typeSenseExtension;

  /**
   * constructor.
   * 
   * @param localstack {@link LocalStackExtension}
   * @param typeSense {@link TypesenseExtension}
   * @param extensionConfig {@link FormKiQApiExtensionConfig}
   * @param responseCallback {@link AbstractFormKiqApiResponseCallback}
   */
  public FormKiqApiExtension(final LocalStackExtension localstack,
      final TypesenseExtension typeSense, final FormKiQApiExtensionConfig extensionConfig,
      final AbstractFormKiqApiResponseCallback responseCallback) {

    this.localStackExtension = localstack;
    this.typeSenseExtension = typeSense;
    this.port = generatePort();
    this.callback = responseCallback;
    this.config = extensionConfig;
  }

  @Override
  public void beforeAll(final ExtensionContext context) throws Exception {

    if (!this.running) {

      System.setProperty("mockserver.logLevel", "WARN");
      this.environmentMap = generateMap();

      this.callback.setEnvironmentMap(this.environmentMap);

      this.formkiqServer = startClientAndServer(this.port);

      this.formkiqServer.when(request()).respond(this.callback);
      this.running = true;
    }
  }

  @Override
  public void close() throws Throwable {
    closeServer();
  }

  private void closeServer() {
    if (this.formkiqServer != null) {
      this.formkiqServer.stop();
    }
    this.formkiqServer = null;
    this.running = false;
  }

  private Map<String, String> generateMap() {

    Map<String, String> map = new HashMap<>();

    if (this.config != null) {
      map.putAll(this.config.getEnvironment());
    }

    if (this.localStackExtension != null) {
      map.put("SNS_DOCUMENT_EVENT", this.localStackExtension.getSnsDocumentEvent());
      map.put("SQS_DOCUMENT_EVENT_URL", this.localStackExtension.getSqsDocumentEventUrl());
    }

    map.put("APP_ENVIRONMENT", FORMKIQ_APP_ENVIRONMENT);
    map.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    map.put("DOCUMENT_VERSIONS_TABLE", DOCUMENTS_VERSION_TABLE);
    map.put("DOCUMENT_SYNC_TABLE", DOCUMENT_SYNCS_TABLE);
    map.put("CACHE_TABLE", CACHE_TABLE);
    map.put("DOCUMENTS_S3_BUCKET", BUCKET_NAME);
    map.put("STAGE_DOCUMENTS_S3_BUCKET", STAGE_BUCKET_NAME);
    map.put("OCR_S3_BUCKET", OCR_BUCKET_NAME);
    map.put("AWS_REGION", AWS_REGION.toString());
    map.put("DEBUG", "false");
    map.put("DISTRIBUTION_BUCKET", "formkiq-distribution-us-east-pro");
    map.put("FORMKIQ_TYPE", "core");
    map.put("USER_AUTHENTICATION", "cognito");
    map.put("MODULE_site_permissions", "automatic");

    if (this.typeSenseExtension != null) {
      map.put("TYPESENSE_HOST", "http://localhost:" + this.typeSenseExtension.getFirstMappedPort());
    }

    map.put("TYPESENSE_API_KEY", API_KEY);
    map.put("MODULE_typesense", "true");

    return map;
  }

  /**
   * Get Base Path.
   * 
   * @return {@link String}
   */
  public String getBasePath() {
    return "http://localhost:" + this.port;
  }

  /**
   * Get Environment {@link Map}.
   * 
   * @return {@link Map}
   */
  public Map<String, String> getEnvironmentMap() {
    return this.environmentMap;
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    closeServer();
  }
}
