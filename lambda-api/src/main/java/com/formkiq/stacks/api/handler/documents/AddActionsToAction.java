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
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.aws.dynamodb.ApiAuthorization;
import com.formkiq.module.actions.Action;
import com.formkiq.module.ocr.AwsTextractQuery;
import com.formkiq.module.ocr.OcrEngine;
import com.formkiq.module.ocr.OcrOutputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Convert {@link Map} to {@link Action}.
 */
public class AddActionsToAction implements Function<AddAction, Action> {

  @Override
  public Action apply(final AddAction addAction) {

    String userId = ApiAuthorization.getAuthorization().getUsername();

    Map<String, Object> parameters = createMap(addAction.parameters());
    return new Action().queueId(addAction.queueId()).type(addAction.type()).parameters(parameters)
        .userId(userId);
  }

  private Map<String, Object> createMap(final AddActionParameters parameters) {
    Map<String, Object> map = null;

    if (parameters != null) {
      map = new HashMap<>();

      List<Map<String, Object>> ocrTextractQueries = toMap(parameters.ocrTextractQueries());
      put(map, "ocrTextractQueries", ocrTextractQueries);

      put(map, "ocrParseTypes", parameters.ocrParseTypes());
      put(map, "ocrEngine", toString(parameters.ocrEngine()));
      put(map, "ocrOutputType", toString(parameters.ocrOutputType()));
      put(map, "ocrNumberOfPages", parameters.ocrNumberOfPages());
      put(map, "addPdfDetectedCharactersAsText", parameters.addPdfDetectedCharactersAsText());
      put(map, "url", parameters.url());
      put(map, "characterMax", parameters.characterMax());
      put(map, "engine", parameters.engine());
      put(map, "notificationType", parameters.notificationType());
      put(map, "notificationToCc", parameters.notificationToCc());
      put(map, "notificationToBcc", parameters.notificationToBcc());
      put(map, "notificationSubject", parameters.notificationSubject());
      put(map, "notificationText", parameters.notificationText());
      put(map, "notificationHtml", parameters.notificationHtml());
      put(map, "tags", parameters.tags());
      put(map, "mappingId", parameters.mappingId());
      put(map, "eventBusName", parameters.eventBusName());
      put(map, "width", parameters.width());
      put(map, "height", parameters.height());
      put(map, "path", parameters.path());
      put(map, "outputType", parameters.outputType());
      put(map, "llmPromptEntityName", parameters.llmPromptEntityName());
    }

    return map;
  }

  private void put(final Map<String, Object> map, final String key,
      final List<Map<String, Object>> value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private void put(final Map<String, Object> map, final String key, final String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private List<Map<String, Object>> toMap(final List<AwsTextractQuery> awsTextractQueries) {
    List<Map<String, Object>> list = null;

    if (!notNull(awsTextractQueries).isEmpty()) {
      list = new ArrayList<>();

      for (AwsTextractQuery q : awsTextractQueries) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", q.text());
        map.put("alias", q.alias());
        map.put("pages", q.pages());

        list.add(map);
      }
    }

    return list;
  }

  private String toString(final OcrEngine ocrEngine) {
    return ocrEngine != null ? ocrEngine.name() : null;
  }

  private String toString(final OcrOutputType ocrOutputType) {
    return ocrOutputType != null ? ocrOutputType.name() : null;
  }
}
