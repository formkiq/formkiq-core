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
package com.formkiq.module.lambda.lucene;

import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.formkiq.graalvm.annotations.Reflectable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** {@link RequestHandler} for handling Document Actions. */
@Reflectable
public class LuceneProcessor implements RequestHandler<Map<String, Object>, Void> {

  /** {@link Gson}. */
  private Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   * 
   */
  public LuceneProcessor() {
    this(System.getenv());
  }

  /**
   * constructor.
   *
   * @param map {@link Map}
   */
  protected LuceneProcessor(final Map<String, String> map) {}

  @SuppressWarnings("unchecked")
  @Override
  public Void handleRequest(final Map<String, Object> map, final Context context) {

    String json = null;

    try {

      LambdaLogger logger = context.getLogger();

      if ("true".equals(System.getenv("DEBUG"))) {
        json = this.gson.toJson(map);
        logger.log(json);
      }

      List<Map<String, Object>> records = (List<Map<String, Object>>) map.get("Records");
      processRecords(logger, records);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Process Records.
   * 
   * @param logger {@link LambdaLogger}
   * @param records {@link List} {@link Map}
   */
  private void processRecords(final LambdaLogger logger, final List<Map<String, Object>> records) {

  }
}
