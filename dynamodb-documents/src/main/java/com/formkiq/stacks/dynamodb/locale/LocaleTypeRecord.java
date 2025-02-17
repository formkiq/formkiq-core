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
package com.formkiq.stacks.dynamodb.locale;

import com.formkiq.aws.dynamodb.DbKeys;
import com.formkiq.aws.dynamodb.DynamodbRecord;
import com.formkiq.graalvm.annotations.Reflectable;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createDatabaseKey;

/**
 * Locale Type Record.
 */
@Reflectable
public class LocaleTypeRecord implements DynamodbRecord<LocaleTypeRecord> {

  /** Locale. */
  private String locale;
  /** {@link LocaleResourceType}. */
  private LocaleResourceType itemType;
  /** Localized Value. */
  private String localizedValue;
  /** Interface Key. */
  private String interfaceKey;
  /** Attribute Key. */
  private String attributeKey;
  /** Allowed Value. */
  private String allowedValue;
  /** Classification Id. */
  private String classificationId;

  /**
   * constructor.
   */
  public LocaleTypeRecord() {}

  /**
   * Create Pk.
   * 
   * @param siteId {@link String}
   * @return String
   */
  public static String createPk(final String siteId) {
    return createDatabaseKey(siteId, "locale#type");
  }

  /**
   * Get Interface SK.
   * 
   * @param locale {@link String}
   * @param itemType {@link LocaleResourceType}
   * @param interfaceKey {@link String}
   * @return String
   */
  public static String getInterfaceSk(final String locale, final LocaleResourceType itemType,
      final String interfaceKey) {
    return locale + "#" + itemType.name() + "#" + interfaceKey;
  }

  /**
   * Get Schema SK.
   * 
   * @param locale {@link String}
   * @param itemType {@link LocaleResourceType}
   * @param attributeKey {@link String}
   * @param allowedValue {@link String}
   * @return String
   */
  public static String getSchemaSk(final String locale, final LocaleResourceType itemType,
      final String attributeKey, final String allowedValue) {
    return locale + "#" + itemType.name() + "#" + attributeKey + "#" + allowedValue;
  }

  /**
   * Get Classification SK.
   * 
   * @param locale {@link String}
   * @param itemType {@link LocaleResourceType}
   * @param classificationId {@link String}
   * @param attributeKey {@link String}
   * @param allowedValue {@link String}
   * @return String
   */
  public static String getClassificationSk(final String locale, final LocaleResourceType itemType,
      final String classificationId, final String attributeKey, final String allowedValue) {
    return locale + "#" + itemType.name() + "#" + classificationId + "#" + attributeKey + "#"
        + allowedValue;
  }

  /**
   * Get Locale.
   * 
   * @return String
   */
  public String getLocale() {
    return this.locale;
  }

  /**
   * Set Locale.
   * 
   * @param localeValue {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setLocale(final String localeValue) {
    this.locale = localeValue;
    return this;
  }

  /**
   * Get Classification Id.
   * 
   * @return String
   */
  public String getClassificationId() {
    return this.classificationId;
  }

  /**
   * Set Classification Id.
   * 
   * @param classification {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setClassificationId(final String classification) {
    this.classificationId = classification;
    return this;
  }

  /**
   * Get Allowed Value.
   * 
   * @return String
   */
  public String getAllowedValue() {
    return this.allowedValue;
  }

  /**
   * Set Allowed Value.
   * 
   * @param allowed {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setAllowedValue(final String allowed) {
    this.allowedValue = allowed;
    return this;
  }

  /**
   * Get Attribute Key.
   * 
   * @return String
   */
  public String getAttributeKey() {
    return this.attributeKey;
  }

  /**
   * Set Attribute Key.
   * 
   * @param key {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setAttributeKey(final String key) {
    this.attributeKey = key;
    return this;
  }

  /**
   * Get Interface Key.
   * 
   * @return String
   */
  public String getInterfaceKey() {
    return this.interfaceKey;
  }

  /**
   * Set Interface Key.
   * 
   * @param key {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setInterfaceKey(final String key) {
    this.interfaceKey = key;
    return this;
  }

  /**
   * Get Localized Value.
   * 
   * @return String
   */
  public String getLocalizedValue() {
    return this.localizedValue;
  }

  /**
   * Set Localized Value.
   * 
   * @param value {@link String}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setLocalizedValue(final String value) {
    this.localizedValue = value;
    return this;
  }

  /**
   * Get {@link LocaleResourceType}.
   * 
   * @return {@link LocaleResourceType}
   */
  public LocaleResourceType getItemType() {
    return this.itemType;
  }

  /**
   * Set Item Type.
   * 
   * @param type {@link LocaleResourceType}
   * @return LocaleRecord
   */
  public LocaleTypeRecord setItemType(final LocaleResourceType type) {
    this.itemType = type;
    return this;
  }

  @Override
  public Map<String, AttributeValue> getAttributes(final String siteId) {

    Map<String, AttributeValue> map = new HashMap<>(getDataAttributes());

    map.put(DbKeys.PK, fromS(pk(siteId)));
    map.put(DbKeys.SK, fromS(sk()));

    return map;
  }

  @Override
  public Map<String, AttributeValue> getDataAttributes() {

    Map<String, AttributeValue> map = new HashMap<>();
    map.put("locale", fromS(this.locale));
    map.put("itemType", fromS(this.itemType.name()));

    addValue(map, "localizedValue", this.localizedValue);
    addValue(map, "interfaceKey", this.interfaceKey);
    addValue(map, "attributeKey", this.attributeKey);
    addValue(map, "allowedValue", this.allowedValue);
    addValue(map, "classificationId", this.classificationId);
    return map;
  }

  private void addValue(final Map<String, AttributeValue> map, final String key,
      final String value) {
    if (value != null) {
      map.put(key, fromS(value));
    }
  }

  @Override
  public LocaleTypeRecord getFromAttributes(final String siteId,
      final Map<String, AttributeValue> attrs) {

    LocaleTypeRecord record = null;

    if (!attrs.isEmpty()) {
      record = new LocaleTypeRecord().setItemType(LocaleResourceType.valueOf(ss(attrs, "itemType")))
          .setLocalizedValue(ss(attrs, "localizedValue")).setInterfaceKey(ss(attrs, "interfaceKey"))
          .setAttributeKey(ss(attrs, "attributeKey")).setAllowedValue(ss(attrs, "allowedValue"))
          .setClassificationId(ss(attrs, "classificationId")).setLocale(ss(attrs, "locale"));
    }

    return record;
  }

  @Override
  public String pk(final String siteId) {
    return createPk(siteId);
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
    String sk;

    if (this.locale == null) {
      throw new IllegalArgumentException("'locale' is required");
    }

    switch (this.itemType) {
      case INTERFACE -> {
        if (this.interfaceKey == null) {
          throw new IllegalArgumentException("'interfaceKey' is required");
        }
        sk = getInterfaceSk(this.locale, this.itemType, this.interfaceKey);
      }
      case SCHEMA -> {
        validateSchemaRequiredFields();
        sk = getSchemaSk(this.locale, this.itemType, this.attributeKey, this.allowedValue);
      }
      case CLASSIFICATION -> {
        validateClassificationRequiredFields();
        sk = getClassificationSk(this.locale, this.itemType, classificationId, attributeKey,
            this.allowedValue);
      }
      default -> throw new IllegalStateException("Unexpected value: " + this.itemType);
    }

    return sk;
  }

  private void validateSchemaRequiredFields() {
    if (this.attributeKey == null) {
      throw new IllegalArgumentException("'attributeKey' is required");
    }
    if (this.allowedValue == null) {
      throw new IllegalArgumentException("'allowedValue' is required");
    }
  }

  private void validateClassificationRequiredFields() {
    validateSchemaRequiredFields();
    if (this.classificationId == null) {
      throw new IllegalArgumentException("'classificationId' is required");
    }
  }

  @Override
  public String skGsi1() {
    return null;
  }

  @Override
  public String skGsi2() {
    return null;
  }

  public String getItemKey() {
    String sk = sk();
    return sk.substring(sk.indexOf("#") + 1);
  }
}
