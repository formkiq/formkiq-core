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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.stacks.common.objects.DynamicObject;

/**
 * 
 * {@link Function} to convert {@link DynamicObject} to {@link DocumentItem}.
 *
 */
public class DynamicObjectToDocumentItem implements Function<DynamicObject, DocumentItem> {

  @Override
  public DocumentItem apply(final DynamicObject t) {

    DocumentItem item = new DynamicDocumentItem(Collections.emptyMap());

    item.setBelongsToDocumentId(t.getString("belongsToDocumentId"));
    item.setChecksum(t.getString("checksum"));
    item.setContentLength(t.getLong("contentLength"));
    item.setContentType(t.getString("contentType"));
    item.setDocumentId(t.getString("documentId"));
    item.setInsertedDate(t.getDate("insertedDate"));
    item.setPath(t.getString("path"));
    item.setUserId(t.getString("userId"));

    List<DynamicObject> list = t.getList("documents");
    List<DocumentItem> documents = list.stream().map(l -> {
      DocumentItem i = new DynamicDocumentItem(Collections.emptyMap());
      i.setDocumentId(l.getString("documentId"));
      return i;
    }).collect(Collectors.toList());

    item.setDocuments(documents);

    return item;
  }
}
