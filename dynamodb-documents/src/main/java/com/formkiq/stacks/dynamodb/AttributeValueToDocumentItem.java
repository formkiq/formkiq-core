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

import static com.formkiq.aws.dynamodb.DbKeys.PREFIX_DOCUMENT_METADATA;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Convert {@link Map} {@link AttributeValue} to {@link DocumentItem}.
 *
 */
public class AttributeValueToDocumentItem
    implements Function<List<Map<String, AttributeValue>>, DocumentItem> {

  /** {@link AttributeValueToDate}. */
  private AttributeValueToDate toInsertedDate = new AttributeValueToDate("inserteddate");
  /** {@link AttributeValueToDate}. */
  private AttributeValueToDate toModifiedDate = new AttributeValueToDate("lastModifiedDate");

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
   * 
   * @param map {@link Map} {@link String} {@link AttributeValue}
   * @return {@link DocumentItem}
   */
  public DocumentItem apply(final Map<String, AttributeValue> map) {

    String id = map.get("documentId").s();
    String userId = map.containsKey("userId") ? map.get("userId").s() : null;

    Date insertedDate = this.toInsertedDate.apply(map);

    Date lastModifiedDate = this.toModifiedDate.apply(map);

    DocumentItemDynamoDb item = new DocumentItemDynamoDb(id, insertedDate, userId);
    item.setLastModifiedDate(lastModifiedDate != null ? lastModifiedDate : insertedDate);

    item.setPath(getString(map.get("path")));
    item.setDeepLinkPath(getDeepLinkPath(map));

    if (map.containsKey("contentType")) {
      item.setContentType(map.get("contentType").s());
    }

    if (map.containsKey("contentLength")) {
      Long contentType = Long.valueOf(map.get("contentLength").n());
      item.setContentLength(contentType);
    }

    if (map.containsKey("checksum")) {
      item.setChecksum(map.get("checksum").s());
    }

    if (map.containsKey("belongsToDocumentId")) {
      item.setBelongsToDocumentId(map.get("belongsToDocumentId").s());
    }

    if (map.containsKey("tagSchemaId")) {
      item.setTagSchemaId(map.get("tagSchemaId").s());
    }

    item.setVersion(getString(map.get("version")));
    item.setS3version(getString(map.get(DocumentVersionService.S3VERSION_ATTRIBUTE)));

    if (map.containsKey("tagSchemaId")) {
      item.setTagSchemaId(map.get("tagSchemaId").s());
    }

    if (map.containsKey("TimeToLive")) {
      item.setTimeToLive(map.get("TimeToLive").n());
    }

    Collection<DocumentMetadata> metadata = toDocumentMetadata(map);
    item.setMetadata(metadata);

    return item;
  }

  private String getDeepLinkPath(final Map<String, AttributeValue> map) {
    return map.containsKey("deepLinkPath") ? getString(map.get("deepLinkPath")) : null;
  }

  private String getString(final AttributeValue value) {
    return value != null ? value.s() : null;
  }

  /**
   * Convert {@link Map} to {@link DocumentMetadata}.
   * 
   * @param map {@link Map} {@link String} {@link AttributeValue}
   * @return {@link Collection} {@link DocumentMetadata}
   */
  private Collection<DocumentMetadata> toDocumentMetadata(final Map<String, AttributeValue> map) {
    Collection<DocumentMetadata> c = null;

    List<String> metadataKeys = map.keySet().stream()
        .filter(k -> k.startsWith(PREFIX_DOCUMENT_METADATA)).collect(Collectors.toList());

    if (!metadataKeys.isEmpty()) {
      c = metadataKeys.stream().map(k -> {

        DocumentMetadata meta = null;
        AttributeValue av = map.get(k);

        String kk = k.substring(PREFIX_DOCUMENT_METADATA.length());

        if (av.s() != null) {
          meta = new DocumentMetadata(kk, map.get(k).s());
        } else {
          List<String> strs = av.l().stream().map(m -> m.s()).collect(Collectors.toList());
          meta = new DocumentMetadata(kk, strs);
        }

        return meta;
      }).collect(Collectors.toList());
    }

    return c;
  }
}
