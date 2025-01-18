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
package com.formkiq.aws.dynamodb.useractivities;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * 
 * UserActivityRecord object.
 *
 */
@Reflectable
public class UserActivityRecord implements DynamodbRecord<UserActivityRecord> {

  /** {@link SimpleDateFormat}. */
  private final SimpleDateFormat df = DateUtil.getIsoDateFormatter();
  /** Document Id. */
  private String documentId;
  /** Record inserted date. */
  private Date insertedDate;
  /** {@link UserActivityType}. */
  private UserActivityType type;
  /** Create by UserId. */
  private String userId;
  /** SK key in the version table. */
  private String versionPk;
  /** SK key in the version table. */
  private String versionSk;

  /**
   * constructor.
   */
  public UserActivityRecord() {

  }

  /**
   * Get Version PK.
   * 
   * @return String
   */
  public String getVersionPk() {
    return versionPk;
  }

  /**
   * Set Version PK.
   *
   * @param versionTablePk {@link String}
   * @return UserActivityRecord
   */
  public UserActivityRecord setVersionPk(final String versionTablePk) {
    this.versionPk = versionTablePk;
    return this;
  }

  /**
   * Get Version SK.
   * 
   * @return String
   */
  public String getVersionSk() {
    return this.versionSk;
  }

  /**
   * Set Version SK.
   * 
   * @param versionTableSk {@link String}
   * @return UserActivityRecord
   */
  public UserActivityRecord setVersionSk(final String versionTableSk) {
    this.versionSk = versionTableSk;
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
   * @return {@link UserActivityRecord}
   */
  public UserActivityRecord setDocumentId(final String id) {
    this.documentId = id;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = getDataAttributes();

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));

    map.put(DbKeys.GSI1_PK, fromS(pkGsi1(siteId)));
    map.put(DbKeys.GSI1_SK, fromS(skGsi1()));
    map.put(DbKeys.GSI2_PK, fromS(pkGsi2(siteId)));
    map.put(DbKeys.GSI2_SK, fromS(skGsi2()));

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {
    Map<String, AttributeValue> map = new HashMap<>();

    map.put("documentId", fromS(this.documentId));
    map.put("type", fromS(this.type.name()));
    map.put("userId", fromS(this.userId));
    map.put("inserteddate", AttributeValue.fromS(this.df.format(this.insertedDate)));

    if (this.versionPk != null) {
      map.put("versionPk", fromS(this.versionPk));
    }

    if (this.versionSk != null) {
      map.put("versionSk", fromS(this.versionSk));
    }

    return map;
  }

  @Override
  public UserActivityRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    UserActivityRecord record = null;

    if (!attrs.isEmpty()) {
      record = new UserActivityRecord().setDocumentId(ss(attrs, "documentId"))
          .setVersionPk(ss(attrs, "versionPk")).setVersionSk(ss(attrs, "versionSk"))
          .setUserId(ss(attrs, "userId")).setType(UserActivityType.valueOf(ss(attrs, "type")));

      String date = ss(attrs, "inserteddate");
      try {
        record = record.setInsertedDate(this.df.parse(date));
      } catch (ParseException e) {
        throw new IllegalArgumentException("invalid inserteddate '" + date + "'");
      }
    }

    return record;
  }

  @Override
  public String pk(final String siteId) {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return createDatabaseKey(siteId, "doc#" + this.documentId);
  }

  @Override
  public String pkGsi1(final String siteId) {
    if (this.userId == null) {
      throw new IllegalArgumentException("'userId' is required");
    }

    return createDatabaseKey(siteId, "activity#user#" + this.userId);
  }

  @Override
  public String pkGsi2(final String siteId) {
    return createDatabaseKey(siteId, "activity#");
  }

  @Override
  public String sk() {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    String formattedDate = getFormattedDate();
    return "activity#" + formattedDate + "#" + this.documentId;
  }

  private String getFormattedDate() {
    return DateUtil.getNowInIso8601Format();
  }

  @Override
  public String skGsi1() {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return "activity#" + getFormattedDate() + "#" + this.documentId;
  }

  @Override
  public String skGsi2() {
    if (this.documentId == null) {
      throw new IllegalArgumentException("'documentId' is required");
    }
    return "activity#" + getFormattedDate() + "#" + this.documentId;
  }

  /**
   * Get Inserted Date.
   *
   * @return {@link Date}
   */
  public Date getInsertedDate() {
    return this.insertedDate;
  }

  /**
   * Set Inserted Date.
   *
   * @param date {@link Date}
   * @return {@link UserActivityRecord}
   */
  public UserActivityRecord setInsertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Get Type.
   * 
   * @return {@link UserActivityType}
   */
  public UserActivityType getType() {
    return this.type;
  }

  /**
   * Set Workflow Status.
   * 
   * @param activityType {@link UserActivityType}
   * @return {@link UserActivityRecord}
   */
  public UserActivityRecord setType(final UserActivityType activityType) {
    this.type = activityType;
    return this;
  }

  /**
   * Get UserId.
   * 
   * @return {@link String}
   */
  public String getUserId() {
    return this.userId;
  }

  /**
   * Set User Id.
   * 
   * @param createdBy {@link String}
   * @return {@link UserActivityRecord}
   */
  public UserActivityRecord setUserId(final String createdBy) {
    this.userId = createdBy;
    return this;
  }
}
