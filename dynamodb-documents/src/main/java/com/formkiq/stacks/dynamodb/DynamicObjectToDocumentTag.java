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
import java.util.function.Function;
import com.formkiq.stacks.common.objects.DynamicObject;

/**
 * 
 * {@link Function} to convert {@link DynamicObject} to {@link DocumentTag}.
 *
 */
public class DynamicObjectToDocumentTag implements Function<DynamicObject, DocumentTag> {

  @Override
  public DocumentTag apply(final DynamicObject t) {
    DocumentTag tag = new DocumentTag();
    tag.setDocumentId(t.getString("documentId"));
    tag.setKey(t.getString("key"));

    if (t.hasString("type")) {
      tag.setType(DocumentTagType.valueOf(t.getString("type").toUpperCase()));
    } else {
      tag.setType(DocumentTagType.USERDEFINED);
    }
    tag.setUserId(t.getString("userId"));
    tag.setValue(t.getString("value"));
    tag.setInsertedDate(t.getDate("insertedDate"));

    if (tag.getInsertedDate() == null) {
      tag.setInsertedDate(new Date());
    }

    return tag;
  }

}
