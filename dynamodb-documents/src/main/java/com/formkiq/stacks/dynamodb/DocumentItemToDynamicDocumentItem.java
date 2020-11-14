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

import static com.formkiq.stacks.common.objects.Objects.notNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 
 * {@link Function} to convert {@link DocumentItem} to {@link DynamicDocumentItem}.
 *
 */
public class DocumentItemToDynamicDocumentItem
    implements Function<DocumentItem, DynamicDocumentItem> {

  @Override
  public DynamicDocumentItem apply(final DocumentItem item) {

    DynamicDocumentItem ditem = convert(item);

    List<DynamicDocumentItem> children =
        notNull(item.getDocuments()).stream().map(d -> convert(d)).collect(Collectors.toList());

    if (!children.isEmpty()) {
      ditem.put("documents", children);
    }

    return ditem;
  }

  private DynamicDocumentItem convert(final DocumentItem item) {

    Map<String, Object> map = new HashMap<>();
    map.put("checksum", item.getChecksum());
    map.put("contentLength", item.getContentLength());
    map.put("contentType", item.getContentType());
    map.put("documentId", item.getDocumentId());
    map.put("insertedDate", item.getInsertedDate());
    map.put("path", item.getPath());
    map.put("userId", item.getUserId());
    map.put("belongsToDocumentId", item.getBelongsToDocumentId());

    return new DynamicDocumentItem(map);
  }
}
