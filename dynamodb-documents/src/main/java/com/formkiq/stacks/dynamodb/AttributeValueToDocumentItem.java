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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentItem}.
 *
 */
public class AttributeValueToDocumentItem
    implements Function<List<Map<String, AttributeValue>>, DocumentItem> {

  /** {@link AttributeValueToInsertedDate}. */
  private AttributeValueToInsertedDate toDate = new AttributeValueToInsertedDate();

  @Override
  public DocumentItem apply(final List<Map<String, AttributeValue>> items) {

    DocumentItem item = null;

    Optional<Map<String, AttributeValue>> r =
        items.stream().filter(i -> "document".equals(i.get("SK").s())).findFirst();

    if (r.isPresent()) {

      item = apply(r.get());

      List<DocumentItem> documents = new ArrayList<>();
      List<Map<String, AttributeValue>> children =
          items.stream().filter(i -> !i.equals(r.get())).collect(Collectors.toList());

      for (Map<String, AttributeValue> map : children) {
        documents.add(apply(map));
      }

      if (!documents.isEmpty()) {
        item.setDocuments(documents);
      }
    }

    return item;
  }

  private DocumentItem apply(final Map<String, AttributeValue> map) {

    String id = map.get("documentId").s();
    String userId = map.containsKey("userId") ? map.get("userId").s() : null;

    Date date = this.toDate.apply(map);
    DocumentItemDynamoDb item = new DocumentItemDynamoDb(id, date, userId);

    if (map.containsKey("path")) {
      item.setPath(map.get("path").s());
    }

    if (map.containsKey("contentType")) {
      item.setContentType(map.get("contentType").s());
    }

    if (map.containsKey("contentLength")) {
      Long contentType = Long.valueOf(map.get("contentLength").n());
      item.setContentLength(contentType);
    }

    if (map.containsKey("etag")) {
      item.setChecksum(map.get("etag").s());
    }

    if (map.containsKey("belongsToDocumentId")) {
      item.setBelongsToDocumentId(map.get("belongsToDocumentId").s());
    }

    return item;
  }
}
