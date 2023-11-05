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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 * Http Method 'Options' {@link HttpRequestHandler}.
 */
public class ApiGatewayHttpRequestHandler implements HttpRequestHandler {

  /** API Key. */
  private String apiKey = null;
  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();
  /** {@link NettyRequestHandler}. */
  private NettyRequestHandler handler;
  /** {@link NettyRequestHandler} Urls. */
  private Collection<String> urls;

  /**
   * constructor.
   * 
   * @param reqestHandler {@link NettyRequestHandler}
   * @param requestApiKey {@link String}
   * @param handlerUrls {@link Collection} {@link String}
   * 
   */
  public ApiGatewayHttpRequestHandler(final NettyRequestHandler reqestHandler,
      final String requestApiKey, final Collection<String> handlerUrls) {
    this.apiKey = requestApiKey;
    this.handler = reqestHandler;
    this.urls = handlerUrls;
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

    Map<String, String> headers = (Map<String, String>) map.get("headers");
    for (Map.Entry<String, String> e : headers.entrySet()) {
      response.headers().add(e.getKey(), e.getValue());
    }

    return response;
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

  /**
   * Remove query parameters from URI.
   * 
   * @param req {@link FullHttpRequest}
   * @return {@link String}
   */
  private String getUri(final FullHttpRequest req) {
    String uri = req.uri();
    int pos = uri.indexOf("?");
    if (pos > -1) {
      uri = uri.substring(0, pos);
    }
    return uri;
  }

  @Override
  public void handle(final ChannelHandlerContext ctx, final FullHttpRequest request)
      throws IOException {

    if (validateAuthorization(ctx, request)) {
      handleApiGatewayRequest(ctx, request);
    }
  }

  private void handleApiGatewayRequest(final ChannelHandlerContext ctx,
      final FullHttpRequest request) throws IOException {
    String uri = getUri(request);

    String resource = Strings.findUrlMatch(this.urls, uri);
    Map<String, String> pathParams = createPathParameters(resource, uri);
    Map<String, String> queryParameters = createQueryParameters(request);

    ApiGatewayRequestEvent apiEvent = new ApiGatewayRequestEvent();
    apiEvent.setPath(request.uri());
    apiEvent.setResource(resource != null ? resource : uri);
    apiEvent.setHttpMethod(request.method().name());
    apiEvent.setPathParameters(pathParams);
    apiEvent.setQueryStringParameters(queryParameters);

    String group = "default";
    ApiGatewayRequestContext requestContext = new ApiGatewayRequestContext();
    requestContext.setAuthorizer(
        Map.of("claims", Map.of("cognito:username", "admin", "cognito:groups", "[" + group + "]")));
    apiEvent.setRequestContext(requestContext);

    String body = getBody(request.content());
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

  @Override
  public boolean isSupported(final FullHttpRequest request) {
    return this.apiKey.equals(request.headers().get("Authorization"));
  }

  /**
   * Validate Authorization {@link FullHttpRequest}.
   * 
   * @param ctx {@link ChannelHandlerContext}
   * @param req {@link FullHttpRequest}
   * @return boolean
   */
  private boolean validateAuthorization(final ChannelHandlerContext ctx,
      final FullHttpRequest req) {

    boolean proceed = false;
    if (!this.apiKey.equals(req.headers().get("Authorization"))) {
      sendResponse(ctx, HttpResponseStatus.FORBIDDEN,
          "{\"message\":\"access denied, invalid API_KEY\"}");
    } else {
      proceed = true;
    }

    return proceed;
  }
}
