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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.aws.services.lambda.ApiGatewayRequestContext;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.runtime.graalvm.LambdaContext;
import com.formkiq.server.auth.IAuthCredentials;
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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.DEFAULT_SITE_ID;

/**
 * Http Method 'Options' {@link HttpRequestHandler}.
 */
public class ApiGatewayHttpRequestHandler implements HttpRequestHandler {

  /** Scheme separator. */
  private static final String SCHEME_SEPARATOR = "://";
  /** Scheme separator length. */
  private static final int SCHEME_SEPARATOR_LENGTH = SCHEME_SEPARATOR.length();

  /** Auth Credentials. */
  private final IAuthCredentials authCredentials;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();
  /** {@link NettyRequestHandler}. */
  private final NettyRequestHandler handler;
  /** Internal API Host. */
  private final String internalApiHost;
  /** {@link NettyRequestHandler} Urls. */
  private final Collection<String> urls;

  /**
   * constructor.
   * 
   * @param reqestHandler {@link NettyRequestHandler}
   * @param authCreds {@link IAuthCredentials}
   * @param handlerUrls {@link Collection} {@link String}
   * 
   */
  public ApiGatewayHttpRequestHandler(final NettyRequestHandler reqestHandler,
      final IAuthCredentials authCreds, final Collection<String> handlerUrls) {
    this.authCredentials = authCreds;
    this.handler = reqestHandler;
    this.internalApiHost = getHost(reqestHandler.getAwsServices().environment("API_URL"));
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

  private Map<String, String> createHeaders(final FullHttpRequest request) {

    Map<String, String> map = new HashMap<>();
    request.headers().forEach(e -> map.put(e.getKey(), e.getValue()));
    return map;
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

  private String getHost(final String url) {
    String host = null;

    if (url != null && url.contains(SCHEME_SEPARATOR)) {
      String u = url.substring(url.indexOf(SCHEME_SEPARATOR) + SCHEME_SEPARATOR_LENGTH);
      int slash = u.indexOf("/");
      host = slash > -1 ? u.substring(0, slash) : u;
    }

    return host;
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
    if (isAuthorized(ctx, request)) {
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
    apiEvent.setHeaders(createHeaders(request));

    ApiGatewayRequestContext requestContext = new ApiGatewayRequestContext();
    requestContext.setAuthorizer(Map.of("claims",
        Map.of("cognito:username", "admin", "cognito:groups", "[" + DEFAULT_SITE_ID + "]")));
    apiEvent.setRequestContext(requestContext);

    String body = getBody(request.content());
    apiEvent.setBody(body);

    String event = this.gson.toJson(apiEvent);

    InputStream is = new ByteArrayInputStream(event.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    Context context = new LambdaContext(ID.uuid());

    this.handler.handleRequest(is, os, context);

    DefaultFullHttpResponse response = buildResponse(os);
    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  /**
   * Validate Authorization {@link FullHttpRequest}.
   * 
   * @param ctx {@link ChannelHandlerContext}
   * @param req {@link FullHttpRequest}
   * @return boolean
   */
  private boolean isAuthorized(final ChannelHandlerContext ctx, final FullHttpRequest req) {
    String authorization = req.headers().get("Authorization");

    if (authorization != null && authorization.startsWith("AWS4-HMAC-SHA256 ")
        && (isLoopbackRequest(ctx) || isInternalApiRequest(req))) {
      return true;
    }

    if (!authCredentials.isApiKeyValid(authorization)) {
      sendResponse(ctx, HttpResponseStatus.FORBIDDEN,
          "{\"message\":\"access denied, invalid Authorization\"}");

      return false;
    }

    return true;
  }

  private boolean isInternalApiRequest(final FullHttpRequest req) {
    String host = req.headers().get("Host");
    return this.internalApiHost != null && this.internalApiHost.equalsIgnoreCase(host);
  }

  private boolean isLoopbackRequest(final ChannelHandlerContext ctx) {
    SocketAddress remoteAddress = ctx.channel().remoteAddress();

    return remoteAddress instanceof InetSocketAddress inetSocketAddress
        && inetSocketAddress.getAddress().isLoopbackAddress();
  }

  @Override
  public boolean isSupported(final FullHttpRequest request) {
    return request.headers().get("Authorization") != null;
  }
}
