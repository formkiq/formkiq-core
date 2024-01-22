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

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;

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
    map.put("lastModifiedDate", item.getLastModifiedDate());
    map.put("path", item.getPath());
    map.put("deepLinkPath", item.getDeepLinkPath());
    map.put("userId", item.getUserId());
    map.put("belongsToDocumentId", item.getBelongsToDocumentId());
    map.put("TimeToLive", item.getTimeToLive());
    map.put("metadata", item.getMetadata());

    return new DynamicDocumentItem(map);
  }
}
