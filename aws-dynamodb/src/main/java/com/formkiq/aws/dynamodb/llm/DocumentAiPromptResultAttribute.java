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
package com.formkiq.aws.dynamodb.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Attribute extracted from a document AI prompt result.
 *
 * @param key attribute key
 * @param stringValues attribute string values
 */
public record DocumentAiPromptResultAttribute(String key, List<String> stringValues) {

  /**
   * Canonical constructor.
   */
  public DocumentAiPromptResultAttribute {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(stringValues, "stringValues must not be null");
    stringValues = List.copyOf(stringValues);
  }

  /**
   * Creates an attribute from a map.
   *
   * @param map {@link Map}
   * @return {@link DocumentAiPromptResultAttribute}
   */
  public static DocumentAiPromptResultAttribute fromMap(final Map<String, Object> map) {
    Objects.requireNonNull(map, "map must not be null");
    List<String> stringValues = notNull((List<String>) map.get("stringValues"));

    return new DocumentAiPromptResultAttribute((String) map.get("key"),
        stringValues.stream().map(Object::toString).toList());
  }

  /**
   * Converts this attribute to a map for DynamoDB persistence.
   *
   * @return {@link Map}
   */
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("key", key);
    map.put("stringValues", stringValues);
    return map;
  }
}
