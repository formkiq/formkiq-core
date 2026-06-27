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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Handler for Http Requests.
 */
public interface HttpRequestHandler {

  /**
   * Build {@link DefaultHttpResponse}.
   * 
   * @param status {@link HttpResponseStatus}
   * @param body {@link String}
   * @return {@link DefaultFullHttpResponse}
   */
  default DefaultFullHttpResponse buildResponse(final HttpResponseStatus status,
      final String body) {
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
        Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
    return response;
  }

  /**
   * Get Request Body.
   * 
   * @param content {@link ByteBuf}
   * @return {@link String}
   * @throws IOException IOException
   */
  default String getBody(final ByteBuf content) throws IOException {
    try (InputStream is = new ByteBufInputStream(content)) {
      return IoUtils.toUtf8String(is);
    }
  }

  /**
   * Handle Http Request.
   * 
   * @param ctx {@link ChannelHandlerContext}
   * @param request {@link FullHttpRequest}
   * @throws IOException IOException
   */
  void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException;

  /**
   * Does this Http Request Handler support this {@link FullHttpRequest}.
   * 
   * @param request {@link FullHttpRequest}
   * @return {@link FullHttpRequest}
   */
  boolean isSupported(FullHttpRequest request);

  /**
   * Sends Response.
   * 
   * @param ctx {@link ChannelHandlerContext}
   * @param status {@link HttpResponseStatus}
   * @param body {@link String}
   */
  default void sendResponse(final ChannelHandlerContext ctx, final HttpResponseStatus status,
      final String body) {

    DefaultFullHttpResponse response = buildResponse(status, body);

    HttpUtil.setContentLength(response, response.content().readableBytes());

    ctx.writeAndFlush(response);
  }

}
