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
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * Convert {@link QueryResponse} to {@link PaginationMapToken}.
 *
 */
public class QueryResponseToPagination implements Function<QueryResponse, PaginationMapToken> {

  @Override
  public PaginationMapToken apply(final QueryResponse result) {

    PaginationMapToken token = null;
    Map<String, AttributeValue> key = result.lastEvaluatedKey();

    if (key != null && key.size() > 0) {

      Map<String, Object> map = new HashMap<>();

      for (Map.Entry<String, AttributeValue> e : key.entrySet()) {
        map.put(e.getKey(), e.getValue().s());
      }

      token = new PaginationMapToken(map);
    }

    return token;
  }
}
