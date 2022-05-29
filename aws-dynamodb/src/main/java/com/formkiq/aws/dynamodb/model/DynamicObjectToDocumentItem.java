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
package com.formkiq.aws.dynamodb.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DynamicObject;

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
