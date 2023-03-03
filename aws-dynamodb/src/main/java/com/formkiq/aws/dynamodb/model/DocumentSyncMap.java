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

import java.util.Date;
import java.util.Map;
import com.formkiq.aws.dynamodb.DynamicObject;

/**
 * 
 * {@link DynamicObject} of {@link DocumentSync}.
 *
 */
public class DocumentSyncMap extends DynamicObject implements DocumentSync {

  /** serialVersionUID. */
  private static final long serialVersionUID = -2697905810803353090L;

  /**
   * constructor.
   * 
   * @param map {@link Map}
   */
  public DocumentSyncMap(final Map<String, Object> map) {
    super(map);
  }

  @Override
  public String getDocumentId() {
    return getString("documentId");
  }

  @Override
  public DocumentSyncServiceType getService() {
    String service = getString("service");
    return service != null ? DocumentSyncServiceType.valueOf(service) : null;
  }

  @Override
  public DocumentSyncStatus getStatus() {
    String status = getString("status");
    return status != null ? DocumentSyncStatus.valueOf(status) : null;
  }

  @Override
  public Date getSyncdDate() {
    return getDate("syncDate");
  }

  @Override
  public DocumentSyncType getType() {
    String type = getString("type");
    return type != null ? DocumentSyncType.valueOf(type) : null;
  }

  @Override
  public String getUserId() {
    return getString("userId");
  }

  @Override
  public void setDocumentId(final String id) {
    put("documentId", id);
  }

  @Override
  public void setService(final DocumentSyncServiceType service) {
    put("service", service.name());
  }

  @Override
  public void setStatus(final DocumentSyncStatus status) {
    put("status", status.name());
  }

  @Override
  public void setSyncDate(final Date date) {
    put("syncDate", date);
  }

  @Override
  public void setType(final DocumentSyncType type) {
    put("type", type.name());
  }

  @Override
  public void setUserId(final String id) {
    put("userId", id);
  }
}
