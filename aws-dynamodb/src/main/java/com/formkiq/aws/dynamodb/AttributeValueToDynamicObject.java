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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DynamicObject}.
 *
 */
public class AttributeValueToDynamicObject
    implements Function<Map<String, AttributeValue>, DynamicObject> {

  @Override
  public DynamicObject apply(final Map<String, AttributeValue> map) {

    DynamicObject o = new DynamicObject(new HashMap<>());

    for (Map.Entry<String, AttributeValue> e : map.entrySet()) {

      if (e.getValue().hasL()) {

        List<String> values =
            e.getValue().l().stream().map(s -> s.s()).collect(Collectors.toList());
        o.put(e.getKey(), values);

      } else {

        String s = e.getValue().s();
        String n = e.getValue().n();
        String v = s == null && n != null ? n : s;
        o.put(e.getKey(), v);
      }
    }

    return o;
  }
}
