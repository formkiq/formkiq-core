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
 * Http Method 'Options' {@link HttpRequestHandler}.
 */
public class CognitoHttpRequestHandler implements HttpRequestHandler {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  @Override
  public void handle(final ChannelHandlerContext ctx, final FullHttpRequest request)
      throws IOException {

    String target = request.headers().get("X-Amz-Target");

    String body = "";

    if ("AWSCognitoIdentityProviderService.InitiateAuth".equals(target)) {
      body = initiateAuthResponse(request);
    } else if ("AWSCognitoIdentityProviderService.RespondToAuthChallenge".equals(target)) {
      body = respondToAuthChallenge(request);
    }

    HttpHeaders headers = new DefaultHttpHeaders();
    headers.add("Access-Control-Allow-Headers", "*");
    headers.add("Access-Control-Allow-Methods", "*");
    headers.add("Access-Control-Allow-Origin", "*");
    headers.add("Content-Type", "application/json");

    DefaultFullHttpResponse response = buildResponse(HttpResponseStatus.OK, body);
    response.headers().set(headers);
    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

  private String respondToAuthChallenge(final FullHttpRequest request) {
    return "";
  }

  private String initiateAuthResponse(final FullHttpRequest request) {
    Map<String, Object> map = Map.of("ChallengeName", "PASSWORD_VERIFIER", "ChallengeParameters",
        Map.of("SALT", "28dfcc4f2d992485f4ea3fe79ceab145", "SECRET_BLOCK",
            "dHLEiRGZAV/yZoBn1cYZMjaD/OOcE9oXmQd8p/Sa9NmYM", "SRP_B",
            "ce20869ae852789756e203f87543", "USERNAME", "06387df5-ed2a-4277-9013-f6ed4d20fd63",
            "USER_ID_FOR_SRP", "06387df5-ed2a-4277-9013-f6ed4d20fd63"));

    String body = this.gson.toJson(map);
    return body;
  }

  @Override
  public boolean isSupported(final FullHttpRequest request) {
    String target = request.headers().get("X-Amz-Target");
    return "AWSCognitoIdentityProviderService.InitiateAuth".equals(target)
        || "AWSCognitoIdentityProviderService.RespondToAuthChallenge".equals(target);
  }
}
