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

import com.formkiq.aws.services.lambda.exceptions.BadException;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

/**
 * {@link BiFunction} to convert {@link ApiGatewayRequestEvent} body to {@link Object}.
 * 
 * @param <T> Type of result
 */
public class JsonToObject<T> implements BiFunction<ApiGatewayRequestEvent, Class<T>, T> {

  /**
   * Static helper for one-off calls.
   * 
   * @param aws {@link AwsServiceCache}
   * @param event {@link ApiGatewayRequestEvent}
   * @param type Type of Class.
   * @param <T> Type of result
   * @return Result
   */
  public static <T> T fromJson(final AwsServiceCache aws, final ApiGatewayRequestEvent event,
      final Class<T> type) {
    return new JsonToObject<T>(aws).apply(event, type);
  }

  /** {@link Gson}. */
  private final Gson gson;

  public JsonToObject(final AwsServiceCache awsservice) {
    this.gson = awsservice.getExtension(Gson.class);
  }


  @Override
  public T apply(final ApiGatewayRequestEvent event, final Class<T> classOfT) {
    try (Reader reader = new InputStreamReader(new ByteArrayInputStream(event.getBodyAsBytes()),
        StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, classOfT);
    } catch (JsonSyntaxException | IOException e) {
      throw new BadException("invalid JSON body");
    }
  }
}
