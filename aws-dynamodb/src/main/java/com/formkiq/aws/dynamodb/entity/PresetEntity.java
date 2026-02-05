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
package com.formkiq.aws.dynamodb.entity;

import com.formkiq.aws.dynamodb.documents.DerivedDocumentAttribute;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Present Entity.
 */
public enum PresetEntity {
  /** LLM Prompt entity. */
  LLM_PROMPT("LlmPrompt", List.of("UserPrompt"), List.of()),
  /** Checkout Lock Entity. */
  CHECKOUT("Checkout", List.of("LockedBy", "LockedDate"), List.of()),
  /** Lock Entity. */
  CHECKOUT_FOR_LEGAL_HOLD("CheckoutForLegalHold", List.of("LockedBy", "LockedDate"), List.of()),
  /** Retention Policy. */
  RETENTION_POLICY("RetentionPolicy",
      List.of("RetentionPeriodInDays", "RetentionStartDateSourceType"),
      List.of(new RetentionEffectiveStartDateAttribute(), new RetentionEffectiveEndDateAttribute(),
          new RetentionEffectiveStatusAttribute()));

  /**
   * Find {@link DerivedDocumentAttribute} by AttributeKey.
   * 
   * @param derivedAttributeKey {@link String}
   * @return {@link DerivedDocumentAttribute}
   */
  public static Optional<PresetEntity> findPresetEntityByDerivedAttribute(
      final String derivedAttributeKey) {
    return Arrays.stream(values()).filter(a -> a.getDerivedAttributes().stream()
        .anyMatch(da -> da.getAttributeKey().equals(derivedAttributeKey))).findAny();
  }

  /**
   * Convert a string to the matching enum constant. Defaults to {@link #LLM_PROMPT} if no match is
   * found.
   *
   * @param value string to convert (case-insensitive)
   * @return matching enum constant, or {@code LLM_PROMPT} if none match
   */
  public static PresetEntity fromString(final String value) {
    if (value != null) {
      for (PresetEntity e : values()) {
        if (e.name.equalsIgnoreCase(value) || e.name().equalsIgnoreCase(value)) {
          return e;
        }
      }
    }
    return null;
  }

  /** Derived Document Attributes. */
  private final List<DerivedDocumentAttribute> derivedAttributes;

  /** Entity Name. */
  private final String name;

  /** Attribute Keys. */
  private final List<String> attributeKeys;

  PresetEntity(final String entityName, final List<String> entityAttributeKeys,
      final List<DerivedDocumentAttribute> derivedDocumentAttributes) {
    this.name = entityName;
    this.attributeKeys = entityAttributeKeys;
    this.derivedAttributes = derivedDocumentAttributes;
  }

  /**
   * Find Derived Attribute.
   * 
   * @param derivedAttributeKey {@link String}
   * @return {@link Optional} {@link DerivedDocumentAttribute}
   */
  public Optional<DerivedDocumentAttribute> findDerivedAttribute(final String derivedAttributeKey) {
    return getDerivedAttributes().stream()
        .filter(a -> a.getAttributeKey().equals(derivedAttributeKey)).findAny();
  }

  /**
   * Get Attribute Keys.
   * 
   * @return {@link List} {@link String}
   */
  public List<String> getAttributeKeys() {
    return attributeKeys;
  }

  /**
   * Get {@link List} {@link DerivedDocumentAttribute}.
   * 
   * @return {@link List} {@link DerivedDocumentAttribute}
   */
  public List<DerivedDocumentAttribute> getDerivedAttributes() {
    return derivedAttributes;
  }

  /**
   * Get the string value for this preset entity.
   *
   * @return string representation
   */
  public String getName() {
    return name;
  }
}
