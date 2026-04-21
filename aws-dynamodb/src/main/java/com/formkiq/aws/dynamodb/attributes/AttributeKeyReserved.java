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
package com.formkiq.aws.dynamodb.attributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Reserved Attribute Keys.
 */
public enum AttributeKeyReserved {
  /** Publication. */
  PUBLICATION("Publication"),
  /** Classification. */
  CLASSIFICATION("Classification"),
  /** Malware Scan Result. */
  MALWARE_SCAN_RESULT("MalwareScanResult"),
  /** Relationships. */
  RELATIONSHIPS("Relationships"),
  /** Checkout. */
  CHECKOUT("Checkout"),
  /** CheckoutForLegalHold. */
  CHECKOUT_FOR_LEGAL_HOLD("CheckoutForLegalHold"),
  /** Checkout. */
  LOCKED_BY("LockedBy"),
  /** Checkout. */
  LOCKED_DATE("LockedDate"),
  /** Esignature Docusign Envelope id. */
  ESIGNATURE_DOCUSIGN_ENVELOPE_ID("EsignatureDocusignEnvelopeId"),
  /** Esignature Docusign Status. */
  ESIGNATURE_DOCUSIGN_STATUS("EsignatureDocusignStatus"),
  /** Retention Policy. */
  RETENTION_POLICY("RetentionPolicy", AttributeType.GOVERNANCE, AttributeDataType.ENTITY, false),
  /** Retention Period In Days. */
  RETENTION_PERIOD_IN_DAYS("RetentionPeriodInDays", AttributeType.STANDARD,
      AttributeDataType.NUMBER, false),
  /** Retention Disposition Date. */
  DISPOSITION_DATE("DispositionDate", AttributeType.GOVERNANCE, AttributeDataType.STRING, false),
  /** Retention Start Date Source Type. */
  RETENTION_START_DATE_SOURCE_TYPE("RetentionStartDateSourceType"),
  /** Retention Effective Start Date. */
  RETENTION_EFFECTIVE_START_DATE("RetentionEffectiveStartDate", AttributeType.STANDARD,
      AttributeDataType.STRING, true),
  /** Retention Effective End Date. */
  RETENTION_EFFECTIVE_END_DATE("RetentionEffectiveEndDate", AttributeType.STANDARD,
      AttributeDataType.STRING, true),
  /** Retention Effective Status. */
  RETENTION_EFFECTIVE_STATUS("RetentionEffectiveStatus", AttributeType.STANDARD,
      AttributeDataType.STRING, true);

  /**
   * Find {@link AttributeKeyReserved}.
   * 
   * @param key {@link AttributeKeyReserved}
   * @return {@link AttributeKeyReserved}
   */
  public static AttributeKeyReserved find(final String key) {
    Optional<AttributeKeyReserved> a =
        Arrays.stream(values()).filter(v -> v.getKey().equalsIgnoreCase(key)).findFirst();
    return a.orElse(null);
  }

  /**
   * Get Derived Attributes.
   * 
   * @return {@link List} {@link AttributeKeyReserved}
   */
  public static List<AttributeKeyReserved> getDerivedAttributes() {
    return Arrays.stream(values()).filter(AttributeKeyReserved::isDerived).toList();
  }

  /** {@link AttributeType}. */
  private final AttributeType type;

  /** Is Attribute Derived. */
  private final boolean derived;

  /** Attribute Data Type. */
  private final AttributeDataType dataType;

  /** Key Name. */
  private final String key;

  AttributeKeyReserved(final String reservedKey) {
    this(reservedKey, AttributeType.STANDARD, AttributeDataType.STRING, false);
  }

  AttributeKeyReserved(final String reservedKey, final AttributeType attributeType,
      final AttributeDataType attributeDataType, final boolean derivedAttribute) {
    this.key = reservedKey;
    this.dataType = attributeDataType;
    this.derived = derivedAttribute;
    this.type = attributeType;
  }

  /**
   * Get Data Type.
   *
   * @return {@link AttributeDataType}
   */
  public AttributeDataType getDataType() {
    return this.dataType;
  }

  /**
   * Get Key Name.
   * 
   * @return {@link String}
   */
  public String getKey() {
    return this.key;
  }

  /**
   * Get Attribute Type.
   * 
   * @return {@link AttributeType}
   */
  public AttributeType getType() {
    return type;
  }

  /**
   * Is Attribute Key Derived.
   * 
   * @return boolean
   */
  public boolean isDerived() {
    return derived;
  }
}
