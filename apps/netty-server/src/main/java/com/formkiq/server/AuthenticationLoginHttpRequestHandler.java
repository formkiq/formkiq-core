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
import java.util.Map;

import com.formkiq.server.auth.IAuthCredentials;
import com.formkiq.server.auth.Tokens;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

/**
 * Http Method '/login' {@link HttpRequestHandler}.
 */
public class AuthenticationLoginHttpRequestHandler implements HttpRequestHandler {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** Auth credentials data. */
  private final IAuthCredentials authCredentials;

  /**
   * constructor.
   * 
   * @param authenticationCredentials {@link IAuthCredentials}
   */
  public AuthenticationLoginHttpRequestHandler(final IAuthCredentials authenticationCredentials) {
    this.authCredentials = authenticationCredentials;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(final ChannelHandlerContext ctx, final FullHttpRequest request)
      throws IOException {

    String body = getBody(request.content());
    Map<String, String> map = this.gson.fromJson(body, Map.class);

    if (!map.containsKey("username") && !map.containsKey("password")) {
      throw new IOException("invalid request");
    }

    String username = map.get("username");
    String password = map.get("password");

    String responseBody = "{\"code\":\"NotAuthorizedException\","
        + "\"message\":\"Incorrect username or password.\"}";
    HttpResponseStatus status = HttpResponseStatus.BAD_REQUEST;

    Tokens tokens = this.authCredentials.getTokens(username, password);
    if (tokens != null) {
      Map<String, Object> results =
          Map.of("AuthenticationResult", Map.of("AccessToken", tokens.accessToken(), "IdToken",
              tokens.idToken(), "RefreshToken", tokens.refreshToken()));

      status = HttpResponseStatus.OK;
      responseBody = this.gson.toJson(results);
    }

    DefaultFullHttpResponse response = buildResponse(status, responseBody);

    setCorsHeaders(response);
    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  @Override
  public boolean isSupported(final FullHttpRequest request) {
    return request.uri().equals("/login");
  }

  private void setCorsHeaders(final DefaultFullHttpResponse response) {
    HttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key");
    headers.add("Access-Control-Allow-Methods", "*");
    headers.add("Access-Control-Allow-Origin", "*");
    headers.add("Content-Type", "application/json");

    response.headers().set(headers);
  }
}
