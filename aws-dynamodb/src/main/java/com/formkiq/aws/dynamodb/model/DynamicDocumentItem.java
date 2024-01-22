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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DynamicObject;

/**
 * 
 * {@link DynamicObject} implementation of the {@link DocumentItem}.
 *
 */
public class DynamicDocumentItem extends DynamicObject implements DocumentItem {

  /** serialVersionUID. */
  private static final long serialVersionUID = -2266479553745251309L;

  /**
   * constructor.
   * 
   * @param map {@link Map}
   */
  public DynamicDocumentItem(final Map<String, Object> map) {
    super(map);
  }

  @Override
  public String getBelongsToDocumentId() {
    return getString("belongsToDocumentId");
  }

  @Override
  public String getChecksum() {
    return getString("checksum");
  }

  @Override
  public Long getContentLength() {
    return getLong("contentLength");
  }

  @Override
  public String getContentType() {
    return getString("contentType");
  }

  @Override
  public String getDeepLinkPath() {
    return getString("deepLinkPath");
  }

  @Override
  public String getDocumentId() {
    return getString("documentId");
  }

  @Override
  public List<DocumentItem> getDocuments() {
    List<DynamicObject> list = getList("documents");
    return list.stream().map(o -> new DynamicObjectToDocumentItem().apply(o))
        .collect(Collectors.toList());
  }

  @Override
  public Date getInsertedDate() {
    return getDate("insertedDate");
  }

  @Override
  public Date getLastModifiedDate() {
    return getDate("lastModifiedDate");
  }

  @SuppressWarnings("unchecked")
  @Override
  public Collection<DocumentMetadata> getMetadata() {
    Collection<Map<String, Object>> c =
        (Collection<Map<String, Object>>) getOrDefault("metadata", null);

    Collection<DocumentMetadata> metadata = null;

    if (c != null) {
      metadata = c.stream().map(m -> {
        DocumentMetadata md = null;
        if (m.containsKey("values")) {
          md = new DocumentMetadata((String) m.get("key"), (List<String>) m.get("values"));
        } else {
          md = new DocumentMetadata((String) m.get("key"), (String) m.get("value"));
        }
        return md;
      }).collect(Collectors.toList());
    }

    return metadata;
  }

  @Override
  public String getPath() {
    return getString("path");
  }

  @Override
  public String getS3version() {
    return getString("s3version");
  }

  @Override
  public String getTagSchemaId() {
    return getString("tagSchemaId");
  }

  @Override
  public String getTimeToLive() {
    return getString("TimeToLive");
  }

  @Override
  public String getUserId() {
    return getString("userId");
  }

  @Override
  public String getVersion() {
    return getString("version");
  }

  @Override
  public void setBelongsToDocumentId(final String documentId) {
    put("belongsToDocumentId", documentId);
  }

  @Override
  public void setChecksum(final String checksum) {
    put("checksum", checksum);
  }

  @Override
  public void setContentLength(final Long contentLength) {
    put("contentLength", contentLength);
  }

  @Override
  public void setContentType(final String contentType) {
    put("contentType", contentType);
  }

  @Override
  public void setDeepLinkPath(final String deepLinkPath) {
    put("deepLinkPath", deepLinkPath);
  }

  @Override
  public void setDocumentId(final String documentId) {
    put("documentId", documentId);
  }

  @Override
  public void setDocuments(final List<DocumentItem> list) {
    put("documents", list);

  }

  @Override
  public void setInsertedDate(final Date date) {
    put("insertedDate", date);
  }

  @Override
  public void setLastModifiedDate(final Date date) {
    put("lastModifiedDate", date);
  }

  @Override
  public void setMetadata(final Collection<DocumentMetadata> metadata) {

    List<Map<String, ? extends Object>> list = metadata.stream()
        .map(m -> m.getValues() != null ? Map.of("key", m.getKey(), "values", m.getValues())
            : Map.of("key", m.getKey(), "value", m.getValue()))
        .collect(Collectors.toList());

    put("metadata", list);
  }

  @Override
  public void setPath(final String path) {
    put("path", path);
  }

  @Override
  public void setS3version(final String version) {
    put("s3version", version);
  }

  @Override
  public void setTagSchemaId(final String id) {
    put("tagSchemaId", id);
  }

  @Override
  public void setTimeToLive(final String ttl) {
    put("TimeToLive", ttl);
  }

  @Override
  public void setUserId(final String userId) {
    put("userId", userId);
  }

  @Override
  public void setVersion(final String version) {
    put("version", version);
  }
}
