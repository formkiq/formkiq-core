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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link PaginationMapToken} to {@link Map} {@link AttributeValue}.
 *
 */
public class PaginationToAttributeValue
    implements Function<PaginationMapToken, Map<String, AttributeValue>> {

  @Override
  public Map<String, AttributeValue> apply(final PaginationMapToken token) {

    Map<String, AttributeValue> map = null;

    if (token != null) {
      map = new HashMap<>();
      for (Map.Entry<String, Object> e : token.getAttributeMap().entrySet()) {
        map.put(e.getKey(), AttributeValue.builder().s(e.getValue().toString()).build());
      }
    }

    return map;
  }
}
