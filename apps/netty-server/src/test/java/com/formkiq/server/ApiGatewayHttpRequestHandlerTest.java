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
package com.formkiq.server;

import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.server.auth.IAuthCredentials;
import com.formkiq.server.auth.Tokens;
import com.formkiq.testutils.api.JwtTokenBuilder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ApiGatewayHttpRequestHandler}.
 */
class ApiGatewayHttpRequestHandlerTest {

  /**
   * Captures the API Gateway event created by {@link ApiGatewayHttpRequestHandler}.
   */
  private static final class CapturingNettyRequestHandler extends NettyRequestHandler {

    /** Captured {@link ApiGatewayRequestEvent}. */
    private ApiGatewayRequestEvent event;

    private CapturingNettyRequestHandler() {
      super(testEnvironment(), Map.of(),
          StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")));
    }

    @Override
    public void handleRequest(final InputStream input, final OutputStream output,
        final Context context) throws IOException {
      String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      this.event = gson.fromJson(body, ApiGatewayRequestEvent.class);

      String response = gson.toJson(Map.of("statusCode", 200, "headers",
          Map.of("Content-Type", "application/json"), "body", "{}"));
      output.write(response.getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * Auth credentials that accept any token.
   */
  private static final class ValidAuthCredentials implements IAuthCredentials {
    @Override
    public Tokens getTokens(final String username, final String password) {
      return null;
    }

    @Override
    public boolean isApiKeyValid(final String apiKey) {
      return true;
    }

    @Override
    public Tokens refreshTokens(final String refreshToken) {
      return null;
    }
  }

  private static Map<String, String> testEnvironment() {
    Map<String, String> env = new HashMap<>();
    env.put("API_URL", "http://api:8080");
    env.put("APP_ENVIRONMENT", "dev");
    env.put("AWS_REGION", "us-east-1");
    env.put("CACHE_TABLE", "Cache");
    env.put("DOCUMENT_SYNC_TABLE", "DocumentSyncs");
    env.put("DOCUMENTS_IAM_URL", "http://api:8080");
    env.put("DOCUMENTS_S3_BUCKET", "documents");
    env.put("DOCUMENTS_TABLE", "Documents");
    env.put("DOCUMENT_VERSIONS_PLUGIN",
        "com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning");
    env.put("FORMKIQ_TYPE", "core");
    env.put("FORMKIQ_VERSION", "1.19.0");
    env.put("LOG_LEVEL", "error");
    env.put("MODULE_site_permissions", "automatic");
    env.put("OCR_S3_BUCKET", "ocr");
    env.put("OCR_SQS_QUEUE_URL", "local-ocr-queue");
    env.put("OPERATIONAL_MODE", "ACTIVE");
    env.put("PATH_STYLE_ACCESS_ENABLED", "true");
    env.put("S3_ACTIONS_PRESIGNER_URL", "http://localhost:9000");
    env.put("SNS_DOCUMENT_EVENT", "");
    env.put("STAGE_DOCUMENTS_S3_BUCKET", "stagingdocuments");
    env.put("USER_AUTHENTICATION", "cognito");
    return env;
  }

  /**
   * JWT claims should be forwarded into the API Gateway authorizer context.
   *
   */
  @Test
  void testJwtGroupsArePassedToAuthorizerClaims() {
    // given
    CapturingNettyRequestHandler requestHandler = new CapturingNettyRequestHandler();
    ApiGatewayHttpRequestHandler handler = new ApiGatewayHttpRequestHandler(requestHandler,
        new ValidAuthCredentials(), List.of("/sites"));

    String jwt = new JwtTokenBuilder("admin@me.com").groups(List.of("Admins", "default")).build();

    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
        "/sites", Unpooled.copiedBuffer(new byte[0]));
    request.headers().add("Authorization", jwt);

    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
      @Override
      public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        handler.handle(ctx, (FullHttpRequest) msg);
      }
    });

    // when
    channel.writeInbound(request);
    FullHttpResponse response = channel.readOutbound();

    // then
    assertEquals(200, response.status().code());

    Map<String, Object> authorizer = requestHandler.event.getRequestContext().getAuthorizer();
    Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
    Object groups = claims.get("cognito:groups");

    assertEquals(List.of("Admins", "default"), groups);
  }
}
