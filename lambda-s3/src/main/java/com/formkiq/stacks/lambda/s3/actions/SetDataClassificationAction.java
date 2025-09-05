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
package com.formkiq.stacks.lambda.s3.actions;

import com.formkiq.module.actions.Action;
import com.formkiq.module.actions.ActionStatus;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.formkiq.stacks.lambda.s3.ProcessActionStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DocumentAction for DataClassification {@link Action}.
 */
public class SetDataClassificationAction implements DocumentAction {

  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   *
   * @param serviceCache {@link AwsServiceCache}
   */
  public SetDataClassificationAction(final AwsServiceCache serviceCache) {
    this.http = new SendHttpRequest(serviceCache);
  }

  @Override
  public ProcessActionStatus run(final Logger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException {
    Map<String, Object> payload = buildPayload(action);
    String json = this.gson.toJson(payload);

    if (logger.isLogged(LogLevel.DEBUG)) {
      String s = String.format("{\"type\",\"%s\",\"method\":\"%s\",\"url\":\"%s\",\"body\":\"%s\"}",
          "ocr", "POST", "/documents/" + documentId + "/dataClassification", json);
      logger.debug(s);
    }

    this.http.sendRequest(siteId, "put", "/documents/" + documentId + "/dataClassification", json);
    return new ProcessActionStatus(ActionStatus.COMPLETE, false);
  }

  private Map<String, Object> buildPayload(final Action action) {
    Map<String, Object> parameters =
        action.parameters() != null ? action.parameters() : new HashMap<>();
    return Map.of("llmPromptEntityName", parameters.get("llmPromptEntityName"));
  }
}
