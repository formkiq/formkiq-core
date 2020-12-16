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
    Optional<Map<String, AttributeValue>> r = items.stream()
        .filter(i -> i.containsKey("SK") && "document".equals(i.get("SK").s())).findFirst();

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
        documents.forEach(d -> d.setBelongsToDocumentId(null));
      }
    }

    return item;
  }

  /**
   * Convert {@link Map} {@link String} {@link AttributeValue} to {@link DocumentItem}.
   * @param map {@link Map} {@link String} {@link AttributeValue}
   * @return {@link DocumentItem}
   */
  public DocumentItem apply(final Map<String, AttributeValue> map) {

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
