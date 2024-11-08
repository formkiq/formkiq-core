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
package com.formkiq.aws.dynamodb;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Map}.
 *
 */
public class AttributeValueToMap
    implements Function<Map<String, AttributeValue>, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(final Map<String, AttributeValue> map) {

    Map<String, Object> result = new HashMap<>();

    if (map != null) {
      for (Map.Entry<String, AttributeValue> e : notNull(map.entrySet())) {

        String key = getKey(e.getKey());

        Object obj = convert(e.getValue());
        result.put(key, obj);
      }
    }

    return result;
  }

  private Object convert(final AttributeValue val) {
    Object obj;

    switch (val.type()) {
      case S -> obj = val.s();
      case N -> obj = val.n();
      case BOOL -> obj = val.b();
      case L -> obj = val.l().stream().map(this::convert).toList();
      default -> throw new IllegalArgumentException(
          "Unsupported attribute value map conversion " + val.type());
    }

    return obj;
  }

  private String getKey(final String key) {

    String k = key;
    if ("inserteddate".equals(key)) {
      k = "insertedDate";
    } else if (k.startsWith("fk#")) {
      final int pos = 3;
      k = k.substring(pos);
    }

    return k;
  }
}
