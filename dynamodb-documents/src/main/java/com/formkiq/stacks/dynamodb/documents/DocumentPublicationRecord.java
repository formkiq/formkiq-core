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

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * Document Publication Record.
 */
public class DocumentPublicationRecord
    implements DynamodbRecord<DocumentPublicationRecord>, DbKeys {

  /** S3 Version. */
  private String s3version;
  /** Content Type. */
  private String contentType;
  /** Path. */
  private String path;
  /** Document Id. */
  private String documentId;
  /** User Id. */
  private String userId;

  /**
   * constructor.
   */
  public DocumentPublicationRecord() {}

  /**
   * Get User Id.
   * 
   * @return String
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set User Id.
   * 
   * @param publicationUserId {@link String}
   * @return DocumentPublicationRecord
   */
  public DocumentPublicationRecord setUserId(final String publicationUserId) {
    this.userId = publicationUserId;
    return this;
  }

  /**
   * Get Document Id.
   * 
   * @return {@link String}
   */
  public String getDocumentId() {
    return this.documentId;
  }

  /**
   * Set Document Id.
   * 
   * @param id {@link String}
   * @return {@link DocumentPublicationRecord}
   */
  public DocumentPublicationRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = new HashMap<>(getDataAttributes());

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));

    return map;
  }

  /**
   * Get Path.
   *
   * @return String
   */
  public String getPath() {
    return this.path;
  }

  /**
   * Set Path.
   *
   * @param documentPath {@link String}
   * @return DocumentPublicationRecord
   */
  public DocumentPublicationRecord setPath(final String documentPath) {
    this.path = documentPath;
    return this;
  }

  /**
   * Get S3 Version.
   *
   * @return String
   */
  public String getS3version() {
    return this.s3version;
  }

  /**
   * Get Content Type.
   *
   * @return String
   */
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Set Content Type.
   *
   * @param documentContentType {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublicationRecord setContentType(final String documentContentType) {
    this.contentType = documentContentType;
    return this;
  }

  /**
   * Set S3 Version.
   *
   * @param version {@link String}
   * @return DocumentPublishRecord
   */
  public DocumentPublicationRecord setS3version(final String version) {
    this.s3version = version;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {

    Map<String, AttributeValue> map = new HashMap<>();
    map.put("documentId", fromS(getDocumentId()));
    map.put("path", fromS(getPath()));
    map.put("userId", fromS(getUserId()));
    map.put("s3Version", fromS(getS3version()));
    map.put("contentType", fromS(getContentType()));

    return map;
  }

  @Override
  public DocumentPublicationRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    DocumentPublicationRecord record = null;

    if (!attrs.isEmpty()) {

      record = new DocumentPublicationRecord().setDocumentId(ss(attrs, "documentId"))
          .setPath(ss(attrs, "path")).setContentType(ss(attrs, "contentType"))
          .setS3version(ss(attrs, "s3Version")).setUserId(ss(attrs, "userId"));
    }

    return record;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, PREFIX_DOCS + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    return null;
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    return "publication";
  }

  @Override
  public String skGsi1() {
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
  }
}
