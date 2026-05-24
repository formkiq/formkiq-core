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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.aws.sqs.events.SqsEventRecord;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.module.lambdaservices.logger.Logger;
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
    private final Map<String, ApiGatewayRequestHandler> urlMap =
        Map.of("/test", new TestRequestHandler());

    TestRestApiRequestHandler(final AwsServiceCache awsServices) {
      this.services = awsServices;
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
}
