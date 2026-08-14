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
  /** Sensitivity. */
  SENSITIVITY("Sensitivity", AttributeType.GOVERNANCE, AttributeDataType.STRING, null),
  /** Malware Scan Result. */
  MALWARE_SCAN_RESULT("MalwareScanResult"),
  /** Relationships. */
  RELATIONSHIPS("Relationships"),
  /** Artifact Status. */
  ARTIFACT_STATUS("ArtifactStatus"),
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
  RETENTION_POLICY("RetentionPolicy", AttributeType.GOVERNANCE, AttributeDataType.ENTITY, null),
  /** Retention Period In Days. */
  RETENTION_PERIOD_IN_DAYS("RetentionPeriodInDays", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Retention Mode. */
  RETENTION_MODE("RetentionMode"),
  /** Retention Disposition Date. */
  DISPOSITION_DATE("DispositionDate", AttributeType.STANDARD, AttributeDataType.STRING,
      AttributeDerivedType.STORED_DERIVED),
  /** Retention Disposition Period In Days. */
  DISPOSITION_DATE_IN_DAYS("DispositionPeriodInDays", AttributeType.STANDARD,
      AttributeDataType.NUMBER, null),
  /** Retention Start Date Source Type. */
  RETENTION_START_DATE_SOURCE_TYPE("RetentionStartDateSourceType"),
  /** Retention Effective Start Date. */
  RETENTION_EFFECTIVE_START_DATE("RetentionEffectiveStartDate", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** Retention Effective End Date. */
  RETENTION_EFFECTIVE_END_DATE("RetentionEffectiveEndDate", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** Retention Effective Status. */
  RETENTION_EFFECTIVE_STATUS("RetentionEffectiveStatus", AttributeType.STANDARD,
      AttributeDataType.STRING, AttributeDerivedType.STANDARD),
  /** Reminder Policy. */
  REMINDER_POLICY("ReminderPolicy", AttributeType.STANDARD, AttributeDataType.ENTITY, null),
  /** Reminder Date Source Attribute Key. */
  REMINDER_DATE_SOURCE_ATTRIBUTE_KEY("ReminderDateSourceAttributeKey", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Period. */
  REMINDER_PERIOD("ReminderPeriod", AttributeType.STANDARD, AttributeDataType.STRING, null),
  /** Reminder Repeat Interval. */
  REMINDER_REPEAT_INTERVAL("ReminderRepeatInterval", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Notification Type. */
  REMINDER_NOTIFICATION_TYPE("ReminderNotificationType", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Notification To Cc. */
  REMINDER_NOTIFICATION_TO_CC("ReminderNotificationToCc", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Notification To Bcc. */
  REMINDER_NOTIFICATION_TO_BCC("ReminderNotificationToBcc", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Notification Subject. */
  REMINDER_NOTIFICATION_SUBJECT("ReminderNotificationSubject", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Notification Text. */
  REMINDER_NOTIFICATION_TEXT("ReminderNotificationText", AttributeType.STANDARD,
      AttributeDataType.STRING, null),
  /** Reminder Due Date. */
  REMINDER_DUE_DATE("ReminderDueDate", AttributeType.STANDARD, AttributeDataType.DATE,
      AttributeDerivedType.STANDARD),
  /** LLM User Prompt. */
  LLM_USER_PROMPT("UserPrompt"),
  /** LLM System Prompt. */
  LLM_SYSTEM_PROMPT("LlmSystemPrompt"),
  /** LLM System Prompt Document Id. */
  LLM_SYSTEM_PROMPT_DOCUMENT_ID("LlmSystemPromptDocumentId"),
  /** LLM System Prompt Artifact Id. */
  LLM_SYSTEM_PROMPT_ARTIFACT_ID("LlmSystemPromptArtifactId"),
  /** LLM Response Preset Entity Types. */
  LLM_RESPONSE_PRESET_ENTITY_TYPES("LlmResponsePresetEntityTypes"),
  /** LLM Response Field Key. */
  LLM_RESPONSE_FIELD_KEYS("LlmResponseFieldKeys"),
  /** LLM Model Id. */
  LLM_MODEL_ID("LlmModelId"),
  /** LLM Max Token. */
  LLM_MAX_TOKENS("LlmMaxTokens", AttributeType.STANDARD, AttributeDataType.NUMBER, null),
  /** LLM Analysis Category. */
  LLM_ANALYSIS_CATEGORY("LlmAnalysisCategory");

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
    return Arrays.stream(values()).filter(a -> a.getDerivedType() != null).toList();
  }

  /** {@link AttributeType}. */
  private final AttributeType type;

  /** Is Attribute Derived. */
  private final AttributeDerivedType derivedType;

  /** Attribute Data Type. */
  private final AttributeDataType dataType;

  /** Key Name. */
  private final String key;

  AttributeKeyReserved(final String reservedKey) {
    this(reservedKey, AttributeType.STANDARD, AttributeDataType.STRING, null);
  }

  AttributeKeyReserved(final String reservedKey, final AttributeType attributeType,
      final AttributeDataType attributeDataType, final AttributeDerivedType derivedAttributeType) {
    this.key = reservedKey;
    this.dataType = attributeDataType;
    this.derivedType = derivedAttributeType;
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
   * Is Attribute Key Derived.
   * 
   * @return boolean
   */
  public AttributeDerivedType getDerivedType() {
    return derivedType;
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

  public boolean isDerived() {
    return derivedType != null;
  }
}
