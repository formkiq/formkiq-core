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
import java.util.UUID;
import com.amazonaws.services.lambda.runtime.Context;
import com.formkiq.lambda.runtime.graalvm.LambdaContext;
import com.formkiq.stacks.lambda.s3.DocumentsS3Update;
import com.formkiq.stacks.lambda.s3.StagingS3Create;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Http Method 'Options' {@link HttpRequestHandler}.
 */
public class MinioS3HttpRequestHandler implements HttpRequestHandler {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /** {@link StagingS3Create}. */
  private StagingS3Create s3Create;
  /** {@link DocumentsS3Update}. */
  private DocumentsS3Update s3Update;

  /**
   * constructor.
   * 
   * @param create {@link StagingS3Create}
   * @param update {@link DocumentsS3Update}
   */
  public MinioS3HttpRequestHandler(final StagingS3Create create, final DocumentsS3Update update) {
    this.s3Create = create;
    this.s3Update = update;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(final ChannelHandlerContext ctx, final FullHttpRequest request)
      throws IOException {

    String body = getBody(request.content());
    Context context = new LambdaContext(UUID.randomUUID().toString());

    Map<String, Object> map = this.gson.fromJson(body, Map.class);

    if (request.uri().endsWith("/minio/s3/documents")) {
      this.s3Update.handleRequest(map, context);
    } else {
      this.s3Create.handleRequest(map, context);
    }

    sendResponse(ctx, HttpResponseStatus.OK, body);
  }

  @Override
  public boolean isSupported(final FullHttpRequest request) {
    return request.uri().contains("/minio/s3/");
  }
}
