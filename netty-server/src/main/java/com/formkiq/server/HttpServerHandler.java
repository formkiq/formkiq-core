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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.server.auth.IAuthCredentials;
import com.formkiq.server.auth.KeycloakAuthCredentials;
import com.formkiq.server.auth.SimpleAuthCredentials;
import com.formkiq.stacks.lambda.s3.DocumentsS3Update;
import com.formkiq.stacks.lambda.s3.StagingS3Create;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * {@link SimpleChannelInboundHandler} for Http Server.
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  /** {@link List} {@link HttpRequestHandler}. */
  private final List<HttpRequestHandler> handlers;
  /** {@link NotSupportedHttpRequestHandler}. */
  private final NotSupportedHttpRequestHandler notSupported = new NotSupportedHttpRequestHandler();

  /**
   * constructor.
   * 
   * @param requestHandler {@link NettyRequestHandler}
   * @param stagingS3Create {@link StagingS3Create}
   * @param documentS3Update {@link DocumentsS3Update}
   */
  public HttpServerHandler(final NettyRequestHandler requestHandler,
      final StagingS3Create stagingS3Create, final DocumentsS3Update documentS3Update) {

    AwsServiceCache awsServices = requestHandler.getAwsServices();
    String apiKey = awsServices.environment("API_KEY");
    String adminUser = awsServices.environment("ADMIN_USERNAME");
    String adminPassword = awsServices.environment("ADMIN_PASSWORD");
    String keycloakTokenEndpoint = awsServices.environment("KEYCLOAK_TOKEN_ENDPOINT");
    String keycloakClientId = awsServices.environment("KEYCLOAK_CLIENT_ID");
    String keycloakClientSecret = awsServices.environment("KEYCLOAK_CLIENT_SECRET");

    Collection<String> urls = requestHandler.getUrlMap().keySet();

    IAuthCredentials authCredentials = (keycloakTokenEndpoint == null)
        ? new SimpleAuthCredentials(adminUser, adminPassword, apiKey)
        : new KeycloakAuthCredentials(keycloakTokenEndpoint, keycloakClientId,
            keycloakClientSecret);

    this.handlers = Arrays.asList(new OptionsHttpRequestHandler(),
        new MinioS3HttpRequestHandler(stagingS3Create, documentS3Update),
        new AuthenticationLoginHttpRequestHandler(authCredentials),
        new ApiGatewayHttpRequestHandler(requestHandler, authCredentials, urls));
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req) {

    Optional<HttpRequestHandler> o =
        this.handlers.stream().filter(h -> h.isSupported(req)).findFirst();

    if (o.isPresent()) {

      try {
        o.get().handle(ctx, req);
      } catch (IOException e) {
        this.notSupported.handle(ctx, req);
      }

    } else {
      this.notSupported.handle(ctx, req);
    }
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
