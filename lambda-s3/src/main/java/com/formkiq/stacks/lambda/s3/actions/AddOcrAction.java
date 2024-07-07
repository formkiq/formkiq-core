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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.module.actions.Action;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.stacks.lambda.s3.DocumentAction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;

/**
 * DocumentAction for OCR {@link Action}.
 */
public class AddOcrAction implements DocumentAction {

  /** {@link SendHttpRequest}. */
  private final SendHttpRequest http;
  /** {@link Gson}. */
  private final Gson gson = new GsonBuilder().create();

  /**
   * constructor.
   * 
   * @param serviceCache {@link AwsServiceCache}
   */
  public AddOcrAction(final AwsServiceCache serviceCache) {
    this.http = new SendHttpRequest(serviceCache);
  }

  @Override
  public void run(final LambdaLogger logger, final String siteId, final String documentId,
      final List<Action> actions, final Action action) throws IOException {
    Map<String, Object> payload = buildAddOcrPayload(action);
    this.http.sendRequest(siteId, "post", "/documents/" + documentId + "/ocr",
        this.gson.toJson(payload));
  }

  private Map<String, Object> buildAddOcrPayload(final Action action) {
    Map<String, String> parameters =
        action.parameters() != null ? action.parameters() : new HashMap<>();

    String addPdfDetectedCharactersAsText =
        parameters.getOrDefault("addPdfDetectedCharactersAsText", "false");

    Map<String, Object> payload = new HashMap<>();

    List<String> parseTypes = getOcrParseTypes(action);
    payload.put("parseTypes", parseTypes);
    payload.put("addPdfDetectedCharactersAsText", "true".equals(addPdfDetectedCharactersAsText));

    if (parameters.containsKey("ocrNumberOfPages")) {
      payload.put("ocrNumberOfPages", parameters.get("ocrNumberOfPages"));
    }

    String ocrExportToCsv = parameters.getOrDefault("ocrExportToCsv", "false");
    payload.put("ocrExportToCsv", "true".equals(ocrExportToCsv));

    String ocrEngine = parameters.get("ocrEngine");

    if (!isEmpty(ocrEngine)) {
      payload.put("ocrEngine", ocrEngine);
    }
    return payload;
  }

  /**
   * Get ParseTypes from {@link Action} parameters.
   *
   * @param action {@link Action}
   * @return {@link List} {@link String}
   */
  public List<String> getOcrParseTypes(final Action action) {

    Map<String, String> parameters = notNull(action.parameters());
    String s = parameters.getOrDefault("ocrParseTypes", "TEXT");

    return Arrays.stream(s.split(",")).map(t -> t.trim().toUpperCase()).distinct()
        .collect(Collectors.toList());
  }
}
