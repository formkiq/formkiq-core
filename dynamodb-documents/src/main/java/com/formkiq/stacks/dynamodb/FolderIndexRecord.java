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

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * {@link DynamodbRecord} for Global Metadata Folder.
 *
 */
@Reflectable
public class FolderIndexRecord implements DynamodbRecord<FolderIndexRecord>, DbKeys {

  /** Index File SK. */
  public static final String INDEX_FILE_SK = "fi" + DbKeys.TAG_DELIMINATOR;
  /** Index Folder SK. */
  public static final String INDEX_FOLDER_SK = "ff" + DbKeys.TAG_DELIMINATOR;

  /** Document Id. */
  @Reflectable
  private String documentId;
  /** Record inserted date. */
  @Reflectable
  private Date insertedDate;
  /** Record modified date. */
  @Reflectable
  private Date lastModifiedDate;
  /** Parent Id. */
  @Reflectable
  private String parentDocumentId;
  /** Path. */
  @Reflectable
  private String path;
  /** Folder Type. */
  @Reflectable
  private String type;
  /** Creator of record. */
  @Reflectable
  private String userId;

  private void checkParentId() {
    if (this.parentDocumentId == null) {
      throw new IllegalArgumentException("'parentDocumentId' is required");
    }
  }

  /**
   * Create Index Key.
   * 
   * @param siteId {@link String}
   * @return {@link String}
   */
  public String createIndexKey(final String siteId) {
    String pk = pk(siteId);
    String parent = pk.substring(pk.lastIndexOf(TAG_DELIMINATOR) + 1);
    return parent + TAG_DELIMINATOR + this.path;
  }

  /**
   * Get DocumentId.
   * 
   * @return {@link String}
   */
  public String documentId() {
    return this.documentId;
  }

  /**
   * Set DocumentId.
   * 
   * @param id {@link String}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord documentId(final String id) {
    this.documentId = id;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    Map<String, AttributeValue> attrs =
        new HashMap<>(Map.of(DbKeys.PK, AttributeValue.fromS(pk(siteId)), DbKeys.SK,
            AttributeValue.fromS(sk()), "documentId", AttributeValue.fromS(this.documentId), "path",
            AttributeValue.fromS(this.path), "type", AttributeValue.fromS(this.type)));

    if (this.parentDocumentId != null) {
      attrs.put("parentDocumentId", AttributeValue.fromS(this.parentDocumentId));
    }
    if (this.insertedDate != null) {
      attrs.put("inserteddate", AttributeValue.fromS(df.format(this.insertedDate)));
    }

    if (this.lastModifiedDate != null) {
      attrs.put("lastModifiedDate", AttributeValue.fromS(df.format(this.lastModifiedDate)));
    }

    if (this.userId != null) {
      attrs.put("userId", AttributeValue.fromS(this.userId));
    }

    if ("folder".equals(this.type)) {
      attrs.put(GSI1_PK, AttributeValue.fromS(pkGsi1(siteId)));
      attrs.put(GSI1_SK, AttributeValue.fromS(skGsi1()));
    }

    return attrs;
  }

  @Override
  public FolderIndexRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    FolderIndexRecord record = new FolderIndexRecord().documentId(ss(attrs, "documentId"))
        .path(ss(attrs, "path")).type(ss(attrs, "type")).userId(ss(attrs, "userId"))
        .parentDocumentId(ss(attrs, "parentDocumentId"));

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    if (attrs.containsKey("inserteddate")) {
      try {
        record = record.insertedDate(df.parse(ss(attrs, "inserteddate")));
      } catch (ParseException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("invalid 'inserteddate'");
      }
    }

    if (attrs.containsKey("lastModifiedDate")) {
      try {
        record = record.lastModifiedDate(df.parse(ss(attrs, "lastModifiedDate")));
      } catch (ParseException e) {
        throw new IllegalArgumentException("invalid 'lastModifiedDate'");
      }
    }

    if (this.parentDocumentId == null) {
      String pk = ss(attrs, PK);
      String s = SiteIdKeyGenerator.resetDatabaseKey(siteId, pk);

      int pos = s.lastIndexOf(TAG_DELIMINATOR);
      if (pos > -1) {
        record.parentDocumentId = s.substring(pos + 1);
      } else {
        record.parentDocumentId = "";
      }
    }

    return record;
  }

  /**
   * Get Inserted Date.
   * 
   * @return {@link Date}
   */
  public Date insertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   * 
   * @param date {@link Date}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord insertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Get Last Modified Date.
   * 
   * @return {@link Date}
   */
  public Date lastModifiedDate() {
    return this.lastModifiedDate;
  }

  /**
   * Set Last Modified Date.
   * 
   * @param date {@link Date}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord lastModifiedDate(final Date date) {
    this.lastModifiedDate = date;
    return this;
  }

  /**
   * Get Parent Id.
   * 
   * @return {@link String}
   */
  public String parentDocumentId() {
    return this.parentDocumentId;
  }

  /**
   * Set Parent Id.
   * 
   * @param documentParentId {@link String}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord parentDocumentId(final String documentParentId) {
    this.parentDocumentId = documentParentId;
    return this;
  }

  /**
   * Get Document Path.
   * 
   * @return {@link String}
   */
  public String path() {
    return this.path;
  }

  /**
   * Set Document Path.
   * 
   * @param documentPath {@link String}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord path(final String documentPath) {
    this.path = documentPath;
    return this;
  }

  @Override
  public String pk(final String siteId) {
    checkParentId();
    return SiteIdKeyGenerator.createDatabaseKey(siteId,
        GLOBAL_FOLDER_METADATA + TAG_DELIMINATOR + this.parentDocumentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, "folder#" + this.documentId);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return null;
  }

  @Override
  public String sk() {
    if (this.path == null || this.type == null) {
      throw new IllegalArgumentException("'path' and 'type' is required");
    }
    String folder = this.path.toLowerCase();
    return "file".equals(this.type) ? INDEX_FILE_SK + folder : INDEX_FOLDER_SK + folder;
  }

  @Override
  public String skGsi1() {
    return "folder";
  }

  @Override
  public String skGsi2() {
    return null;
  }

  /**
   * Get Document Type.
   * 
   * @return {@link String}
   */
  public String type() {
    return this.type;
  }

  /**
   * Set Document Type.
   * 
   * @param documentType {@link String}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord type(final String documentType) {
    this.type = documentType;
    return this;
  }

  /**
   * Get Created by user.
   * 
   * @return {@link String}
   */
  public String userId() {
    return this.userId;
  }

  /**
   * Set Create by User.
   * 
   * @param createdBy {@link String}
   * @return {@link FolderIndexRecord}
   */
  public FolderIndexRecord userId(final String createdBy) {
    this.userId = createdBy;
    return this;
  }
}
