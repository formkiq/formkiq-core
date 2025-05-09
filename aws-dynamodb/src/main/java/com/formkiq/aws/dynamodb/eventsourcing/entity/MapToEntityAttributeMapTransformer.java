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
package com.formkiq.aws.dynamodb.eventsourcing.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link Function} to transform {@link Map} to {@link List} {@link Map}.
 */
public class MapToEntityAttributeMapTransformer
    implements Function<Map<String, Object>, List<Map<String, Object>>> {

  @Override
  public List<Map<String, Object>> apply(final Map<String, Object> map) {

    List<Map<String, Object>> list = new ArrayList<>();
    List<String> keys = new ArrayList<>(map.keySet());
    Collections.sort(keys);

    for (String key : keys) {
      if (key.startsWith("attr#")) {

        Object value = map.get(key);
        String objectKey = findObjectKey(value);
        String valueType = findObjectValueType(value);

        if (objectKey != null) {
          list.add(Map.of("valueType", valueType, objectKey, value, "key",
              key.substring("attr#".length())));
        } else {
          list.add(Map.of("valueType", valueType, "key", key.substring("attr#".length())));
        }
      }
    }

    return list;
  }

  private String findObjectValueType(final Object value) {
    String objectKey = "KEY_ONLY";

    if (value instanceof Collection<?> c && !c.isEmpty()) {
      objectKey = findObjectValueType(c.iterator().next());
    } else if (value instanceof Double) {
      objectKey = "NUMBER";
    } else if (value instanceof Boolean) {
      objectKey = "BOOLEAN";
    } else if (value instanceof String) {
      objectKey = "STRING";
    }

    return objectKey;
  }

  private String findObjectKey(final Object value) {
    String objectKey = null;

    if (value instanceof Collection<?> c && !c.isEmpty()) {
      objectKey = findObjectKey(c.iterator().next()) + "s";
    } else if (value instanceof Double) {
      objectKey = "numberValue";
    } else if (value instanceof Boolean) {
      objectKey = "booleanValue";
    } else if (value instanceof String) {
      objectKey = "stringValue";
    }

    return objectKey;
  }
}
