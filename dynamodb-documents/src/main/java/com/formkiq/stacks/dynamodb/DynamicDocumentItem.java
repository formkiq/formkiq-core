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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.stacks.common.objects.DynamicObject;

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
  public String getDocumentId() {
    return getString("documentId");
  }

  @Override
  public Date getInsertedDate() {
    return getDate("insertedDate");
  }

  @Override
  public String getPath() {
    return getString("path");
  }

  @Override
  public String getUserId() {
    return getString("userId");
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
  public void setDocumentId(final String documentId) {
    put("documentId", documentId);
  }

  @Override
  public void setInsertedDate(final Date date) {
    put("insertedDate", date);
  }

  @Override
  public void setPath(final String path) {
    put("path", path);
  }

  @Override
  public void setUserId(final String userId) {
    put("userId", userId);
  }

  @Override
  public String getBelongsToDocumentId() {
    return getString("belongsToDocumentId");
  }

  @Override
  public List<DocumentItem> getDocuments() {
    List<DynamicObject> list = getList("documents");
    return list.stream().map(o -> new DynamicObjectToDocumentItem().apply(o))
        .collect(Collectors.toList());
  }

  @Override
  public void setBelongsToDocumentId(final String documentId) {
    put("belongsToDocumentId", documentId);
  }

  @Override
  public void setDocuments(final List<DocumentItem> list) {
    put("documents", list);

  }
}
