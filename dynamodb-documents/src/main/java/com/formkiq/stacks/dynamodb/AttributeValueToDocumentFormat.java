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
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentFormat}.
 *
 */
public class AttributeValueToDocumentFormat
    implements Function<Map<String, AttributeValue>, DocumentFormat> {

  /** {@link AttributeValueToInsertedDate}. */
  private AttributeValueToInsertedDate toDate = new AttributeValueToInsertedDate();

  @Override
  public DocumentFormat apply(final Map<String, AttributeValue> map) {

    Date date = this.toDate.apply(map);

    DocumentFormat item = new DocumentFormat();
    item.setContentType(map.get("contentType").s());
    item.setDocumentId(map.get("documentId").s());
    item.setInsertedDate(date);
    item.setUserId(map.containsKey("userId") ? map.get("userId").s() : null);

    return item;
  }
}
