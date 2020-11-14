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

/**
 * 
 * {@link Function} to convert {@link DocumentTag} to {@link DynamicDocumentTag}.
 *
 */
public class DocumentTagToDynamicDocumentTag implements Function<DocumentTag, DynamicDocumentTag> {

  @Override
  public DynamicDocumentTag apply(final DocumentTag tag) {
    Map<String, Object> map = new HashMap<>();
    map.put("documentId", tag.getDocumentId());
    map.put("key", tag.getKey());
    map.put("type", tag.getType().name());
    map.put("userId", tag.getUserId());
    map.put("value", tag.getValue());

    return new DynamicDocumentTag(map);
  }

}
