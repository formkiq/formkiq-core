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
package com.formkiq.stacks.dynamodb.documents;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import com.formkiq.aws.dynamodb.model.DocumentItem;
import com.formkiq.aws.dynamodb.model.DocumentMetadata;
import com.formkiq.graalvm.annotations.Reflectable;

/**
 * Document Record.
 */
@Reflectable
public class Document implements DocumentItem {

  /** Belongs to document id. */
  private String belongsToDocumentId;
  /** Checksum. */
  private String checksum;
  /** Content Length. */
  private Long contentLength;
  /** Content Type. */
  private String contentType;
  /** Deep Link Path. */
  private String deepLinkPath;
  /** Document Id. */
  private String documentId;
  /** Child Documents. */
  private List<DocumentItem> documents;
  /** Inserted Date. */
  private Date insertedDate;
  /** Last Modified Date. */
  private Date lastModifiedDate;
  /** {@link Collection} {@link DocumentMetadata}. */
  private Collection<DocumentMetadata> metadata;
  /** Path. */
  private String path;
  /** S3 Version. */
  private String s3version;
  /** Tag Schema Id. */
  private String tagSchemaId;
  /** Time to Live. */
  private String timeToLive;
  /** User Id. */
  private String userId;
  /** Version. */
  private String version;

  /**
   * constructor.
   */
  public Document() {}

  @Override
  public String getBelongsToDocumentId() {
    return this.belongsToDocumentId;
  }

  @Override
  public String getChecksum() {
    return this.checksum;
  }

  @Override
  public Long getContentLength() {
    return this.contentLength;
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public String getDeepLinkPath() {
    return this.deepLinkPath;
  }

  @Override
  public String getDocumentId() {
    return this.documentId;
  }

  @Override
  public List<DocumentItem> getDocuments() {
    return this.documents;
  }

  @Override
  public Date getInsertedDate() {
    return this.insertedDate;
  }

  @Override
  public Date getLastModifiedDate() {
    return this.lastModifiedDate;
  }

  @Override
  public Collection<DocumentMetadata> getMetadata() {
    return this.metadata;
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public String getS3version() {
    return this.s3version;
  }

  @Override
  public String getTagSchemaId() {
    return this.tagSchemaId;
  }

  @Override
  public String getTimeToLive() {
    return this.timeToLive;
  }

  @Override
  public String getUserId() {
    return this.userId;
  }

  @Override
  public String getVersion() {
    return this.version;
  }

  @Override
  public void setBelongsToDocumentId(final String docId) {
    this.belongsToDocumentId = docId;
  }

  @Override
  public void setChecksum(final String documentChecksum) {
    this.checksum = documentChecksum;
  }

  @Override
  public void setContentLength(final Long documentContentLength) {
    this.contentLength = documentContentLength;
  }

  @Override
  public void setContentType(final String documentContentType) {
    this.contentType = documentContentType;
  }

  @Override
  public void setDeepLinkPath(final String documentDeepLinkPath) {
    this.deepLinkPath = documentDeepLinkPath;
  }

  @Override
  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  @Override
  public void setDocuments(final List<DocumentItem> childDocuments) {
    this.documents = childDocuments;
  }

  @Override
  public void setInsertedDate(final Date date) {
    this.insertedDate = date;
  }

  @Override
  public void setLastModifiedDate(final Date date) {
    this.lastModifiedDate = date;
  }

  @Override
  public void setMetadata(final Collection<DocumentMetadata> documentMetadata) {
    this.metadata = documentMetadata;
  }

  @Override
  public void setPath(final String documentPath) {
    this.path = documentPath;
  }

  @Override
  public void setS3version(final String documentVersion) {
    this.s3version = documentVersion;
  }

  @Override
  public void setTagSchemaId(final String documentTagSchemaId) {
    this.tagSchemaId = documentTagSchemaId;
  }

  @Override
  public void setTimeToLive(final String ttl) {
    this.timeToLive = ttl;
  }

  @Override
  public void setUserId(final String username) {
    this.userId = username;
  }

  @Override
  public void setVersion(final String documentVersion) {
    this.version = documentVersion;
  }
}
