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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.services.lambda.ApiGatewayRequestContext;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.runtime.graalvm.LambdaContext;
import com.formkiq.stacks.lambda.s3.DocumentsS3Update;
import com.formkiq.stacks.lambda.s3.StagingS3Create;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import software.amazon.awssdk.utils.IoUtils;

/**
 * {@link SimpleChannelInboundHandler} for Http Server.
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link NettyRequestHandler}. */
  private NettyRequestHandler handler;
  /** {@link StagingS3Create}. */
  private StagingS3Create s3Create;
  /** {@link DocumentsS3Update}. */
  private DocumentsS3Update s3Update;
  /** {@link NettyRequestHandler} Urls. */
  private Collection<String> urls;

  /**
   * constructor.
   * 
   * @param requestHandler {@link NettyRequestHandler}
   * @param stagingS3Create {@link StagingS3Create}
   * @param documentS3Update {@link DocumentsS3Update}
   */
  public HttpServerHandler(final NettyRequestHandler requestHandler,
      final StagingS3Create stagingS3Create, final DocumentsS3Update documentS3Update) {
    this.handler = requestHandler;
    this.urls = this.handler.getUrlMap().keySet();
    this.s3Create = stagingS3Create;
    this.s3Update = documentS3Update;
  }

  @SuppressWarnings("unchecked")
  private DefaultFullHttpResponse buildResponse(final ByteArrayOutputStream os) {

    String output = new String(os.toByteArray(), StandardCharsets.UTF_8);
    Map<String, Object> map = this.gson.fromJson(output, Map.class);
    String body = map.getOrDefault("body", "").toString();
    String statusCode = map.getOrDefault("statusCode", "404").toString().replaceAll("\\.0", "");

    DefaultFullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.parseLine(statusCode),
            Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
    return response;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest req)
      throws Exception {

    if (req.uri().contains("/minio/s3/")) {
      processS3CreateRequest(ctx, req);
    } else {
      processApiGatewayRequest(ctx, req);
    }
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  private Map<String, String> createPathParameters(final String resource, final String uri) {

    Map<String, String> map = new HashMap<>();

    if (resource != null && uri != null) {
      String[] s0 = resource.split("/");
      String[] s1 = uri.split("/");


      for (int i = 0; i < s0.length; i++) {
        if (s0[i].startsWith("{") && s0[i].endsWith("}")) {
          map.put(s0[i].substring(1, s0[i].length() - 1), s1[i]);
        }
      }
    }

    return map;
  }

  private Map<String, String> createQueryParameters(final FullHttpRequest request) {

    Map<String, String> map = new HashMap<>();
    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
    Map<String, List<String>> params = queryStringDecoder.parameters();

    for (Entry<String, List<String>> e : params.entrySet()) {
      map.put(e.getKey(), e.getValue().get(0));
    }

    return map;
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  private String getBody(final ByteBuf content) throws IOException {
    try (InputStream is = new ByteBufInputStream(content)) {
      return IoUtils.toUtf8String(is);
    }
  }

  private void processApiGatewayRequest(final ChannelHandlerContext ctx, final FullHttpRequest req)
      throws IOException {

    String resource = Strings.findUrlMatch(this.urls, req.uri());
    Map<String, String> pathParams = createPathParameters(resource, req.uri());
    Map<String, String> queryParameters = createQueryParameters(req);

    ApiGatewayRequestEvent apiEvent = new ApiGatewayRequestEvent();
    apiEvent.setPath(req.uri());
    apiEvent.setResource(resource != null ? resource : req.uri());
    apiEvent.setHttpMethod(req.method().name());
    apiEvent.setPathParameters(pathParams);
    apiEvent.setQueryStringParameters(queryParameters);

    String group = "default";
    ApiGatewayRequestContext requestContext = new ApiGatewayRequestContext();
    requestContext.setAuthorizer(
        Map.of("claims", Map.of("cognito:username", "admin", "cognito:groups", "[" + group + "]")));
    apiEvent.setRequestContext(requestContext);

    String body = getBody(req.content());
    apiEvent.setBody(body);

    String event = this.gson.toJson(apiEvent);

    InputStream is = new ByteArrayInputStream(event.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    Context context = new LambdaContext(UUID.randomUUID().toString());

    this.handler.handleRequest(is, os, context);

    DefaultFullHttpResponse response = buildResponse(os);
    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  @SuppressWarnings("unchecked")
  private void processS3CreateRequest(final ChannelHandlerContext ctx, final FullHttpRequest req)
      throws IOException {

    String body = getBody(req.content());
    Context context = new LambdaContext(UUID.randomUUID().toString());

    Map<String, Object> request = this.gson.fromJson(body, Map.class);

    if (req.uri().endsWith("/minio/s3/documents")) {
      this.s3Update.handleRequest(request, context);
    } else {
      this.s3Create.handleRequest(request, context);
    }

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK, Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));

    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }
}
