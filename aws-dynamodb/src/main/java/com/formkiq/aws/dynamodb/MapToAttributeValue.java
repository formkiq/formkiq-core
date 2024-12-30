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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Convert {@link Map} to {@link AttributeValue} {@link Map}.
 *
 */
public class MapToAttributeValue
    implements Function<Map<String, Object>, Map<String, AttributeValue>> {

  @Override
  public Map<String, AttributeValue> apply(final Map<String, Object> map) {

    Map<String, AttributeValue> result = null;

    if (map != null) {
      result = new HashMap<>();
      for (Map.Entry<String, Object> e : map.entrySet()) {
        AttributeValue a = convert(e.getValue());
        result.put(e.getKey(), a);
      }
    }

    return result;
  }

  private AttributeValue convert(final Object obj) {
    AttributeValue o = null;
    if (obj instanceof Double d) {
      o = AttributeValue.fromN(String.valueOf(d));
    } else if (obj instanceof Long l) {
      o = AttributeValue.fromN(String.valueOf(l));
    } else if (obj instanceof String s) {
      o = AttributeValue.fromS(s);
    } else if (obj instanceof Collection<?> c) {
      o = AttributeValue.fromL(c.stream().map(this::convert).toList());
    } else {
      throw new IllegalArgumentException("Unsupported data type: " + obj.getClass().getName());
    }

    return o;
  }
}
