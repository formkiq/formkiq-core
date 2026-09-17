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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.sqs.events.SqsEventRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.module.lambdaservices.logger.LoggerRecorder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Unit tests for {@link AbstractRestApiRequestHandler}. */
class RestApiRequestHandlerEventInterceptorTest {

  private static class TestApiAuthorizationInterceptor implements ApiAuthorizationInterceptor {

    @Override
    public void afterBuildAuthorization(final ApiGatewayRequestEvent event,
        final ApiAuthorization authorization) {
      assertEquals("finance", event.getQueryStringParameter("siteId"));
    }

    @Override
    public void beforeBuildAuthorization(final ApiGatewayRequestEvent event) {
      event.setQueryStringParameters(Map.of("siteId", "finance"));
    }
  }

  private static class TestErrorRequestHandler implements ApiGatewayRequestHandler {

    @Override
    public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
        final ApiAuthorization authorization, final AwsServiceCache awsServices) {
      throw new AssertionError("original failure");
    }

    @Override
    public String getRequestUrl() {
      return "/test";
    }
  }

  private static class TestRequestHandler implements ApiGatewayRequestHandler {

    @Override
    public ApiRequestHandlerResponse get(final ApiGatewayRequestEvent event,
        final ApiAuthorization authorization, final AwsServiceCache awsServices) {
      return ApiRequestHandlerResponse.builder().ok().body("siteId", authorization.getSiteId())
          .body("requestSiteId", event.getQueryStringParameter("siteId")).build();
    }

    @Override
    public String getRequestUrl() {
      return "/test";
    }
  }

  private static class TestRestApiRequestHandler extends AbstractRestApiRequestHandler {

    /** {@link AwsServiceCache}. */
    private final AwsServiceCache services;
    /** Url Map. */
    private final Map<String, ApiGatewayRequestHandler> urlMap;

    TestRestApiRequestHandler(final AwsServiceCache awsServices) {
      this(awsServices, new TestRequestHandler());
    }

    TestRestApiRequestHandler(final AwsServiceCache awsServices,
        final ApiGatewayRequestHandler requestHandler) {
      this.services = awsServices;
      this.urlMap = Map.of("/test", requestHandler);
    }

    @Override
    public AwsServiceCache getAwsServices() {
      return this.services;
    }

    @Override
    public Map<String, ApiGatewayRequestHandler> getUrlMap() {
      return this.urlMap;
    }

    @Override
    public void handleSqsRequest(final Logger logger, final AwsServiceCache awsServices,
        final SqsEventRecord sqsEventRecord) {
      // not used
    }
  }

  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  private ApiGatewayRequestEvent event() {
    ApiGatewayRequestEvent event = new ApiGatewayRequestEvent();
    event.setHttpMethod("GET");
    event.setPath("/test");
    event.setResource("/test");

    ApiGatewayRequestContext context = new ApiGatewayRequestContext();
    context.setAuthorizer(Map.of("claims",
        Map.of("cognito:username", "user", "cognito:groups", "[default finance]")));
    context.setIdentity(Map.of("sourceIp", "127.0.0.1"));
    context.setProtocol("HTTP/1.1");
    context.setRequestId("requestId");
    context.setRequestTime("requestTime");
    event.setRequestContext(context);

    return event;
  }

  @Test
  void handleRequestLogsBodyWithoutHeaders() throws IOException {
    LoggerRecorder logger =
        logRequestBody("{\"PDFBytes\":\"private-pdf\",\"status\":\"sent\"}", false, null);
    assertFalse(logger.containsString("private-pdf"));
    assertTrue(logger.containsString("\"PDFBytes\":\"[REDACTED]\""));
    assertTrue(logger.containsString("\"status\":\"sent\""));
  }

  @Test
  void handleRequestLogsErrorWhenResponseIsNull() {
    // given
    LoggerRecorder logger = new LoggerRecorder();
    AwsServiceCache services = new AwsServiceCache().environment(Map.of()).setLogger(logger);
    services.registerAppend(ApiAuthorizationInterceptor.class,
        new ClassServiceExtension<>(new TestApiAuthorizationInterceptor()));
    TestRestApiRequestHandler handler =
        new TestRestApiRequestHandler(services, new TestErrorRequestHandler());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // when
    AssertionError error = assertThrows(AssertionError.class,
        () -> handler.handleRequest(
            new ByteArrayInputStream(this.gson.toJson(event()).getBytes(StandardCharsets.UTF_8)),
            output, null));

    // then
    assertEquals("original failure", error.getMessage());
    assertTrue(logger.containsString("original failure"));
    assertTrue(logger.containsString("\"status\":500"));
  }

  @Test
  void handleRequestRedactsCallbackBodyWithoutChangingRequest() throws IOException {
    String body = """
        {"event":"envelope-completed","data":{"envelopeId":"envelope-123",
          "envelopeSummary":{"status":"completed","envelopeDocuments":[
            {"PDFBytes":"private-pdf-content","documentId":"1","name":"Agreement.pdf"},
            {"pdfBytes":"second-pdf-content","documentId":"2"}]}},
          "documents":[{"documentBase64":"outbound-pdf-content"}],
          "access_token":"access-token-secret","refresh_token":"refresh-token-secret"}
        """;
    for (boolean base64 : List.of(false, true)) {
      String requestBody =
          base64 ? Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)) : body;
      LoggerRecorder logger = logRequestBody(requestBody, base64,
          Map.of("Authorization", "authorization-secret", "X-Amz-Security-Token",
              "security-token-secret", "X-DocuSign-Signature-1", "test-signature"));
      for (String secret : List.of("private-pdf-content", "second-pdf-content",
          "outbound-pdf-content", "access-token-secret", "refresh-token-secret",
          "authorization-secret", "security-token-secret", requestBody)) {
        assertFalse(logger.containsString(secret));
      }
      for (String visible : List.of("envelope-123", "envelope-completed", "Agreement.pdf",
          "\"status\":\"completed\"", "\"documentId\":\"1\"", "/esignature/docusign/events",
          "\"PDFBytes\":\"[REDACTED]\"", "\"pdfBytes\":\"[REDACTED]\"",
          "\"documentBase64\":\"[REDACTED]\"", "\"access_token\":\"[REDACTED]\"",
          "\"refresh_token\":\"[REDACTED]\"")) {
        assertTrue(logger.containsString(visible), visible);
      }
    }
  }

  @Test
  void handleRequestRedactsMalformedBodiesWithoutChangingRequest() throws IOException {
    for (boolean base64 : List.of(false, true)) {
      LoggerRecorder logger = logRequestBody("{malformed-private-body", base64, null);
      assertFalse(logger.containsString("malformed-private-body"));
      assertTrue(
          logger.containsString(base64 ? "body=\"[INVALID BASE64 BODY]\"" : "body=[REDACTED]"));
    }
  }

  @Test
  void handleRequestRedactsSensitiveHeadersFromDebugLog() throws IOException {
    // given
    LoggerRecorder logger = new LoggerRecorder();
    AwsServiceCache services = new AwsServiceCache().environment(Map.of()).setLogger(logger);
    services.registerAppend(ApiAuthorizationInterceptor.class,
        new ClassServiceExtension<>(new TestApiAuthorizationInterceptor()));
    TestRestApiRequestHandler handler = new TestRestApiRequestHandler(services);
    ApiGatewayRequestEvent event = event();
    event.setHeaders(Map.of("Authorization", "authorization-secret", "X-Amz-Security-Token",
        "security-token-secret", "X-Custom-Header", "visible-value"));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // when
    handler.handleRequest(
        new ByteArrayInputStream(this.gson.toJson(event).getBytes(StandardCharsets.UTF_8)), output,
        null);

    // then
    assertTrue(logger.containsString("\"Authorization\":\"****\""));
    assertTrue(logger.containsString("\"X-Amz-Security-Token\":\"****\""));
    assertTrue(logger.containsString("\"X-Custom-Header\":\"visible-value\""));
    assertFalse(logger.containsString("authorization-secret"));
    assertFalse(logger.containsString("security-token-secret"));
  }

  @Test
  void handleRequestRunsBeforeBuildAuthorizationInterceptor() throws IOException {
    // given
    AwsServiceCache services =
        new AwsServiceCache().environment(Map.of()).setLogger("INFO", "TEXT");
    services.registerAppend(ApiAuthorizationInterceptor.class,
        new ClassServiceExtension<>(new TestApiAuthorizationInterceptor()));
    TestRestApiRequestHandler handler = new TestRestApiRequestHandler(services);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // when
    handler.handleRequest(
        new ByteArrayInputStream(this.gson.toJson(event()).getBytes(StandardCharsets.UTF_8)),
        output, null);

    // then
    Map<String, Object> response =
        this.gson.fromJson(output.toString(StandardCharsets.UTF_8), Map.class);
    assertEquals(200, ((Number) response.get("statusCode")).intValue());

    Map<String, Object> body = this.gson.fromJson((String) response.get("body"), Map.class);
    assertEquals("finance", body.get("siteId"));
    assertEquals("finance", body.get("requestSiteId"));
  }

  private LoggerRecorder logRequestBody(final String body, final boolean base64,
      final Map<String, String> headers) throws IOException {
    LoggerRecorder logger = new LoggerRecorder();
    AwsServiceCache services = new AwsServiceCache().environment(Map.of()).setLogger(logger);
    services.registerAppend(ApiAuthorizationInterceptor.class,
        new ClassServiceExtension<>(new TestApiAuthorizationInterceptor()));
    final TestRestApiRequestHandler handler =
        new TestRestApiRequestHandler(services, new TestRequestHandler() {
          @Override
          public ApiRequestHandlerResponse post(final ApiGatewayRequestEvent request,
              final ApiAuthorization authorization, final AwsServiceCache awsServices) {
            assertEquals(body, request.getBody());
            assertEquals(base64, request.getIsBase64Encoded());
            assertEquals(headers, request.getHeaders());
            return ApiRequestHandlerResponse.builder().ok().build();
          }
        });
    ApiGatewayRequestEvent event = event();
    event.setPath("/esignature/docusign/events");
    event.setHttpMethod("POST");
    event.setHeaders(headers);
    event.setBody(body);
    event.setIsBase64Encoded(base64);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    handler.handleRequest(
        new ByteArrayInputStream(this.gson.toJson(event).getBytes(StandardCharsets.UTF_8)), output,
        null);
    Map<?, ?> response = this.gson.fromJson(output.toString(StandardCharsets.UTF_8), Map.class);
    assertEquals(200, ((Number) response.get("statusCode")).intValue());
    return logger;
  }
}
