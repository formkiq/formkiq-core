/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
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
