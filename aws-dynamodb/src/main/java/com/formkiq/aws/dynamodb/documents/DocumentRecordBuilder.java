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
package com.formkiq.aws.dynamodb.documents;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamoDbKey;
import com.formkiq.aws.dynamodb.builder.DynamoDbEntityBuilder;
import com.formkiq.aws.dynamodb.builder.DynamoDbTypes;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.aws.dynamodb.objects.Strings;
import com.formkiq.validation.ValidationBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;
import static com.formkiq.aws.dynamodb.objects.Strings.removeQuotes;
import static com.formkiq.strings.Strings.isEmpty;

/**
 * {@link DynamoDbEntityBuilder} for {@link DocumentRecord}.
 */
public class DocumentRecordBuilder implements DynamoDbEntityBuilder<DocumentRecord>, DbKeys {

  /** Document Id. */
  private String documentId;
  /** Belongs To Document Id. */
  private String belongsToDocumentId;
  /** Document Path. */
  private String path;
  /** Deep Link Path. */
  private String deepLinkPath;
  /** Content Type. */
  private String contentType;
  /** Content Length. */
  private Long contentLength;
  /** Checksum. */
  private String checksum;
  /** Checksum Type. */
  private String checksumType;
  /** S3 Version. */
  private String s3version;
  /** User Id. */
  private String userId;
  /** Version. */
  private String version;
  /** Width. */
  private String width;
  /** Height. */
  private String height;
  /** Time To Live. */
  private String timeToLive;
  /** Inserted Date. */
  private Date insertedDate;
  /** Last Modified Date. */
  private Date lastModifiedDate;
  /** {@link DocumentMetadata}. */
  private Collection<DocumentMetadata> metadata;
  /** Set GSI1. */
  private boolean gsi1;
  /** Parent Document Id. */
  private String parentDocumentId;
  /** Original Values default values. */
  private DocumentRecord defaultValues;

  /**
   * Set Belongs To Document Id.
   *
   * @param id {@link String}
   * @return {@link DocumentRecordBuilder}
   */
  public DocumentRecordBuilder belongsToDocumentId(final String id) {
    this.belongsToDocumentId = id;
    return this;
  }

  @Override
  public DocumentRecord build(final DynamoDbKey key) {
    updateDeepLink();
    Collection<DocumentMetadata> documentMetadata = updateDefaultValues();

    validate();

    Map<String, AttributeValue> prev =
        defaultValues != null ? defaultValues.getAttributes() : Collections.emptyMap();

    String cs = ps(checksum, prev, "checksum");
    return new DocumentRecord(key, documentId, ps(belongsToDocumentId, prev, "belongsToDocumentId"),
        ps(path, prev, "path"), deepLinkPath, ps(contentType, prev, "contentType"),
        ps(contentLength, prev, "contentLength"), cs != null ? removeQuotes(cs) : null,
        ps(checksumType, prev, "checksumType"), ps(s3version, prev, "s3version"),
        ps(userId, prev, "userId"), ps(version, prev, "version"), ps(width, prev, "width"),
        ps(height, prev, "height"), ps(timeToLive, prev, "TimeToLive"), insertedDate,
        lastModifiedDate, documentMetadata);
  }

  @Override
  public DocumentRecord build(final String siteId) {
    DynamoDbKey key = buildKey(siteId);
    return build(key);
  }

  /**
   * Build DynamoDB key for a document item.
   *
   * <p>
   * This follows a common FormKiQ pattern:
   * <ul>
   * <li>PK: {@code document#<documentId>}</li>
   * <li>SK: {@code document}</li>
   * </ul>
   *
   * If your existing schema differs (e.g., uses {@code documents#} or has GSIs), adjust
   * accordingly.
   */
  @Override
  public DynamoDbKey buildKey(final String siteId) {

    setDefaults();
    Objects.requireNonNull(documentId, "documentId must not be null");

    String pk =
        parentDocumentId != null ? PREFIX_DOCS + parentDocumentId : PREFIX_DOCS + documentId;
    String sk = parentDocumentId != null ? "document" + TAG_DELIMINATOR + documentId : "document";

    DynamoDbKey.Builder builder = DynamoDbKey.builder().pk(siteId, pk).sk(sk);

    if (gsi1) {
      String shortdate = DateUtil.getYyyyMmDdFormatter().format(insertedDate);
      String gsi1Pk = DbKeys.PREFIX_DOCUMENT_DATE_TS + shortdate;

      DateUtil.getIsoDateFormatter();
      String fullInsertedDate = DateUtil.getIsoDateFormatter().format(insertedDate);
      String gsi1Sk = fullInsertedDate + DbKeys.TAG_DELIMINATOR + documentId;

      builder.gsi1Pk(siteId, gsi1Pk).gsi1Sk(gsi1Sk);
    }

    return builder.build();
  }

  public DocumentRecordBuilder checksum(final String cs) {
    this.checksum = cs;
    return this;
  }

  public DocumentRecordBuilder checksumType(final String ct) {
    this.checksumType = ct;
    return this;
  }

  public DocumentRecordBuilder contentLength(final Long cl) {
    this.contentLength = cl;
    return this;
  }

  public DocumentRecordBuilder contentType(final String ct) {
    this.contentType = ct;
    return this;
  }

  public DocumentRecordBuilder deepLinkPath(final String dl) {
    this.deepLinkPath = dl;
    return this;
  }

  public DocumentRecordBuilder documentId(final String id) {
    this.documentId = id;
    return this;
  }

  /**
   * Set GSI 1.
   *
   * @param setGsi1 {@link boolean}
   * @return {@link DocumentRecordBuilder}
   */
  public DocumentRecordBuilder gsi1(final boolean setGsi1) {
    this.gsi1 = setGsi1;
    return this;
  }

  public DocumentRecordBuilder height(final String h) {
    this.height = h;
    return this;
  }

  public DocumentRecordBuilder insertedDate(final Date d) {
    this.insertedDate = d == null ? null : new Date(d.getTime());
    return this;
  }

  public DocumentRecordBuilder lastModifiedDate(final Date d) {
    this.lastModifiedDate = d == null ? null : new Date(d.getTime());
    return this;
  }

  public DocumentRecordBuilder metadata(final Collection<DocumentMetadata> md) {
    this.metadata = md;
    return this;
  }

  public DocumentRecordBuilder parentDocumentId(final String id) {
    this.parentDocumentId = id;
    return this;
  }

  public DocumentRecordBuilder path(final String filepath) {
    this.path = filepath;
    return this;
  }

  private Long ps(final Long current, final Map<String, AttributeValue> map, final String key) {
    return current != null ? current : DynamoDbTypes.toLong(map.get(key));
  }

  private String ps(final String current, final Map<String, AttributeValue> map, final String key) {
    return !isEmpty(current) ? current : DynamoDbTypes.toString(map.get(key));
  }

  public DocumentRecordBuilder s3version(final String v) {
    this.s3version = v;
    return this;
  }

  private void setDeepLinkContentType() {

    if (!Strings.isEmpty(deepLinkPath) && Strings.isEmpty(contentType)) {

      Map<String, String> googleContentTypes = Map.of("document",
          "application/vnd.google-apps.document", "spreadsheets",
          "application/vnd.google-apps.spreadsheet", "forms", "application/vnd.google-apps.form",
          "presentation", "application/vnd.google-apps.presentation");

      for (Map.Entry<String, String> e : googleContentTypes.entrySet()) {
        if (deepLinkPath.startsWith("https://docs.google.com/" + e.getKey())) {
          this.contentType = e.getValue();
        }
      }
    }
  }

  /**
   * Set Default Values.
   *
   * @param record {@link DocumentRecord}
   * @return DocumentRecordBuilder
   */
  public DocumentRecordBuilder setDefaultValues(final DocumentRecord record) {
    defaultValues = record;
    insertedDate = record.insertedDate();
    return this;
  }

  private void setDefaults() {
    if (insertedDate == null) {
      insertedDate = new Date();
    }

    if (lastModifiedDate == null) {
      lastModifiedDate = insertedDate;
    }

    if (isEmpty(path)) {
      path = documentId;
    }

    setDeepLinkContentType();
  }

  public DocumentRecordBuilder timeToLive(final String ttl) {
    this.timeToLive = ttl;
    return this;
  }

  private void updateDeepLink() {
    if (!isEmpty(deepLinkPath)) {

      if (isEmpty(path) || path.equals(documentId)) {

        String filename = Strings.getFilename(deepLinkPath);

        if (!isEmpty(filename)) {
          path = filename;
        }
      }
    }
  }

  private Collection<DocumentMetadata> updateDefaultValues() {

    Collection<DocumentMetadata> documentMetaData =
        metadata != null ? new ArrayList<>(metadata) : new ArrayList<>();

    if (defaultValues != null) {

      var metadataKeys =
          notNull(documentMetaData.stream().map(DocumentMetadata::key).collect(Collectors.toSet()));

      for (DocumentMetadata meta : notNull(defaultValues.metadata())) {
        if (!metadataKeys.contains(meta.key())) {
          documentMetaData.add(meta);
        }
      }
    }

    notNull(documentMetaData).removeIf(DocumentMetadata::isEmpty);

    return documentMetaData;
  }

  public DocumentRecordBuilder userId(final String u) {
    this.userId = u;
    return this;
  }

  private void validate() {
    ValidationBuilder vb = new ValidationBuilder();

    validateDeepLinkPath(vb);
    validateDocumentPath(vb);

    validateDimension(vb, "width", width);
    validateDimension(vb, "height", height);

    vb.check();
  }

  private void validateDeepLinkPath(final ValidationBuilder vb) {
    if (!Strings.isEmpty(deepLinkPath)) {
      if (!Strings.isUrl(deepLinkPath)) {
        vb.addError("deepLinkPath", "DeepLinkPath '" + deepLinkPath + "' is not a valid URL");
      }
    }
  }

  private void validateDimension(final ValidationBuilder vb, final String key,
      final String dimension) {
    if (!Strings.isEmpty(dimension)) {

      if (!"auto".equals(dimension)) {
        if (!com.formkiq.strings.Strings.isNumeric(dimension)) {
          vb.addError(key, "invalid '" + key + "' must be numeric or 'auto'");
        }
      }
    }
  }

  private void validateDocumentPath(final ValidationBuilder vb) {
    if (path != null && path.contains("//")) {
      vb.addError("path", "invalid Path contains multiple '//' characters");
    }
  }

  public DocumentRecordBuilder version(final String v) {
    this.version = v;
    return this;
  }

  public DocumentRecordBuilder width(final String w) {
    this.width = w;
    return this;
  }
}
