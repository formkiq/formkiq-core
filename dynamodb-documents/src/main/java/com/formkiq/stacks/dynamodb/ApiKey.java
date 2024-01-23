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
import static com.formkiq.aws.dynamodb.objects.Strings.isEmpty;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.aws.dynamodb.SiteIdKeyGenerator;
import com.formkiq.aws.dynamodb.objects.DateUtil;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 
 * {@link DynamodbRecord} for API Key.
 *
 */
@Reflectable
public class ApiKey implements DynamodbRecord<ApiKey>, DbKeys {

  /** Api Key Length. */
  static final int API_KEY_LENGTH = 51;
  /** Mask Value, must be even number. */
  private static final int MASK = 8;

  /** API Key of record. */
  @Reflectable
  private String apiKey;
  /** Record inserted date. */
  @Reflectable
  private Date insertedDate;
  /** Name of Api Key. */
  @Reflectable
  private String name;
  /** Type of Permission. */
  @Reflectable
  private Collection<ApiKeyPermission> permissions;
  /** SiteId for ApiKey. */
  @Reflectable
  private String siteId;
  /** Creator of record. */
  @Reflectable
  private String userId;

  /**
   * Get Api Key.
   * 
   * @return {@link String}
   */
  public String apiKey() {
    return this.apiKey;
  }

  /**
   * Set Api Key.
   * 
   * @param key {@link String}
   * @return {@link ApiKey}
   */
  public ApiKey apiKey(final String key) {
    this.apiKey = key;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteIdParam) {

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    Map<String, AttributeValue> map = new HashMap<>();
    map.put(DbKeys.PK, AttributeValue.fromS(pk(siteIdParam)));
    map.put(DbKeys.SK, AttributeValue.fromS(sk()));
    map.put("apiKey", AttributeValue.fromS(this.apiKey));
    map.put("name", AttributeValue.fromS(this.name));
    map.put("userId", AttributeValue.fromS(this.userId));
    map.put("siteId", AttributeValue.fromS(this.siteId));
    map.put("permissions",
        fromL(this.permissions.stream().map(p -> fromS(p.name())).collect(Collectors.toList())));

    map.put(GSI1_PK, AttributeValue.fromS(pkGsi1(siteIdParam)));
    map.put(GSI1_SK, AttributeValue.fromS(skGsi1()));

    map.put(GSI2_PK, AttributeValue.fromS(pkGsi2(siteIdParam)));
    map.put(GSI2_SK, AttributeValue.fromS(skGsi2()));

    if (this.insertedDate != null) {
      map.put("inserteddate", AttributeValue.fromS(df.format(this.insertedDate)));
    }

    return map;
  }

  @Override
  public ApiKey getFromAttributes(final String siteIdParam,
      final Map<String, AttributeValue> attrs) {

    ApiKey record = new ApiKey().apiKey(ss(attrs, "apiKey")).name(ss(attrs, "name"))
        .userId(ss(attrs, "userId")).permissions(toPermissions(attrs)).siteId(ss(attrs, "siteId"));

    SimpleDateFormat df = DateUtil.getIsoDateFormatter();

    if (attrs.containsKey("inserteddate")) {
      try {
        record = record.insertedDate(df.parse(ss(attrs, "inserteddate")));
      } catch (ParseException e) {
        e.printStackTrace();
        throw new IllegalArgumentException("invalid 'inserteddate'");
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
   * @return {@link ApiKey}
   */
  public ApiKey insertedDate(final Date date) {
    this.insertedDate = date;
    return this;
  }

  /**
   * Mask API Key.
   * 
   * @return {@link String}
   */
  public String mask() {
    return this.apiKey != null && this.apiKey.length() == API_KEY_LENGTH
        ? this.apiKey.subSequence(0, MASK) + "****************"
            + this.apiKey.substring(this.apiKey.length() - MASK / 2)
        : this.apiKey;
  }

  /**
   * Get Api Key Name.
   * 
   * @return {@link String}
   */
  public String name() {
    return this.name;
  }

  /**
   * Set Api Key Name.
   * 
   * @param apiKeyName {@link String}
   * @return {@link ApiKey}
   */
  public ApiKey name(final String apiKeyName) {
    this.name = apiKeyName;
    return this;
  }

  /**
   * Get Api Key Permissions.
   * 
   * @return {@link Collection} {@link ApiKeyPermission}
   */
  public Collection<ApiKeyPermission> permissions() {
    return this.permissions;
  }

  /**
   * Set Api Key Permissions.
   * 
   * @param apiKeyPermissions {@link Collection} {@link ApiKeyPermission}
   * @return {@link ApiKey}
   */
  public ApiKey permissions(final Collection<ApiKeyPermission> apiKeyPermissions) {
    this.permissions = apiKeyPermissions;
    return this;
  }

  @Override
  public String pk(final String siteIdParam) {
    return "apikeys" + TAG_DELIMINATOR;
  }

  @Override
  public String pkGsi1(final String siteIdParam) {
    return createDatabaseKey(siteIdParam, pk(null));
  }

  @Override
  public String pkGsi2(final String siteIdParam) {
    return createDatabaseKey(siteIdParam, pk(null));
  }

  /**
   * Get SiteId.
   * 
   * @return {@link String}
   */
  public String siteId() {
    return this.siteId;
  }

  /**
   * Set SiteId.
   * 
   * @param siteIdParam {@link String}
   * @return {@link ApiKey}
   */
  public ApiKey siteId(final String siteIdParam) {
    this.siteId = !isEmpty(siteIdParam) ? siteIdParam : SiteIdKeyGenerator.DEFAULT_SITE_ID;
    return this;
  }

  @Override
  public String sk() {
    if (this.apiKey == null) {
      throw new IllegalArgumentException("'apiKey' is required");
    }
    return "apikey" + TAG_DELIMINATOR + this.apiKey;
  }

  @Override
  public String skGsi1() {
    return "apikey" + TAG_DELIMINATOR + this.name + TAG_DELIMINATOR + this.apiKey;
  }

  @Override
  public String skGsi2() {
    return "apikey" + TAG_DELIMINATOR + mask();
  }

  /**
   * To Permissions.
   * 
   * @param attrs {@link Map} {@link AttributeValue}
   * @return {@link Collection} {@link ApiKeyPermission}
   */
  private Collection<ApiKeyPermission> toPermissions(final Map<String, AttributeValue> attrs) {

    Collection<ApiKeyPermission> list = Collections.emptyList();

    if (attrs.containsKey("permissions")) {
      list = attrs.get("permissions").l().stream().map(p -> ApiKeyPermission.valueOf(p.s()))
          .collect(Collectors.toList());
    }

    return list;
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
   * @return {@link ApiKey}
   */
  public ApiKey userId(final String createdBy) {
    this.userId = createdBy;
    return this;
  }
}
