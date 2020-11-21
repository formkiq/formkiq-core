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
package com.formkiq.stacks.dynamodb;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link Preset}.
 *
 */
public class AttributeValueToPreset
    implements Function<Map<String, AttributeValue>, Preset>, DbKeys {

  /** Number of splits. */
  private static final int GSI2_SK_LENGTH = 3;
  /** {@link AttributeValueToInsertedDate}. */
  private AttributeValueToInsertedDate toDate = new AttributeValueToInsertedDate();

  @Override
  public Preset apply(final Map<String, AttributeValue> map) {

    Preset item = new Preset();

    if (map.containsKey(GSI2_SK)) {
      String[] s = map.get(GSI2_SK).s().split("\t");
      if (s.length == GSI2_SK_LENGTH) {
        item.setType(s[0]);
      }
    }

    if (map.containsKey("documentId")) {
      item.setId(map.get("documentId").s());
    }

    if (map.containsKey("id")) {
      item.setId(map.get("id").s());
    }

    if (map.containsKey("type")) {
      item.setType(map.get("type").s());
    }

    if (map.containsKey("tagKey")) {
      item.setName(map.get("tagKey").s());
    }

    Date date = this.toDate.apply(map);
    item.setInsertedDate(date);
    item.setUserId(map.containsKey("userId") ? map.get("userId").s() : null);

    return item;
  }
}
