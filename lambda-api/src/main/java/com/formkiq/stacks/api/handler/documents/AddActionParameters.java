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

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.ocr.AwsTextractQuery;
import com.formkiq.module.ocr.OcrEngine;
import com.formkiq.module.ocr.OcrOutputType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Parameters for an action. Many are optional; types follow the OpenAPI spec.
 */
@Reflectable
public record AddActionParameters(List<AwsTextractQuery> ocrTextractQueries, String ocrParseTypes,
    OcrEngine ocrEngine, OcrOutputType ocrOutputType, String ocrNumberOfPages,
    String addPdfDetectedCharactersAsText, String url, String characterMax, String engine,
    String notificationType, String notificationToCc, String notificationToBcc,
    String notificationSubject, String notificationText, String notificationHtml, String tags,
    String mappingId, String eventBusName, String checksumType, String width, String height,
    String path, String outputType, String llmPromptEntityName) {

  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();

    List<Map<String, Object>> textractQueries = toMap(this.ocrTextractQueries);
    put(map, "ocrTextractQueries", textractQueries);

    put(map, "ocrParseTypes", this.ocrParseTypes);
    put(map, "ocrEngine", toString(this.ocrEngine));
    put(map, "ocrOutputType", toString(this.ocrOutputType));
    put(map, "ocrNumberOfPages", this.ocrNumberOfPages);
    put(map, "addPdfDetectedCharactersAsText", this.addPdfDetectedCharactersAsText);
    put(map, "url", this.url);
    put(map, "characterMax", this.characterMax);
    put(map, "engine", this.engine);
    put(map, "notificationType", this.notificationType);
    put(map, "notificationToCc", this.notificationToCc);
    put(map, "notificationToBcc", this.notificationToBcc);
    put(map, "notificationSubject", this.notificationSubject);
    put(map, "notificationText", this.notificationText);
    put(map, "notificationHtml", this.notificationHtml);
    put(map, "tags", this.tags);
    put(map, "mappingId", this.mappingId);
    put(map, "eventBusName", this.eventBusName);
    put(map, "checksumType", this.checksumType);
    put(map, "width", this.width);
    put(map, "height", this.height);
    put(map, "path", this.path);
    put(map, "outputType", this.outputType);
    put(map, "llmPromptEntityName", this.llmPromptEntityName);

    return map;
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

  private String toString(final OcrEngine value) {
    return value != null ? value.name() : null;
  }

  private String toString(final OcrOutputType value) {
    return value != null ? value.name() : null;
  }
}
